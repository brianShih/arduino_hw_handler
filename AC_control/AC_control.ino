#include <Dhcp.h>
#include <Dns.h>
#include <Ethernet.h>
#include <EthernetClient.h>
#include <EthernetServer.h>
#include <EthernetUdp.h>
#include <util.h>
#include <SPI.h>


byte mac[] = {
  0x00, 0xAA, 0xBB, 0xCC, 0xDE, 0x02 };

EthernetServer server(80);

char data01[] = "watertap=";

char *resp_turnon = {
"<form action=\"/?watertap=1\" method =\"post\"><BUTTON name=\"watertap\" type=\"submit\" value=\"1\">TURN ON</BUTTON></form>"
};

char *resp_turnoff = {
"<form action=\"/?watertap=0\" method =\"post\"><BUTTON name=\"watertap\" type=\"submit\" value=\"0\">TURN OFF</BUTTON></form>"
};
//"<form action=\"\" method=\"post\"><input type='button' value=\"TURN ON\"></form>"


void setup(){
  //start serial connection
  Serial.begin(9600);
  // this check is only needed on the Leonardo:
   while (!Serial) {
    ; // wait for serial port to connect. Needed for Leonardo only
  }
 
  pinMode(9, OUTPUT); 

  digitalWrite(9, 0);
 if (Ethernet.begin(mac) == 0) {
    Serial.println("Failed to configure Ethernet using DHCP");
    // no point in carrying on, so do nothing forevermore:
    for(;;)
      ;
  }
  // print your local IP address:
  Serial.print("My IP address: ");
  for (byte thisByte = 0; thisByte < 4; thisByte++) {
    // print the value of each byte of the IP address:
    Serial.print(Ethernet.localIP()[thisByte], DEC);
    Serial.print("."); 
  }
  Serial.println();
  server.begin();
  Serial.print("server is at ");
  Serial.println(Ethernet.localIP());
}

void post(EthernetClient client, int refresh){
  int status = 0;
  // send a standard http response header
  if (refresh == 1)
  {
    client.println("HTTP/1.1 301 Moved Permanently");
    client.println("Location: http://192.168.1.4");
    Serial.print("refresh the http request...\r\n");
  }
  else
    client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html");
  client.println("Connection: close");  // the connection will be closed after completion of the response
  //client.println("Refresh: 5");  // refresh the page automatically every 5 sec
  client.println();
  //client.println("<!DOCTYPE HTML>");
  client.println("<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.2//EN\" \"http://www.openmobilealliance.org/tech/DTD/xhtml-mobile12.dtd\">");
  client.println("<html>");
  status = digitalRead(9);
  client.println("WATER TAP :");
  if (status == 0) {
    //OFF status
    client.println(resp_turnon);
  } else {
    //ON status
    //<input type="button" value="文字">
    client.println(resp_turnoff);
  }
  client.println("</html>");  
}
void loop(){
  int data_index = 0;
  int http_refresh = 0;
  char tp[20];
  EthernetClient client;
  client = server.available();
  if (client) {
    boolean currentLineIsBlank = true;
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();

        if (c == '?')
        {
          while (data_index < strlen(data01) + 1)
          {
            tp[data_index] = client.read();
            data_index += 1;
          }
          http_refresh = 1;
        }

        Serial.print(c);
        if (c == '\n' && currentLineIsBlank) {
          post(client, http_refresh);
          break;
        }
        if (c == '\n') {
          // you're starting a new line
          currentLineIsBlank = true;
        }
        else if (c != '\r') {
          // you've gotten a character on the current line
          currentLineIsBlank = false;
        }
      }
      
    }
  }
  // give the web browser time to receive the data
  delay(1);
  // close the connection:
  client.stop();
  if (tp[data_index - 1] == '1')
  {
    digitalWrite(9, 1);
    Serial.println("GPIO turn on");
  }
  else if (tp[data_index - 1] == '0')
  {
    digitalWrite(9, 0);
    Serial.println("GPIO turn off");
  }
  //Serial.print("Water tap action ");
  //Serial.println(tp[data_index - 1]);
  //Serial.println("client disonnected");
}



