#include <SoftwareSerial.h>

const byte TEAM_ID = 2;

// --- PINS (Team 2 Special) ---
const int PIN_R = 5;  // PWM
const int PIN_B = 6;  // PWM
const int PIN_G = 7;  // DIGITAL ONLY (Cannot Dim)

const int PIN_IR_TX = 13;
const int PIN_IR_RX = 3;
const int PIN_BUZ   = 8;
const int PIN_LDR   = 4;

const int XBEE_RX = 10; 
const int XBEE_TX = 11;

SoftwareSerial XBee(XBEE_RX, XBEE_TX);
Stream& LINK = XBee;

const byte FLAG_IR    = 1;
const byte FLAG_LDR   = 2;
const byte FLAG_LIGHT = 4;
const byte FLAG_HB    = 8;

const byte CMD_SET_RGB  = 1;
const byte CMD_BEEP     = 2;
const byte CMD_FIREMODE = 3;

bool txActive = false;
byte fireMode = 0;
unsigned long lastStatus = 0;
unsigned long lastAutoFire = 0;
bool prevIr = false; 

// --- HELPER FUNCTIONS DEFINED FIRST ---

void beepShort(){ digitalWrite(PIN_BUZ,HIGH); delay(60); digitalWrite(PIN_BUZ,LOW); }
void beepLong(){  digitalWrite(PIN_BUZ,HIGH); delay(800); digitalWrite(PIN_BUZ,LOW); }
void chirp(){ for(int i=0;i<3;i++){ beepShort(); delay(100);} }

void fireBurst(){
  tone(PIN_IR_TX, 38000); txActive = true; delay(15); noTone(PIN_IR_TX); txActive = false;
}

bool ldrBright(){ return digitalRead(PIN_LDR)==LOW; }

// --- COLOR LOGIC: FIXED FOR DIGITAL GREEN ---
void rgbCode(byte code){
  // COMMON CATHODE: 255=ON, 0=OFF
  if(code==0) { 
    // OFF
    analogWrite(PIN_R, 0); 
    analogWrite(PIN_B, 0); 
    digitalWrite(PIN_G, LOW); 
  }
  else if(code==1) { 
    // GREEN (Healthy)
    analogWrite(PIN_R, 0); 
    analogWrite(PIN_B, 0); 
    digitalWrite(PIN_G, HIGH); // Green ON
  }
  else if(code==2) { 
    // HURT STATE -> BLUE
    // We swap Yellow for Blue because Green is too bright to mix
    analogWrite(PIN_R, 0); 
    analogWrite(PIN_B, 255);   // Blue ON
    digitalWrite(PIN_G, LOW);  // Green OFF (Crucial!)
  }
  else if(code==3) { 
    // RED (Dead)
    analogWrite(PIN_R, 255);   // Red ON
    analogWrite(PIN_B, 0); 
    digitalWrite(PIN_G, LOW);  // Green OFF
  }
}

void sendStatus(byte flags, byte value){
  byte s = (byte)((TEAM_ID + flags + value) & 0xFF);
  LINK.write(TEAM_ID); LINK.write(flags); LINK.write(value); LINK.write(s);
}

void handleCommand(){
  while(LINK.available() >= 4){
    byte team = LINK.read();
    byte cmd  = LINK.read();
    byte arg  = LINK.read();
    byte sum  = LINK.read();
    if((byte)((team+cmd+arg)&0xFF) != sum) continue;
    if(team != TEAM_ID && team != 0xFF) continue;

    if(cmd==CMD_SET_RGB) rgbCode(arg);
    else if(cmd==CMD_BEEP) { if(arg==1) beepShort(); else if(arg==2) beepLong(); else if(arg==3) chirp(); }
    else if(cmd==CMD_FIREMODE) fireMode = arg;
  }
}

// --- STRICT SENSITIVITY (Anti-Noise) ---
bool irActive(){
  if(txActive) return false; // Don't detect our own shot
  
  int hits = 0;
  int samples = 60; // INCREASED SAMPLES from 30 to 60 (Listen longer)
  
  for(int i=0; i<samples; i++){
    // READ THE PIN
    if(digitalRead(PIN_IR_RX) == LOW) {
      hits++;
    }
    delayMicroseconds(100); // Faster sampling to catch the frequency
  }
  
  // STRICT THRESHOLD: 
  // We took 60 samples. 
  // We require 40 hits to filter out noise.
  return hits >= 3; 
}

void setup(){
  pinMode(PIN_IR_TX,OUTPUT);
  pinMode(PIN_IR_RX,INPUT_PULLUP);
  pinMode(PIN_R,OUTPUT); pinMode(PIN_B,OUTPUT); pinMode(PIN_G,OUTPUT);
  pinMode(PIN_BUZ,OUTPUT);
  pinMode(PIN_LDR,INPUT_PULLUP);

  XBee.begin(9600); Serial.begin(9600);
  
  // STARTUP FLASH
  rgbCode(3); delay(250); // Red
  rgbCode(1); delay(250); // Green
  rgbCode(0);             // Off
}

void loop(){
  // 1. LISTEN FOR COMMANDS (Critical!)
  handleCommand(); 
  
  // 2. Run the Auto-Fire logic
  if(fireMode==2 && millis()-lastAutoFire>=200){ lastAutoFire=millis(); fireBurst(); }
  
  // 3. Run the NEW Strict Sensor Logic
  bool ir = irActive();
  if(ir && !prevIr) sendStatus(FLAG_IR, 1);
  prevIr = ir; 

  // 4. Run the Heartbeat
  if(millis()-lastStatus >= 200){
    lastStatus = millis();
    byte flags = FLAG_HB;
    if(ldrBright()) flags |= FLAG_LIGHT;
    if(ir) flags |= FLAG_IR;
    sendStatus(flags, 1);
  }
}