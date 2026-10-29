package br.com.bliqbrasil.totem.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Admin do dispositivo. Só existe para o app poder virar *device owner*
 * (uma vez por terminal, via adb) — é o que permite travar a tela sem a
 * confirmação do Android e sem a saída por Voltar + Início.
 *
 *   adb shell dpm set-device-owner br.com.bliqbrasil.totem/.kiosk.KioskAdminReceiver
 */
class KioskAdminReceiver : DeviceAdminReceiver()
