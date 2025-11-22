#include <SoftwareSerial.h>

// --- CONFIGURATION FOR TEAM 1 ---
const byte TEAM_ID = 1;

// --- PINS (Standard LilyPad) ---
const int PIN_IR_TX = 13;
const int PIN_IR_RX = 8;
const int PIN_R = 9;   // PWM
const int PIN_G = 11;  // PWM
const int PIN_B = 10;  // PWM
const int PIN_BUZ = 5;
const int PIN_LDR = 12;

// XBee Pins
const int XBEE_RX = 4; 
const int XBEE_TX = 3; 

SoftwareSerial XBee(XBEE_RX, XBEE_TX);
Stream& LINK = XBee;

// --- FLAGS ---
const byte FLAG_IR    = 1;
const byte FLAG_LDR   = 2;
const byte FLAG_LIGHT = 4;
const byte FLAG_HB    = 8;

// Commands
const byte CMD_SET_RGB  = 1;
const byte CMD_BEEP     = 2;
const byte CMD_FIREMODE = 3;

// --- VARIABLES ---
bool txActive = false;
byte fireMode = 0;
unsigned long lastStatus = 0;
unsigned long lastAutoFire = 0;
bool ready = false;

// Instant Hit Tracking
bool prevIr = false; 

// --- COLOR LOGIC (Standard Common Anode) ---
void rgbCode(byte code){
  // STANDARD LILYPAD LED (Common Anode)
  // 255 = OFF, 0 = ON
  
  if(code==0) { 
    // OFF (All 255)
    analogWrite(PIN_R, 255); analogWrite(PIN_G, 255); analogWrite(PIN_B, 255); 
  }
  else if(code==1) { 
    // GREEN
    analogWrite(PIN_R, 255); analogWrite(PIN_G, 0);   analogWrite(PIN_B, 255); 
  }
  else if(code==2) { 
    // YELLOW (Mix Red and Green)
    analogWrite(PIN_R, 0);   analogWrite(PIN_G, 0);   analogWrite(PIN_B, 255); 
  }
  else if(code==3) { 
    // RED
    analogWrite(PIN_R, 0);   analogWrite(PIN_G, 255); analogWrite(PIN_B, 255); 
  }
}

// --- SENSITIVITY: WIDE NET (Matches Team 2) ---
bool irActive(){
  if(txActive) return false;
  int hits = 0;
  for(int i=0; i<30; i++){
    if(digitalRead(PIN_IR_RX) == LOW) hits++;
    delayMicroseconds(200);
  }
  // Change the threshold from 3 to 15
  return hits >= 3; 
}

// --- HELPERS ---
void beepShort(){ digitalWrite(PIN_BUZ,HIGH); delay(60); digitalWrite(PIN_BUZ,LOW); }
void beepLong(){  digitalWrite(PIN_BUZ,HIGH); delay(800); digitalWrite(PIN_BUZ,LOW); }
void chirp(){ for(int i=0;i<3;i++){ beepShort(); delay(100);} }

void fireBurst(){
  tone(PIN_IR_TX, 38000);
  txActive = true;
  delay(15);
  noTone(PIN_IR_TX);
  txActive = false;
}

bool ldrBright(){ return digitalRead(PIN_LDR)==LOW; }

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
    else if(cmd==CMD_BEEP) {
       if(arg==1) beepShort();
       else if(arg==2) beepLong();
       else if(arg==3) chirp();
    }
    else if(cmd==CMD_FIREMODE) fireMode = arg;
  }
}

void setup(){
  pinMode(PIN_IR_TX,OUTPUT);
  pinMode(PIN_IR_RX,INPUT_PULLUP);
  pinMode(PIN_R,OUTPUT); pinMode(PIN_G,OUTPUT); pinMode(PIN_B,OUTPUT);
  pinMode(PIN_BUZ,OUTPUT);
  pinMode(PIN_LDR,INPUT_PULLUP);

  XBee.begin(9600);
  Serial.begin(9600);
  
  // STARTUP SEQUENCE: Red -> Green -> OFF
  rgbCode(3); delay(200); 
  rgbCode(1); delay(200); 
  rgbCode(0); // <--- This ensures it stays OFF until game starts
}

void loop(){
  handleCommand();
  
  // Auto-Fire
  if(fireMode==2 && millis()-lastAutoFire>=200){ 
    lastAutoFire=millis(); 
    fireBurst(); 
  }
  
  // --- INSTANT HIT LOGIC ---
  bool ir = irActive();
  if(ir && !prevIr){
    sendStatus(FLAG_IR, 1);
  }
  prevIr = ir; 

  // --- HEARTBEAT ---
  if(millis()-lastStatus >= 200){
    lastStatus = millis();
    byte flags = FLAG_HB;
    if(ldrBright()) flags |= FLAG_LIGHT;
    if(ir) flags |= FLAG_IR; // Backup
    sendStatus(flags, 1);
  }
}