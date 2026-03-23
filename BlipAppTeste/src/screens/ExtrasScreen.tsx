import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { RootStackParamList } from '../types/navigation';
import Button from '../components/Button';
import Counter from '../components/Counter';
import { formatCurrency } from '../services/paymentService';

const EXTRA_MINUTE_PRICE = 2;

type Props = {
  navigation: NativeStackNavigationProp<RootStackParamList, 'Extras'>;
  route: RouteProp<RootStackParamList, 'Extras'>;
};

export default function ExtrasScreen({ navigation, route }: Props) {
  const { washOption } = route.params;
  const [extraMinutes, setExtraMinutes] = useState(0);

  const extraCost = extraMinutes * EXTRA_MINUTE_PRICE;
  const totalPrice = washOption.price + extraCost;
  const totalMinutes = washOption.minutes + extraMinutes;

  const handleContinue = () => {
    navigation.navigate('Checkout', {
      washOption,
      extraMinutes,
      totalPrice,
    });
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Minutos Extras</Text>
          <Text style={styles.subtitle}>Adicione tempo à sua lavagem</Text>
        </View>

        <View style={styles.summaryCard}>
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>Lavagem escolhida</Text>
            <Text style={styles.summaryValue}>{washOption.label}</Text>
          </View>
          <View style={styles.divider} />
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>Tempo base</Text>
            <Text style={styles.summaryValue}>{washOption.minutes} min</Text>
          </View>
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>Valor base</Text>
            <Text style={styles.summaryValue}>{formatCurrency(washOption.price)}</Text>
          </View>
        </View>

        <View style={styles.counterCard}>
          <Text style={styles.counterTitle}>Minutos extras</Text>
          <Text style={styles.counterHint}>R$ 2,00 por minuto</Text>
          <Counter
            value={extraMinutes}
            onChange={setExtraMinutes}
            min={0}
            max={60}
          />
          {extraMinutes > 0 && (
            <Text style={styles.extraCostText}>
              + {formatCurrency(extraCost)} em extras
            </Text>
          )}
        </View>

        <View style={styles.totalCard}>
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Tempo total</Text>
            <Text style={styles.totalMinutes}>{totalMinutes} minutos</Text>
          </View>
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Valor total</Text>
            <Text style={styles.totalPrice}>{formatCurrency(totalPrice)}</Text>
          </View>
        </View>

        <Button
          label="Continuar para pagamento"
          onPress={handleContinue}
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
    paddingVertical: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#202124',
  },
  subtitle: {
    fontSize: 14,
    color: '#5F6368',
    marginTop: 4,
  },
  summaryCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    gap: 10,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  summaryLabel: {
    fontSize: 14,
    color: '#5F6368',
  },
  summaryValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#202124',
  },
  divider: {
    height: 1,
    backgroundColor: '#E8EAED',
  },
  counterCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 24,
    alignItems: 'center',
    gap: 8,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
  },
  counterTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#202124',
  },
  counterHint: {
    fontSize: 13,
    color: '#5F6368',
    marginBottom: 8,
  },
  extraCostText: {
    fontSize: 14,
    color: '#34A853',
    fontWeight: '600',
    marginTop: 8,
  },
  totalCard: {
    backgroundColor: '#1A73E8',
    borderRadius: 14,
    padding: 20,
    gap: 8,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  totalLabel: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.8)',
  },
  totalMinutes: {
    fontSize: 15,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  totalPrice: {
    fontSize: 22,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  button: {
    marginTop: 8,
  },
});
