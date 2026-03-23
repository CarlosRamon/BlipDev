# BlipEsp32 — Terminal BLE para Posto de Lavagem

Firmware para ESP32 que se comunica via BLE com o app **BlipTeste**.

## Requisitos

- [PlatformIO](https://platformio.org/) (VS Code extension ou CLI)
- ESP32 Dev Module (qualquer variante com BLE)

## Instalação

```bash
# Instalar PlatformIO CLI (se nao tiver)
pip install platformio

# Compilar e gravar
pio run --target upload

# Abrir monitor serial
pio device monitor
```

Ou pelo VS Code: instale a extensão **PlatformIO IDE** e use os botões Build/Upload/Monitor.

## UUIDs BLE

| Item              | UUID                                   |
|-------------------|----------------------------------------|
| Service           | `12345678-1234-1234-1234-1234567890ab` |
| Characteristic    | `abcd1234-5678-90ab-cdef-1234567890ab` |
| Nome do dispositivo | `ESP32_LAVAGEM`                      |

## Protocolo de comunicação

### App → ESP32 (Write)
```json
{ "action": "START", "duration": 15 }
{ "action": "STOP" }
```

### ESP32 → App (Notify)
```json
{ "status": "OK",    "message": "Maquina liberada" }
{ "status": "ERROR", "message": "Comando desconhecido" }
```

## LED onboard (GPIO 2)

| Estado          | LED        |
|-----------------|------------|
| Desconectado    | Apagado    |
| Conectado       | Aceso fixo |
| Maquina rodando | Piscando   |
| Tempo esgotado  | 5 blinks   |

## Logs Serial (115200 baud)

```
╔══════════════════════════════════════════╗
║         BlipEsp32 — Posto de Lavagem     ║
╚══════════════════════════════════════════╝
[BLE] Advertising iniciado — aguardando conexao...

╔════════════════════════════════╗
║  [BLE] Dispositivo CONECTADO  ║
╚════════════════════════════════╝

[BLE] Dados recebidos:
  Raw: {"action":"START","duration":15}
  Action:   START
  Duration: 15 minutos

╔═══════════════════════════════════╗
║   MAQUINA LIBERADA — INICIANDO   ║
║   Tempo total: 15 minutos        ║
╚═══════════════════════════════════╝
```
