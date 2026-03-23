// ============================================================
//  BlipEsp32 — Terminal BLE para Posto de Lavagem
//  Arduino IDE version
//
//  Dependencias (instalar pelo Library Manager):
//    - ArduinoJson  (by Benoit Blanchon)  versao 6.x
//
//  Placa: ESP32 Dev Module
//  Board Manager URL:
//    https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
// ============================================================

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>

// ── UUIDs (devem ser identicos ao app BlipTeste) ─────────────
#define SERVICE_UUID        "12345678-1234-1234-1234-1234567890ab"
#define CHARACTERISTIC_UUID "abcd1234-5678-90ab-cdef-1234567890ab"
#define DEVICE_NAME         "ESP32_LAVAGEM"

// ── Pinos ────────────────────────────────────────────────────
#define LED_PIN 2   // LED onboard

// ── Estado global ─────────────────────────────────────────────
BLEServer*         bleServer         = nullptr;
BLECharacteristic* bleCharacteristic = nullptr;

bool deviceConnected     = false;
bool wasConnected        = false;

bool     machineRunning  = false;
int      totalMinutes    = 0;
unsigned long startedAt  = 0;

unsigned long lastLogTime = 0;
#define LOG_INTERVAL_MS 10000

// ── Callbacks de conexao ──────────────────────────────────────
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* server) override {
        deviceConnected = true;
        Serial.println("\n╔════════════════════════════════╗");
        Serial.println(  "║  [BLE] Dispositivo CONECTADO   ║");
        Serial.println(  "╚════════════════════════════════╝\n");
    }
    void onDisconnect(BLEServer* server) override {
        deviceConnected = false;
        wasConnected    = true;
        Serial.println("\n╔════════════════════════════════╗");
        Serial.println(  "║ [BLE] Dispositivo DESCONECTADO ║");
        Serial.println(  "╚════════════════════════════════╝\n");
    }
};

// ── Callbacks de escrita na characteristic ────────────────────
class MyCharCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* ch) override {
        String raw = ch->getValue().c_str();

        Serial.println("[BLE] Dados recebidos:");
        Serial.print  ("  Raw: ");
        Serial.println(raw);

        StaticJsonDocument<256> doc;
        DeserializationError err = deserializeJson(doc, raw);

        if (err) {
            Serial.print("  [ERRO] JSON invalido: ");
            Serial.println(err.c_str());
            return;
        }

        const char* action   = doc["action"]  | "UNKNOWN";
        int         duration = doc["duration"] | 0;

        Serial.print("  Action:   "); Serial.println(action);
        Serial.print("  Duration: "); Serial.print(duration); Serial.println(" minutos\n");

        if (strcmp(action, "START") == 0 && duration > 0) {
            machineRunning = true;
            totalMinutes   = duration;
            startedAt      = millis();

            Serial.println("╔═══════════════════════════════════╗");
            Serial.println("║   MAQUINA LIBERADA — INICIANDO    ║");
            Serial.print  ("║   Tempo total: ");
            Serial.print  (duration);
            Serial.println(" minutos          ║");
            Serial.println("╚═══════════════════════════════════╝\n");

            ch->setValue("{\"status\":\"OK\",\"message\":\"Maquina liberada\"}");
            ch->notify();

        } else if (strcmp(action, "STOP") == 0) {
            machineRunning = false;
            Serial.println("[CMD] Maquina PARADA.");
            ch->setValue("{\"status\":\"OK\",\"message\":\"Maquina parada\"}");
            ch->notify();

        } else {
            Serial.print("[AVISO] Comando desconhecido: ");
            Serial.println(action);
            ch->setValue("{\"status\":\"ERROR\",\"message\":\"Comando desconhecido\"}");
            ch->notify();
        }
    }
};

// ── Helpers ───────────────────────────────────────────────────
void blinkLed(int times, int delayMs = 150) {
    for (int i = 0; i < times; i++) {
        digitalWrite(LED_PIN, HIGH); delay(delayMs);
        digitalWrite(LED_PIN, LOW);  delay(delayMs);
    }
}

