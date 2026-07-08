package br.com.bliqbrasil.totem.payment.stone

import android.graphics.Bitmap

sealed class PaymentState {
    object Idle : PaymentState()
    object WaitingCard : PaymentState()
    object WaitingPassword : PaymentState()
    data class WaitingQrCode(val qrBitmap: Bitmap?) : PaymentState()
    object Sending : PaymentState()
    object WaitingRemoveCard : PaymentState()
    data class Success(val result: StonePaymentResult) : PaymentState()
    data class Failure(val code: String, val message: String) : PaymentState()
    object Cancelled : PaymentState()
}

data class StonePaymentResult(
    val success: Boolean,
    val code: String,
    val message: String,
    val body: String?,
    val acquirerTransactionKey: String?,
)

enum class TransactionType {
    DEBIT,
    CREDIT,
    PIX,
}
