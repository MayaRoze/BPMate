#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_L3GD20_U.h>
#include <Adafruit_LSM303_U.h>
#include "BluetoothSerial.h"

/* ===== LED (pin 16) ===== */
static const int LED_PIN = 16;
static const unsigned long BLINK_PERIOD_MS = 300;
static const unsigned long BLINK_WINDOW_MS = 5000; // blink 5s after boot/disconnect, then OFF

bool ledBlinkState = false;
unsigned long lastBlinkMs = 0;
unsigned long blinkUntilMs = 0;

/* Sensors */
Adafruit_L3GD20_Unified gyro  = Adafruit_L3GD20_Unified(20);
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);

/* Bluetooth */
BluetoothSerial SerialBT;
String device_name = "ESP32-BT-Happy";

/* Config */
int sampleFreq = 50;        // Hz
bool flagBTConnected = false;
bool isSending = true;
unsigned long startTimeMs = 0;

/* Bluetooth checks */
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth not enabled
#endif
#if !defined(CONFIG_BT_SPP_ENABLED)
#error Bluetooth SPP not enabled
#endif

/* ===== LED helper =====
   - OFF when no one is connecting
   - BLINK while waiting for a client (device is "connecting")
   - ON when connected
*/
void updateLed(bool hasClientNow) {
  unsigned long now = millis();

  if (hasClientNow) {
    digitalWrite(LED_PIN, HIGH);     // connected -> solid ON
    return;
  }

  // disconnected
  if (now < blinkUntilMs) {
    // blink during the window
    if (now - lastBlinkMs >= BLINK_PERIOD_MS) {
      lastBlinkMs = now;
      ledBlinkState = !ledBlinkState;
      digitalWrite(LED_PIN, ledBlinkState ? HIGH : LOW);
    }
  } else {
    digitalWrite(LED_PIN, LOW);      // disconnected (outside window) -> OFF
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW); // generally OFF at boot

  SerialBT.begin(device_name);
  Serial.printf("Device \"%s\" started. Pair via Bluetooth.\n", device_name.c_str());

  /* Init gyro */
  if (!gyro.begin()) {
    Serial.println("ERROR: L3GD20 not detected!");
  }
  gyro.enableAutoRange(true);

  /* Init accelerometer */
  if (!accel.begin()) {
    Serial.println("ERROR: LSM303 not detected!");
  }

  startTimeMs = millis();

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  blinkUntilMs = millis() + BLINK_WINDOW_MS; // blink briefly after boot

}

void loop() {
  bool hasClientNow = SerialBT.hasClient();
  updateLed(hasClientNow);

  // --- Connection state tracking ---
  if (hasClientNow && !flagBTConnected) {
    Serial.println("Bluetooth client connected!");
    flagBTConnected = true;
  }
  if (!hasClientNow && flagBTConnected) {
    Serial.println("Bluetooth client disconnected!");
    flagBTConnected = false;
    blinkUntilMs = millis() + BLINK_WINDOW_MS; // blink briefly after disconnect (optional)
  }

  /* 1. Handle Bluetooth commands (only meaningful when connected) */
  if (hasClientNow && SerialBT.available()) {
    String cmd = SerialBT.readStringUntil('\n');
    cmd.trim();

    if (cmd == "START") {
      isSending = true;
      Serial.println("CMD: START");
    } else if (cmd == "STOP") {
      isSending = false;
      Serial.println("CMD: STOP");
    }
  }

  if (!hasClientNow) {
    delay(10); // tiny delay so blink timing is stable and loop isn't too hot
    return;
  }

  if (!isSending) {
    delay(10);
    return;
  }

  /* 3. Read sensors */
  sensors_event_t gyro_event;
  sensors_event_t accel_event;

  gyro.getEvent(&gyro_event);
  accel.getEvent(&accel_event);

  /* 4. Safe axis extraction */
  float aX = isnan(accel_event.acceleration.x) ? 0.0f : accel_event.acceleration.x;
  float aY = isnan(accel_event.acceleration.y) ? 0.0f : accel_event.acceleration.y;
  float aZ = isnan(accel_event.acceleration.z) ? 0.0f : accel_event.acceleration.z;

  float gX = isnan(gyro_event.gyro.x) ? 0.0f : gyro_event.gyro.x;
  float gY = isnan(gyro_event.gyro.y) ? 0.0f : gyro_event.gyro.y;
  float gZ = isnan(gyro_event.gyro.z) ? 0.0f : gyro_event.gyro.z;

  /* 5. Timestamp */
  float t = (millis() - startTimeMs) / 1000.0f;

  /* 6. CSV payload */
  String dataLine =
      String(t, 3) + "," +
      String(aX, 3) + "," + String(aY, 3) + "," + String(aZ, 3) + "," +
      String(gX, 3) + "," + String(gY, 3) + "," + String(gZ, 3);

  /* 7. Send */
  SerialBT.println(dataLine);
  Serial.print("BT Sent: ");
  Serial.println(dataLine);

  delay(1000 / sampleFreq);  // ~50 Hz
}
