import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ActivityIndicator,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { RootStackParamList, PaymentMethod } from '../types/navigation';
import Button from '../components/Button';
import { formatCurrency } from '../services/paymentService';
import { useBluetooth } from '../context/BluetoothContext';

type Props = {
  navigation: NativeStackNavigationProp<RootStackParamList, 'Success'>;
  route: RouteProp<RootStackParamList, 'Success'>;
};

const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  credit: 'Cartão de Crédito',
  debit: 'Cartão de Débito',
  pix: 'Pix',
};

type BleStep = 'idle' | 'connecting' | 'sending' | 'success' | 'error';

type StepConfig = {
  icon: string;
  label: string;
  color: string;
  showSpinner: boolean;
};

const STEP_CONFIG: Record<BleStep, StepConfig> = {
  idle: { icon: '⏳', label: 'Aguardando...', color: '#9AA0A6', showSpinner: false },
  connecting: { icon: '🔵', label: 'Conectando ao dispositivo...', color: '#FBBC04', showSpinner: true },
  sending: { icon: '📡', label: 'Enviando comando...', color: '#1A73E8', showSpinner: true },
  success: { icon: '🔓', label: 'Máquina liberada com sucesso!', color: '#34A853', showSpinner: false },
  error: { icon: '❌', label: '', color: '#EA4335', showSpinner: false },
};

export default function SuccessScreen({ navigation, route }: Props) {
  const { paymentMethod, totalMinutes, totalPrice } = route.params;
  const { connectionStatus, scanAndConnect, sendCommand } = useBluetooth();

  const [bleStep, setBleStep] = useState<BleStep>('idle');
  const [bleError, setBleError] = useState<string | null>(null);
  const ranRef = useRef(false);

  const command = JSON.stringify({ action: 'START', duration: totalMinutes });

  const runBleSequence = async () => {
    setBleError(null);

    try {
      // Conectar se necessário
      if (connectionStatus !== 'connected') {
        setBleStep('connecting');
        await scanAndConnect();
      }

      // Enviar comando
      setBleStep('sending');
      await sendCommand(command);

      setBleStep('success');
      console.log('[BLE] Comando enviado:', command);
    } catch (e: unknown) {
      setBleStep('error');
      setBleError(e instanceof Error ? e.message : 'Erro desconhecido.');
    }
  };

  // Dispara automaticamente uma única vez ao montar a tela
  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;
    runBleSequence();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleBack = () => {
    navigation.popToTop();
  };

  const stepConfig = STEP_CONFIG[bleStep];
  const canGoBack = bleStep === 'success' || bleStep === 'error';

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <View style={styles.content}>
          {/* Ícone de sucesso do pagamento */}
          <View style={styles.iconContainer}>
            <Text style={styles.icon}>✅</Text>
          </View>

          <Text style={styles.title}>Pagamento realizado{'\n'}com sucesso!</Text>

          {/* Detalhes da transação */}
          <View style={styles.detailsCard}>
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Forma de pagamento</Text>
              <Text style={styles.detailValue}>{PAYMENT_LABELS[paymentMethod]}</Text>
            </View>
            <View style={styles.divider} />
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Tempo liberado</Text>
              <Text style={styles.detailValue}>{totalMinutes} minutos</Text>
            </View>
            <View style={styles.divider} />
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Valor cobrado</Text>
              <Text style={[styles.detailValue, styles.priceValue]}>
                {formatCurrency(totalPrice)}
              </Text>
            </View>
          </View>

          {/* Status do envio BLE */}
          <View style={[styles.bleCard, bleStep === 'error' && styles.bleCardError]}>
            <View style={styles.bleRow}>
              {stepConfig.showSpinner ? (
                <ActivityIndicator color={stepConfig.color} size="small" />
              ) : (
                <Text style={styles.bleIcon}>{stepConfig.icon}</Text>
              )}
              <Text style={[styles.bleLabel, { color: stepConfig.color }]}>
                {bleStep === 'error' ? bleError ?? 'Erro desconhecido.' : stepConfig.label}
              </Text>
            </View>

            {bleStep === 'error' && (
              <Button
                label="Tentar novamente"
                onPress={runBleSequence}
                variant="outline"
                style={styles.retryButton}
              />
            )}
          </View>
        </View>

        <Button
          label="Voltar ao início"
          onPress={handleBack}
          variant="outline"
          disabled={!canGoBack}
          style={styles.button}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'space-between',
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 20,
  },
  iconContainer: {
    width: 96,
    height: 96,
    borderRadius: 48,
    backgroundColor: '#E6F4EA',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  icon: {
    fontSize: 48,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#202124',
    textAlign: 'center',
    lineHeight: 32,
  },
  detailsCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 20,
    width: '100%',
    gap: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  detailLabel: {
    fontSize: 14,
    color: '#5F6368',
  },
  detailValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#202124',
  },
  priceValue: {
    color: '#1A73E8',
    fontSize: 16,
  },
  divider: {
    height: 1,
    backgroundColor: '#E8EAED',
  },
  bleCard: {
    backgroundColor: '#F1F3F4',
    borderRadius: 12,
    padding: 16,
    width: '100%',
    gap: 12,
  },
  bleCardError: {
    backgroundColor: '#FDE8E8',
  },
  bleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  bleIcon: {
    fontSize: 18,
  },
  bleLabel: {
    fontSize: 14,
    fontWeight: '600',
    flex: 1,
  },
  retryButton: {
    paddingVertical: 10,
  },
  button: {
    marginTop: 16,
  },
});
