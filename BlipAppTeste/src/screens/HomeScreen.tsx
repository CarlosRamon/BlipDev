import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList, WashOption } from '../types/navigation';
import { formatCurrency } from '../services/paymentService';
import { useBluetooth } from '../context/BluetoothContext';
import BleStatusBar from '../components/BleStatusBar';

type Props = {
  navigation: NativeStackNavigationProp<RootStackParamList, 'Home'>;
};

const WASH_OPTIONS: WashOption[] = [
  { id: 'wash-10', label: 'Lavagem Rápida', minutes: 10, price: 30 },
  { id: 'wash-20', label: 'Lavagem Completa', minutes: 20, price: 50 },
];

export default function HomeScreen({ navigation }: Props) {
  const { connectionStatus, errorMessage, scanAndConnect, disconnectDevice } = useBluetooth();

  const handleSelect = (option: WashOption) => {
    navigation.navigate('Extras', { washOption: option });
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <Text style={styles.emoji}>🚗</Text>
          <Text style={styles.title}>Posto de Lavagem</Text>
          <Text style={styles.subtitle}>Selecione o tipo de lavagem</Text>
        </View>

        {/* Painel de conexão BLE */}
        <BleStatusBar
          status={connectionStatus}
          errorMessage={errorMessage}
          onConnect={scanAndConnect}
          onDisconnect={disconnectDevice}
        />

        <View style={styles.optionsList}>
          {WASH_OPTIONS.map((option) => (
            <TouchableOpacity
              key={option.id}
              style={styles.card}
              onPress={() => handleSelect(option)}
              activeOpacity={0.85}
            >
              <View style={styles.cardLeft}>
                <Text style={styles.cardLabel}>{option.label}</Text>
                <Text style={styles.cardMinutes}>{option.minutes} minutos</Text>
              </View>
              <View style={styles.cardRight}>
                <Text style={styles.cardPrice}>{formatCurrency(option.price)}</Text>
                <Text style={styles.cardArrow}>›</Text>
              </View>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.hint}>Minutos extras disponíveis na próxima tela</Text>
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
    paddingVertical: 24,
  },
  emoji: {
    fontSize: 56,
    marginBottom: 12,
  },
  title: {
    fontSize: 26,
    fontWeight: '700',
    color: '#202124',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 15,
    color: '#5F6368',
    marginTop: 6,
    textAlign: 'center',
  },
  optionsList: {
    gap: 16,
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    borderLeftWidth: 4,
    borderLeftColor: '#1A73E8',
  },
  cardLeft: {
    flex: 1,
  },
  cardLabel: {
    fontSize: 17,
    fontWeight: '600',
    color: '#202124',
  },
  cardMinutes: {
    fontSize: 13,
    color: '#5F6368',
    marginTop: 4,
  },
  cardRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cardPrice: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1A73E8',
  },
  cardArrow: {
    fontSize: 24,
    color: '#BDC1C6',
    fontWeight: '300',
  },
  hint: {
    textAlign: 'center',
    color: '#9AA0A6',
    fontSize: 12,
    marginTop: 8,
  },
});
