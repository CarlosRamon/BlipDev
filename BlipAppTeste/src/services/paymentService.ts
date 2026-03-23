import { PaymentMethod } from '../types/navigation';

export type PaymentResult = {
  success: boolean;
  transactionId: string;
  method: PaymentMethod;
  amount: number;
  timestamp: Date;
};

function generateTransactionId(): string {
  return Math.random().toString(36).substring(2, 10).toUpperCase();
}

export async function processPayment(
  method: PaymentMethod,
  amount: number
): Promise<PaymentResult> {
  // Simula latência de processamento de pagamento
  await new Promise((resolve) => setTimeout(resolve, 2000));

  return {
    success: true,
    transactionId: generateTransactionId(),
    method,
    amount,
    timestamp: new Date(),
  };
}

export function liberarMaquina(totalMinutes: number): void {
  console.log(`[MachineService] Liberando máquina por ${totalMinutes} minutos.`);
  console.log('[MachineService] Comando enviado: START_WASH');
  console.log(`[MachineService] Timer configurado: ${totalMinutes * 60} segundos`);
}

export function formatCurrency(value: number): string {
  return value.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}
