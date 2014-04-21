int led        = 13;
const int sw1  =  4;     // the number of the pushbutton pin
const int sw2  =  3;     // the number of the pushbutton pin
const int sw3  =  2;     // the number of the pushbutton pin
int sw1_state = 0;
int sw2_state = 0;
int sw3_state = 0;
int trg_count = 1;

void setup(){
  // define the PIN as OUTPUT
  pinMode(led,OUTPUT); 
  pinMode(sw1, INPUT);
  pinMode(sw2, INPUT);
  pinMode(sw3, INPUT);
  // using baud rate 9600 to listen.
  Serial.begin(9600);
}

void loop(){
  while (Serial.available()){
    char cCmd = Serial.read();
    if (cCmd =='1'){
      digitalWrite(led,HIGH);
    }else if (cCmd == '0'){
      digitalWrite(led,LOW);
    }
  }

  sw1_state = digitalRead(sw1);
  sw2_state = digitalRead(sw2);
  sw3_state = digitalRead(sw3);

  if (sw1_state == HIGH)
    Serial.write("1");   // string even
  if (sw2_state == HIGH)
    Serial.write("2");   // voice even
  if (sw3_state == HIGH)
    Serial.write("3");   // video even
  delay(150); 
}

