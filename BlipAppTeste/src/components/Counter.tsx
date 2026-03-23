import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

type CounterProps = {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  label?: string;
};

export default function Counter({
  value,
  onChange,
  min = 0,
  max = 60,
  label,
}: CounterProps) {
  const decrement = () => {
    if (value > (min ?? 0)) onChange(value - 1);
  };

  const increment = () => {
    if (max === undefined || value < max) onChange(value + 1);
  };

  return (
    <View style={styles.container}>
      {label && <Text style={styles.label}>{label}</Text>}
      <View style={styles.row}>
        <TouchableOpacity
          style={[styles.button, value <= (min ?? 0) && styles.buttonDisabled]}
          onPress={decrement}
          disabled={value <= (min ?? 0)}
          activeOpacity={0.7}
        >
          <Text style={styles.buttonText}>−</Text>
        </TouchableOpacity>

        <View style={styles.valueContainer}>
          <Text style={styles.value}>{value}</Text>
          <Text style={styles.unit}>min</Text>
        </View>

        <TouchableOpacity
          style={[styles.button, max !== undefined && value >= max && styles.buttonDisabled]}
          onPress={increment}
          disabled={max !== undefined && value >= max}
          activeOpacity={0.7}
        >
          <Text style={styles.buttonText}>+</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 8,
  },
  label: {
    fontSize: 14,
    color: '#5F6368',
    fontWeight: '500',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  button: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#1A73E8',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  buttonDisabled: {
    backgroundColor: '#BDC1C6',
    elevation: 0,
    shadowOpacity: 0,
  },
  buttonText: {
    fontSize: 24,
    color: '#FFFFFF',
    fontWeight: '600',
    lineHeight: 28,
  },
  valueContainer: {
    alignItems: 'center',
    minWidth: 60,
  },
  value: {
    fontSize: 32,
    fontWeight: '700',
    color: '#202124',
  },
  unit: {
    fontSize: 12,
    color: '#5F6368',
    marginTop: -4,
  },
});
