package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import java.util.*;
import java.util.concurrent.TimeUnit;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import com.lightbend.akka.sample.SensorSamples;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.text.SimpleDateFormat;

public class Server extends AbstractActor {
  private final HashMap<String, Boolean> adminNames;
  private final HashMap<String, SensorSamples> sensors;
  private static final String adminPassword = "12345";
  private static int day = 0;
  private static int sensorSerial = 1;


  static public Props props() {
    return Props.create(Server.class, () -> new Server());
  }


  public Server() {
    this.adminNames = new HashMap<>();
    this.sensors = new HashMap<>();
    adminNames.put("admin",false);
    adminNames.put("yochevet",false);
  }

  @Override
  public Receive createReceive() {
    updateDate();
    return receiveBuilder()
            .match(Connect.class, this::connect)
            .match(DisConnect.class, this::disConnect)
            .match(SendMeasurement.class, this::sendMeasurement)
            .match(GetInfoFile.class, this::getInfoFile)
            .match(GetInfo.class, this::getInfo)
            .build();
  }

  private void updateDate(){
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    int curDay =  Integer.parseInt(formatter.format(date).split("-")[0]);
    this.day=curDay;
  }

  private void connect(Connect connect) {
    String response;
    if(connect.userName.equals("sensor")){
      String str = "Sensor-"+this.sensorSerial;
      this.sensorSerial++;
      response = str+" connected to server";
      //add to DB
      SensorSamples newSensorSample = new SensorSamples(this.day);
      this.sensors.put(str, newSensorSample);
    }
    else {
      if (adminNames.getOrDefault(connect.userName, true)) {
        if (!adminNames.containsKey(connect.userName))
          response = connect.userName + " is not legal admin name";
        else
          response = connect.userName + " is not available";
      } else {
        if (!connect.password.equals(adminPassword)) {
          response = "Wrong password!";
        } else {
          response = connect.userName + " has connected successfully";
          adminNames.replace(connect.userName, true);
        }
      }
    }
    getSender().tell(response, getSelf());
  }

  private void disConnect(DisConnect disConnect) {
    String response;
    if (adminNames.containsKey(disConnect.userName)) {//validation check
      adminNames.replace(disConnect.userName,false);
      response = disConnect.userName + " has been disconnected successfully!";
    }
    else {
      response = disConnect.userName + " failed to disconnect";
    }
    getSender().tell(response, getSelf());
  }

  private void sendMeasurement(SendMeasurement measurement) {
    String sernsorSerial = measurement.sensorName;
    if (sensors.containsKey(sernsorSerial)){
      sensors.get(sernsorSerial).addMeasurement(this.day,measurement.measurement);
    }
  }

  private String getQuery(GetInfo getInfo){
    StringBuilder strBld = new StringBuilder();
    String sensorsQuery = getInfo.infoOf;
    int date = getInfo.date;
    if( date != -1 && Math.abs(date-this.day) >6 ) {
      return "Error - server saves records for only 7 days";
    }
    if (date == -1){ //specific date
      if(sensorsQuery.equals("all")){
        this.sensors.forEach((sensorName,sensorSamples) -> strBld.append(sensorName+":\n").append(sensorSamples.toString()).append("------------------\n"));
      } else {
        strBld.append(this.sensors.get(sensorsQuery).toString());
      }
    } else {
      if(sensorsQuery.equals("all")){
        this.sensors.forEach((sensorName,sensorSamples) -> strBld.append(sensorName).append(":\n").append(sensorSamples.toStringSpecificDay(date)).append("------------------\n"));
      } else {
        strBld.append(this.sensors.get(sensorsQuery).toStringSpecificDay(date));
      }
    }
    return strBld.toString();
  }

  private void getInfo(GetInfo getInfo) {
    String response=getQuery(getInfo);
    getSender().tell(response, getSelf());
  }

  private void getInfoFile(GetInfo getInfo) {
    String res = getQuery(getInfo);
    byte[] response = res.getBytes();
  }
  
}