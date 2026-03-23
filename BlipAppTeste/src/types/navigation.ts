export type WashOption = {
  id: string;
  label: string;
  minutes: number;
  price: number;
};

export type PaymentMethod = 'credit' | 'debit' | 'pix';

export type RootStackParamList = {
  Home: undefined;
  Extras: {
    washOption: WashOption;
  };
  Checkout: {
    washOption: WashOption;
    extraMinutes: number;
    totalPrice: number;
  };
  Success: {
    paymentMethod: PaymentMethod;
    totalMinutes: number;
    totalPrice: number;
  };
};