void printBanner() {
    Serial.println("\n╔══════════════════════════════════════════╗");
    Serial.println(  "║     BlipEsp32 — Posto de Lavagem         ║");
    Serial.println(  "║     BLE Terminal de Maquina              ║");
    Serial.println(  "╚══════════════════════════════════════════╝\n");
}

void logMachineStatus() {
    if (!machineRunning) return;

    unsigned long elapsed   = (millis() - startedAt) / 1000;
    unsigned long total     = (unsigned long)totalMinutes * 60;
    unsigned long remaining = (total > elapsed) ? (total - elapsed) : 0;

    int remMin = remaining / 60;
    int remSec = remaining % 60;

    Serial.println("┌─────────────────────────────────┐");
    Serial.println("│      STATUS DA MAQUINA           │");
    Serial.print  ("│  Programa:  "); Serial.print(totalMinutes); Serial.println(" min");
    Serial.print  ("│  Decorrido: "); Serial.print(elapsed); Serial.println("s");
    Serial.print  ("│  Restante:  ");
    if (remMin < 10) Serial.print("0");
    Serial.print(remMin); Serial.print(":");
    if (remSec < 10) Serial.print("0");
    Serial.println(remSec);

    if (remaining == 0) {
        machineRunning = false;
        Serial.println("│  Status: CONCLUIDO ✓");
        Serial.println("└─────────────────────────────────┘\n");
        digitalWrite(LED_PIN, LOW);
        blinkLed(5, 100);
    } else {
        Serial.println("│  Status: EM ANDAMENTO ...");
        Serial.println("└─────────────────────────────────┘\n");
    }
}

// ── Setup ─────────────────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    delay(500);
    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, LOW);

    printBanner();

    BLEDevice::init(DEVICE_NAME);
    BLEDevice::setPower(ESP_PWR_LVL_P9);

    bleServer = BLEDevice::createServer();
    bleServer->setCallbacks(new MyServerCallbacks());

    BLEService* service = bleServer->createService(SERVICE_UUID);

    bleCharacteristic = service->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ   |
        BLECharacteristic::PROPERTY_WRITE  |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    bleCharacteristic->addDescriptor(new BLE2902());
    bleCharacteristic->setCallbacks(new MyCharCallbacks());
    bleCharacteristic->setValue("{\"status\":\"READY\"}");

    service->start();

    BLEAdvertising* advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    advertising->setMinPreferred(0x06);
    advertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] Advertising iniciado — aguardando conexao...");
    Serial.print  ("[BLE] Nome:               "); Serial.println(DEVICE_NAME);
    Serial.print  ("[BLE] Service UUID:        "); Serial.println(SERVICE_UUID);
    Serial.print  ("[BLE] Characteristic UUID: "); Serial.println(CHARACTERISTIC_UUID);
    Serial.println();

    blinkLed(3);
    Serial.println("[SISTEMA] Pronto. Aguardando app BlipTeste...\n");
}

// ── Loop ──────────────────────────────────────────────────────
void loop() {
    // Reinicia advertising apos desconexao
    if (wasConnected && !deviceConnected) {
        delay(500);
        BLEDevice::startAdvertising();
        wasConnected = false;
        Serial.println("[BLE] Re-advertising iniciado...\n");
    }

    // Controle do LED
    if (!deviceConnected) {
        digitalWrite(LED_PIN, LOW);
    } else if (machineRunning) {
        digitalWrite(LED_PIN, (millis() / 500) % 2); // pisca
    } else {
        digitalWrite(LED_PIN, HIGH); // aceso fixo = conectado
    }

    // Log periodico da maquina
    if (machineRunning) {
        unsigned long now = millis();
        if (now - lastLogTime >= LOG_INTERVAL_MS) {
            lastLogTime = now;
            logMachineStatus();
        }

        // Verifica fim do tempo
        unsigned long elapsed = (now - startedAt) / 1000;
        unsigned long total   = (unsigned long)totalMinutes * 60;
        if (elapsed >= total) {
            machineRunning = false;
            Serial.println("\n╔══════════════════════════════════╗");
            Serial.println(  "║   TEMPO ESGOTADO — MAQUINA OFF   ║");
            Serial.println(  "╚══════════════════════════════════╝\n");
            digitalWrite(LED_PIN, LOW);
            blinkLed(5, 100);
        }
    }

    delay(100);
}
