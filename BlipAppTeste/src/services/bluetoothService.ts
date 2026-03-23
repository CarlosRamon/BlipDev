import { BleManager, Device, BleError } from 'react-native-ble-plx';
import { Platform, PermissionsAndroid } from 'react-native';
import { Buffer } from 'buffer';

export const SERVICE_UUID = '12345678-1234-1234-1234-1234567890ab';
export const CHARACTERISTIC_UUID = 'abcd1234-5678-90ab-cdef-1234567890ab';
export const DEVICE_NAME = 'ESP32_LAVAGEM';

const SCAN_TIMEOUT_MS = 10_000;
const CONNECT_TIMEOUT_MS = 10_000;

// Singleton — criado uma única vez para o ciclo de vida do app
export const bleManager = new BleManager();

export async function requestAndroidPermissions(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;

  const apiLevel = Number(Platform.Version);

  if (apiLevel >= 31) {
    // Android 12+ (API 31+): BLUETOOTH_SCAN + BLUETOOTH_CONNECT
    const results = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    ]);
    return (
      results[PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN] === 'granted' &&
      results[PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT] === 'granted'
    );
  } else {
    // Android < 12: ACCESS_FINE_LOCATION obrigatória para BLE scan
    const result = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      {
        title: 'Permissão de Localização',
        message: 'Necessária para escanear dispositivos Bluetooth.',
        buttonPositive: 'Permitir',
        buttonNegative: 'Negar',
      }
    );
    return result === 'granted';
  }
}

/**
 * Escaneia dispositivos BLE e retorna o primeiro com nome "ESP32_LAVAGEM".
 * Lança erro se não encontrar dentro do timeout.
 */
export function scanDevices(): Promise<Device> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bleManager.stopDeviceScan();
      reject(
        new Error('Dispositivo não encontrado. Verifique se o ESP32 está ligado e próximo.')
      );
    }, SCAN_TIMEOUT_MS);

    bleManager.startDeviceScan(null, { allowDuplicates: false }, (error: BleError | null, device: Device | null) => {
      if (error) {
        clearTimeout(timer);
        bleManager.stopDeviceScan();
        reject(error);
        return;
      }

      if (device?.name === DEVICE_NAME) {
        clearTimeout(timer);
        bleManager.stopDeviceScan();
        resolve(device);
      }
    });
  });
}

/**
 * Conecta ao dispositivo pelo ID e descobre serviços/características.
 */
export async function connectToDevice(deviceId: string): Promise<Device> {
  const device = await bleManager.connectToDevice(deviceId, {
    timeout: CONNECT_TIMEOUT_MS,
    autoConnect: false,
  });
  await device.discoverAllServicesAndCharacteristics();
  return device;
}

/**
 * Encerra a conexão com o dispositivo, se estiver conectado.
 */
export async function disconnect(deviceId: string): Promise<void> {
  const isConnected = await bleManager.isDeviceConnected(deviceId);
  if (isConnected) {
    await bleManager.cancelDeviceConnection(deviceId);
  }
}

/**
 * Envia um comando (string JSON) para a characteristic do ESP32.
 * Usa base64 conforme exigido pelo react-native-ble-plx.
 */
export async function sendCommand(device: Device, data: string): Promise<void> {
  const base64 = Buffer.from(data, 'utf-8').toString('base64');
  await device.writeCharacteristicWithResponseForService(
    SERVICE_UUID,
    CHARACTERISTIC_UUID,
    base64
  );
}
