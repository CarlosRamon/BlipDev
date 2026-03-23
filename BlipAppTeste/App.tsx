import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { RootStackParamList } from './src/types/navigation';
import { BluetoothProvider } from './src/context/BluetoothContext';
import HomeScreen from './src/screens/HomeScreen';
import ExtrasScreen from './src/screens/ExtrasScreen';
import CheckoutScreen from './src/screens/CheckoutScreen';
import SuccessScreen from './src/screens/SuccessScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <BluetoothProvider>
      <NavigationContainer>
        <StatusBar style="light" />
        <Stack.Navigator
          initialRouteName="Home"
          screenOptions={{
            headerStyle: { backgroundColor: '#1A73E8' },
            headerTintColor: '#FFFFFF',
            headerTitleStyle: { fontWeight: '600', fontSize: 17 },
            headerBackTitleVisible: false,
            contentStyle: { backgroundColor: '#F8F9FA' },
          }}
        >
          <Stack.Screen
            name="Home"
            component={HomeScreen}
            options={{ title: 'Posto de Lavagem' }}
          />
          <Stack.Screen
            name="Extras"
            component={ExtrasScreen}
            options={{ title: 'Minutos Extras' }}
          />
          <Stack.Screen
            name="Checkout"
            component={CheckoutScreen}
            options={{ title: 'Checkout' }}
          />
          <Stack.Screen
            name="Success"
            component={SuccessScreen}
            options={{
              title: 'Pagamento Confirmado',
              headerBackVisible: false,
              gestureEnabled: false,
            }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </BluetoothProvider>
  );
}
