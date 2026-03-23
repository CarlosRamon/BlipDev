import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
} from 'react-native';
import { ConnectionStatus } from '../context/BluetoothContext';

type BleStatusBarProps = {
  status: ConnectionStatus;
  errorMessage: string | null;
  onConnect: () => void;
  onDisconnect: () => void;
};

type StatusConfig = {
  dot: string;
  label: string;
  showConnect: boolean;
  showDisconnect: boolean;
  showSpinner: boolean;
};

const STATUS_CONFIG: Record<ConnectionStatus, StatusConfig> = {
  disconnected: {
    dot: '#BDC1C6',
    label: 'Desconectado',
    showConnect: true,
    showDisconnect: false,
    showSpinner: false,
  },
  scanning: {
    dot: '#FBBC04',
    label: 'Procurando ESP32...',
    showConnect: false,
    showDisconnect: false,
    showSpinner: true,
  },
  connecting: {
    dot: '#FBBC04',
    label: 'Conectando...',
    showConnect: false,
    showDisconnect: false,
    showSpinner: true,
  },
  connected: {
    dot: '#34A853',
    label: 'ESP32 conectado',
    showConnect: false,
    showDisconnect: true,
    showSpinner: false,
  },
  error: {
    dot: '#EA4335',
    label: 'Falha na conexão',
    showConnect: true,
    showDisconnect: false,
    showSpinner: false,
  },
};

export default function BleStatusBar({
  status,
  errorMessage,
  onConnect,
  onDisconnect,
}: BleStatusBarProps) {
  const config = STATUS_CONFIG[status];

  return (
    <View style={styles.container}>
      <View style={styles.left}>
        {config.showSpinner ? (
          <ActivityIndicator size="small" color="#FBBC04" style={styles.spinner} />
        ) : (
          <View style={[styles.dot, { backgroundColor: config.dot }]} />
        )}
        <View>
          <Text style={styles.label}>{config.label}</Text>
          {status === 'error' && errorMessage && (
            <Text style={styles.errorText} numberOfLines={2}>
              {errorMessage}
            </Text>
          )}
        </View>
      </View>

      <View>
        {config.showConnect && (
          <TouchableOpacity style={styles.button} onPress={onConnect} activeOpacity={0.8}>
            <Text style={styles.buttonText}>Conectar</Text>
          </TouchableOpacity>
        )}
        {config.showDisconnect && (
          <TouchableOpacity
            style={[styles.button, styles.buttonDisconnect]}
            onPress={onDisconnect}
            activeOpacity={0.8}
          >
            <Text style={[styles.buttonText, styles.buttonTextDisconnect]}>Desconectar</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 3,
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  spinner: {
    width: 10,
    marginRight: 0,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#202124',
  },
  errorText: {
    fontSize: 11,
    color: '#EA4335',
    marginTop: 2,
    maxWidth: 200,
  },
  button: {
    backgroundColor: '#1A73E8',
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 8,
  },
  buttonDisconnect: {
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: '#BDC1C6',
  },
  buttonText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  buttonTextDisconnect: {
    color: '#5F6368',
  },
});
