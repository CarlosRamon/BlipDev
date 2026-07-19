package br.com.bliqbrasil.totem.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import br.com.bliqbrasil.totem.data.model.ConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.resumeWithException

private const val CCCD_UUID       = "00002902-0000-1000-8000-00805f9b34fb"
private const val SCAN_TIMEOUT_MS = 10_000L

// Backoff delays between retry attempts: 3s, 5s, 8s, 12s, 15s, 15s…
private val RECONNECT_DELAYS = listOf(3_000L, 5_000L, 8_000L, 12_000L, 15_000L)

class BliqBleManager(private val context: Context) {

    // ── BLE config (configured after loading PosConfig) ──────────────────

    private var deviceName  = "ESP32_LAVAGEM"
    private var serviceUuid = "00535500-0000-0000-0000-000000000000"
    private var charUuid    = "00435500-0000-0000-0000-000000000000"

    fun setDeviceName(uuidEsp32: String) {
        deviceName = "CLP_$uuidEsp32"
    }

    fun debugInfo(): Map<String, String> = mapOf(
        "Permissão"    to if (hasPermissions()) "OK" else "NEGADA",
        "Dispositivo"  to deviceName,
        "Service UUID" to serviceUuid,
        "Char UUID"    to charUuid,
    )

    fun setUuids(serviceUuid: String, charUuid: String) {
        val changed = this.serviceUuid != serviceUuid || this.charUuid != charUuid
        this.serviceUuid = serviceUuid
        this.charUuid    = charUuid
        if (changed && _status.value == ConnectionStatus.CONNECTED) {
            startAutoConnect(force = true)
        }
    }

    // ── Public state ──────────────────────────────────────────────────────

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _notification = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val notification: SharedFlow<String> = _notification.asSharedFlow()

    // ── Internal state ────────────────────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeMutex = Mutex()

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var characteristic: BluetoothGattCharacteristic? = null

    // Persistent connection loop job — cancelled on explicit disconnect
    private var connectionJob: Job? = null
    private var autoReconnect  = false
    private var retryAttempt   = 0
    private var lastAddress: String? = null

    // Continuations for one-shot GATT operations
    private var connectCont:    CancellableContinuation<Unit>? = null
    private var discoverCont:   CancellableContinuation<Unit>? = null
    private var writeCont:      CancellableContinuation<Unit>? = null
    private var descriptorCont: CancellableContinuation<Unit>? = null

