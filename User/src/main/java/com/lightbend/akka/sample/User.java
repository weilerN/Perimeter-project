package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import com.lightbend.akka.sample.Messages.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class User extends AbstractActor {
  private String userName;
  private final ActorSelection server;


  static public Props props(String userName) {
    return Props.create(User.class, () -> new User(userName));
  }

  public User(String userName) {
    this.userName = userName;
    this.server = getContext().actorSelection("akka://System@"+UserMain.server_address+"/user/Server");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(GetInfo.class, this::getInfo)
        .match(DisConnect.class, this::disConnect)
        .match(String.class, this::stringPrinter)
        .build();
  }

  private void stringPrinter(String message){
    System.out.println(message);
  }

  private void disConnect(DisConnect disConnect) {
        Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);
        Future<Object> answer = Patterns.ask(server, disConnect, timeout);
        try{
          String result = (String) Await.result(answer, timeout.duration());
          if((disConnect.userName.equals(this.userName)) && result.equals(disConnect.userName + " has been disconnected successfully!")){
            System.out.println(result);
            getContext().stop(getSelf()); //shut down this actorRef
          }
          else{
            System.out.println(disConnect.userName + " failed to disconnect");
          }
        }
        catch (Exception e){
          System.out.println("server is offline! try again later!");
        }
  }

  private void getInfo(GetInfo getInfo) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, getInfo, timeout);
    try{
      String result = (String) Await.result(answer, timeout.duration());
      Date date = new Date();
      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

      if (getInfo instanceof GetInfoFile) {
        this.receiveFileMessage(formatter.format(date), (GetInfoFile)getInfo);
      }
      else{
        System.out.printf("[%s] query result:\n %s\n", formatter.format(date), result);
      }
    }
    catch (Exception e){
      System.out.println("server is offline! try again later!");
    }
  }

  private void receiveFileMessage(String date, GetInfoFile getInfo) {
    try {
      new File("files").mkdirs();
      File file = new File("files/newFile");
      OutputStream os = new FileOutputStream(file);
      os.write(getInfo.file);
      System.out.printf("[%s] File received: \n", date);
      os.close();
    }
    catch (Exception e){
      System.out.println("failed to convert file: "+ e.getMessage());
    }
  }

}
