import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { RootStackParamList, PaymentMethod } from '../types/navigation';
import Button from '../components/Button';
import { processPayment, formatCurrency } from '../services/paymentService';

type Props = {
  navigation: NativeStackNavigationProp<RootStackParamList, 'Checkout'>;
  route: RouteProp<RootStackParamList, 'Checkout'>;
};

type PaymentOption = {
  method: PaymentMethod;
  label: string;
  icon: string;
};

const PAYMENT_OPTIONS: PaymentOption[] = [
  { method: 'credit', label: 'Crédito', icon: '💳' },
  { method: 'debit', label: 'Débito', icon: '🏦' },
  { method: 'pix', label: 'Pix', icon: '⚡' },
];

export default function CheckoutScreen({ navigation, route }: Props) {
  const { washOption, extraMinutes, totalPrice } = route.params;
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod | null>(null);
  const [loading, setLoading] = useState(false);

  const totalMinutes = washOption.minutes + extraMinutes;

  const handlePay = async () => {
    if (!selectedMethod) return;

    setLoading(true);
    try {
      await processPayment(selectedMethod, totalPrice);
      navigation.navigate('Success', {
        paymentMethod: selectedMethod,
        totalMinutes,
        totalPrice,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Resumo do Pedido</Text>
        </View>

        <View style={styles.summaryCard}>
          <Text style={styles.sectionTitle}>Detalhes da lavagem</Text>

          <View style={styles.row}>
            <Text style={styles.rowLabel}>Tipo</Text>
            <Text style={styles.rowValue}>{washOption.label}</Text>
          </View>
          <View style={styles.divider} />
          <View style={styles.row}>
            <Text style={styles.rowLabel}>Tempo base</Text>
            <Text style={styles.rowValue}>{washOption.minutes} min</Text>
          </View>
          {extraMinutes > 0 && (
            <>
              <View style={styles.row}>
                <Text style={styles.rowLabel}>Tempo extra</Text>
                <Text style={styles.rowValue}>+ {extraMinutes} min</Text>
              </View>
            </>
          )}
          <View style={styles.divider} />
          <View style={styles.row}>
            <Text style={styles.rowLabel}>Tempo total</Text>
            <Text style={[styles.rowValue, styles.highlight]}>{totalMinutes} minutos</Text>
          </View>
        </View>

        <View style={styles.totalCard}>
          <Text style={styles.totalLabel}>Total a pagar</Text>
          <Text style={styles.totalValue}>{formatCurrency(totalPrice)}</Text>
        </View>

        <View style={styles.paymentSection}>
          <Text style={styles.sectionTitle}>Forma de pagamento</Text>
          <View style={styles.paymentOptions}>
            {PAYMENT_OPTIONS.map((option) => {
              const isSelected = selectedMethod === option.method;
              return (
                <TouchableOpacity
                  key={option.method}
                  style={[styles.paymentCard, isSelected && styles.paymentCardSelected]}
                  onPress={() => setSelectedMethod(option.method)}
                  activeOpacity={0.8}
                >
                  <Text style={styles.paymentIcon}>{option.icon}</Text>
                  <Text style={[styles.paymentLabel, isSelected && styles.paymentLabelSelected]}>
                    {option.label}
                  </Text>
                  {isSelected && <View style={styles.paymentCheck} />}
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        <Button
          label={loading ? 'Processando...' : 'Confirmar Pagamento'}
          onPress={handlePay}
          disabled={!selectedMethod}
          loading={loading}
          style={styles.button}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  container: {
    flexGrow: 1,
    padding: 24,
    gap: 16,
  },
  header: {
    alignItems: 'center',
    paddingVertical: 8,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#202124',
  },
  summaryCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 20,
    gap: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    color: '#9AA0A6',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  rowLabel: {
    fontSize: 14,
    color: '#5F6368',
  },
  rowValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#202124',
  },
  highlight: {
    color: '#1A73E8',
    fontSize: 16,
  },
  divider: {
    height: 1,
    backgroundColor: '#E8EAED',
  },
  totalCard: {
    backgroundColor: '#1A73E8',
    borderRadius: 14,
    padding: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  totalLabel: {
    fontSize: 15,
    color: 'rgba(255,255,255,0.85)',
    fontWeight: '500',
  },
  totalValue: {
    fontSize: 26,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  paymentSection: {
    gap: 12,
  },
  paymentOptions: {
    flexDirection: 'row',
    gap: 12,
  },
  paymentCard: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    gap: 6,
    borderWidth: 2,
    borderColor: 'transparent',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 3,
  },
  paymentCardSelected: {
    borderColor: '#1A73E8',
    backgroundColor: '#EAF1FB',
  },
  paymentIcon: {
    fontSize: 28,
  },
  paymentLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#5F6368',
  },
  paymentLabelSelected: {
    color: '#1A73E8',
  },
  paymentCheck: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#1A73E8',
    marginTop: 2,
  },
  button: {
    marginTop: 8,
  },
});