    // ── GATT callback ─────────────────────────────────────────────────────

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectCont?.resume(Unit) { g.disconnect() }
                    connectCont = null
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectCont != null) {
                        connectCont?.resumeWithException(Exception("Conexão falhou (status $status)"))
                        connectCont = null
                    } else {
                        // Cancela operações pendentes para liberar o writeMutex
                        writeCont?.resumeWithException(Exception("BLE desconectado durante escrita"))
                        writeCont = null
                        descriptorCont?.resumeWithException(Exception("BLE desconectado"))
                        descriptorCont = null
                        handleUnexpectedDisconnect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) discoverCont?.resume(Unit) {}
            else discoverCont?.resumeWithException(Exception("Falha ao descobrir serviços ($status)"))
            discoverCont = null
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) writeCont?.resume(Unit) {}
            else writeCont?.resumeWithException(Exception("Falha ao escrever ($status)"))
            writeCont = null
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) descriptorCont?.resume(Unit) {}
            else descriptorCont?.resumeWithException(Exception("Falha ao habilitar notificações"))
            descriptorCont = null
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            scope.launch { _notification.emit(value.toString(Charsets.UTF_8)) }
        }

        @Deprecated("Used for Android < 13")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            val value = char.value ?: return
            scope.launch { _notification.emit(value.toString(Charsets.UTF_8)) }
        }

    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Starts an auto-connect loop that keeps trying until connected,
     * and automatically reconnects on drops. Safe to call multiple times.
     *
     * @param force If true, cancels any in-progress loop and restarts immediately.
     */
    fun startAutoConnect(force: Boolean = false) {
        if (autoReconnect && !force) return
        autoReconnect = true
        retryAttempt  = 0
        connectionJob?.cancel()
        connectionJob = scope.launch { connectionLoop() }
    }

    /**
     * Suspends until BLE is connected (or throws TimeoutCancellationException).
     * Used by Session/Success screens when they need the connection to be ready.
     */
    suspend fun waitForConnection(timeoutMs: Long = 15_000L) {
        if (!autoReconnect) startAutoConnect()
        withTimeout(timeoutMs) {
            status.first { it == ConnectionStatus.CONNECTED }
        }
    }

    suspend fun sendCommand(json: String) {
        val g    = gatt           ?: throw Exception("Nenhum dispositivo conectado.")
        val char = characteristic ?: throw Exception("Característica BLE não disponível.")

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as android.bluetooth.BluetoothManager
        val connected = btManager.getConnectionState(g.device, BluetoothProfile.GATT) ==
            BluetoothProfile.STATE_CONNECTED
        if (!connected) throw Exception("Dispositivo desconectado.")

        writeMutex.withLock {
            suspendCancellableCoroutine<Unit> { cont ->
                writeCont = cont
                val data = json.toByteArray(Charsets.UTF_8)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = data
                    @Suppress("DEPRECATION")
                    g.writeCharacteristic(char)
                }
            }
        }
    }

    fun disconnect() {
        autoReconnect = false
        connectionJob?.cancel()
        connectionJob = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        characteristic = null
        _status.value = ConnectionStatus.DISCONNECTED
        _errorMessage.value = null
    }

    fun hasPermissions(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun neededPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun onDestroy() {
        autoReconnect = false
        connectionJob?.cancel()
        scope.cancel()
        gatt?.close()
        gatt = null
    }

    // ── Connection loop (auto-connect + auto-reconnect) ───────────────────

    private suspend fun connectionLoop() {
        while (autoReconnect) {
            try {
                _errorMessage.value = null
                val device: BluetoothDevice = when {
                    // On even retries after the first: try direct connect by address (faster)
                    lastAddress != null && retryAttempt > 0 && retryAttempt % 2 == 0 -> {
                        _status.value = ConnectionStatus.CONNECTING
                        getBluetoothAdapter().getRemoteDevice(lastAddress)
                    }
                    else -> {
                        _status.value = ConnectionStatus.SCANNING
                        val found = scanForDevice()
                        lastAddress = found.address
                        _status.value = ConnectionStatus.CONNECTING
                        found
                    }
                }

                doConnect(device)
                _status.value = ConnectionStatus.CONNECTED
                retryAttempt = 0
                return  // Stay connected — gattCallback handles drops
            } catch (e: Exception) {
                Log.e("BliqBLE", "Falha na conexão (tentativa $retryAttempt): ${e.message}")
                closeGatt()
                if (!autoReconnect) {
                    _status.value = ConnectionStatus.DISCONNECTED
                    return
                }
                val delayMs = RECONNECT_DELAYS.getOrElse(retryAttempt) { RECONNECT_DELAYS.last() }
                retryAttempt++
                _status.value = ConnectionStatus.RECONNECTING
                _errorMessage.value = e.message
                delay(delayMs)
            }
        }
    }

    private fun handleUnexpectedDisconnect() {
        // Atualiza o status ANTES de zerar o gatt — evita janela onde status=CONNECTED mas gatt=null
        if (autoReconnect) {
            _status.value = ConnectionStatus.RECONNECTING
        } else {
            _status.value = ConnectionStatus.DISCONNECTED
        }
        closeGatt()
        if (autoReconnect) {
            connectionJob?.cancel()
            connectionJob = scope.launch {
                val delayMs = RECONNECT_DELAYS.getOrElse(retryAttempt) { RECONNECT_DELAYS.last() }
                delay(delayMs)
                retryAttempt++
                connectionLoop()
            }
        }
    }

    // ── Low-level GATT helpers ────────────────────────────────────────────

    private fun closeGatt() {
        gatt?.close()
        gatt = null
        characteristic = null
    }

    private fun getBluetoothAdapter(): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as android.bluetooth.BluetoothManager
        return manager.adapter ?: throw Exception("Bluetooth não disponível neste dispositivo.")
    }

    private suspend fun scanForDevice(): BluetoothDevice {
        val adapter = getBluetoothAdapter()
        if (!adapter.isEnabled) throw Exception("Bluetooth está desativado. Ative o Bluetooth e tente novamente.")
        val scanner = adapter.bluetoothLeScanner
            ?: throw Exception("Scanner BLE não disponível.")

        return try {
            withTimeout(SCAN_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val callback = object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult) {
                            if (result.device.name == deviceName) {
                                scanner.stopScan(this)
                                if (cont.isActive) cont.resume(result.device) {}
                            }
                        }
                        override fun onScanFailed(errorCode: Int) {
                            if (cont.isActive) cont.resumeWithException(
                                Exception("Scan falhou (código $errorCode)")
                            )
                        }
                    }
                    scanner.startScan(callback)
                    cont.invokeOnCancellation { scanner.stopScan(callback) }
                }
            }
        } catch (_: TimeoutCancellationException) {
            throw Exception("ESP32 não encontrado. Verifique se está ligado e próximo.")
        }
    }

    private suspend fun doConnect(device: BluetoothDevice) {
        suspendCancellableCoroutine<Unit> { cont ->
            connectCont = cont
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
        val g = gatt ?: throw Exception("GATT nulo após conexão.")
        suspendCancellableCoroutine<Unit> { cont ->
            discoverCont = cont
            g.discoverServices()
        }
        setupCharacteristic(g)
    }

    private suspend fun setupCharacteristic(g: BluetoothGatt) {
        Log.d("BliqBLE", "setupCharacteristic — buscando serviceUuid=$serviceUuid")
        g.services.forEach { s -> Log.d("BliqBLE", "  serviço encontrado: ${s.uuid}") }

        val service = g.getService(UUID.fromString(serviceUuid))
            ?: throw Exception("Serviço BLE não encontrado. Esperado=$serviceUuid | Encontrados=${g.services.map { it.uuid }}")
        val char = service.getCharacteristic(UUID.fromString(charUuid))
            ?: throw Exception("Característica BLE não encontrada. Esperado=$charUuid | Encontradas=${service.characteristics.map { it.uuid }}")

        characteristic = char
        g.setCharacteristicNotification(char, true)

        val descriptor = char.getDescriptor(UUID.fromString(CCCD_UUID)) ?: return

        suspendCancellableCoroutine<Unit> { cont ->
            descriptorCont = cont
            val enableValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, enableValue)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = enableValue
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }
        }
    }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
}
