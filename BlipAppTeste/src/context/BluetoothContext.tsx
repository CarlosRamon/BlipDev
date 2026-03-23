import React, {
  createContext,
  useContext,
  useState,
  useRef,
  useCallback,
  ReactNode,
} from 'react';
import { Device, Subscription } from 'react-native-ble-plx';
import * as BLE from '../services/bluetoothService';

export type ConnectionStatus =
  | 'disconnected'
  | 'scanning'
  | 'connecting'
  | 'connected'
  | 'error';

type BluetoothContextType = {
  connectedDevice: Device | null;
  connectionStatus: ConnectionStatus;
  errorMessage: string | null;
  scanAndConnect: () => Promise<void>;
  sendCommand: (data: string) => Promise<void>;
  disconnectDevice: () => Promise<void>;
  resetError: () => void;
};

const BluetoothContext = createContext<BluetoothContextType | null>(null);

export function BluetoothProvider({ children }: { children: ReactNode }) {
  const [connectedDevice, setConnectedDevice] = useState<Device | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const disconnectSubscription = useRef<Subscription | null>(null);

  const cleanupSubscription = () => {
    disconnectSubscription.current?.remove();
    disconnectSubscription.current = null;
  };

  const scanAndConnect = useCallback(async () => {
    setErrorMessage(null);

    try {
      // 1. Permissões
      const granted = await BLE.requestAndroidPermissions();
      if (!granted) {
        setConnectionStatus('error');
        setErrorMessage('Permissões Bluetooth negadas. Habilite nas configurações do sistema.');
        return;
      }

      // 2. Scan
      setConnectionStatus('scanning');
      const found = await BLE.scanDevices();

      // 3. Conectar
      setConnectionStatus('connecting');
      const device = await BLE.connectToDevice(found.id);

      // 4. Monitorar desconexão
      cleanupSubscription();
      disconnectSubscription.current = device.onDisconnected((_error, _dev) => {
        setConnectedDevice(null);
        setConnectionStatus('disconnected');
        cleanupSubscription();
      });

      setConnectedDevice(device);
      setConnectionStatus('connected');
    } catch (e: unknown) {
      cleanupSubscription();
      setConnectionStatus('error');
      setErrorMessage(
        e instanceof Error ? e.message : 'Falha ao conectar. Tente novamente.'
      );
    }
  }, []);

  const sendCommand = useCallback(
    async (data: string) => {
      if (!connectedDevice) {
        throw new Error('Nenhum dispositivo conectado.');
      }

      // Verificar se ainda está conectado antes de enviar
      const isConnected = await BLE.bleManager.isDeviceConnected(connectedDevice.id);
      if (!isConnected) {
        setConnectedDevice(null);
        setConnectionStatus('disconnected');
        throw new Error('Dispositivo desconectado. Reconecte e tente novamente.');
      }

      await BLE.sendCommand(connectedDevice, data);
    },
    [connectedDevice]
  );

  const disconnectDevice = useCallback(async () => {
    if (connectedDevice) {
      cleanupSubscription();
      await BLE.disconnect(connectedDevice.id);
      setConnectedDevice(null);
      setConnectionStatus('disconnected');
    }
  }, [connectedDevice]);

  const resetError = useCallback(() => {
    setErrorMessage(null);
    setConnectionStatus('disconnected');
  }, []);

  return (
    <BluetoothContext.Provider
      value={{
        connectedDevice,
        connectionStatus,
        errorMessage,
        scanAndConnect,
        sendCommand,
        disconnectDevice,
        resetError,
      }}
    >
      {children}
    </BluetoothContext.Provider>
  );
}

export function useBluetooth(): BluetoothContextType {
  const ctx = useContext(BluetoothContext);
  if (!ctx) throw new Error('useBluetooth deve ser usado dentro de <BluetoothProvider>');
  return ctx;
}
