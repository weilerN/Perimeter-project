package com.lightbend.akka.sample;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class UserMain {

  public static String user_host;
  public static String user_port;
  public static String server_address;
  private static ActorRef user;
  private static ActorSelection server;
  private static ActorSystem system;
  private static String userName;
  private static boolean exit = false;
  public static boolean groupInviteFlag = false;
  public static String groupInviteName;
  public static final String illegalCmd="Error- illegal command. run:\n/user show <text|file> <sensor-name|all> \nnote: optional to add: day <day> in the end of command";

  //connect and set akka system
  private static void systemConnect(){
    int count = 0;
    handleConfig();
    Config conf = ConfigFactory.load("application.conf");
    conf.resolve();
    while(true) {
      try {
        system = ActorSystem.create("System", conf);
        user_port = Integer.toString((Integer)system.provider().getDefaultAddress().port().get());
        break;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        handleConfig();
        conf = ConfigFactory.load("application.conf");
        if (++count == 5)
          throw e;
      }
    }
  }

  //main func. connect to akka system, then parse user imput commnads
  public static void main(String[] args) {
    if(args.length < 2){
      System.out.println("MISSING ARGS!");
      return;
    }
    int option = 0;
    Scanner scanner = new Scanner(System.in);
    user_host = args[0];
    server_address = args[1];
    String[] command;
    String SensorCommand = "";
    String userNameConnect,password;
    try {
      System.out.println("Connecting to sever, please wait..");
      systemConnect();
      server = system.actorSelection("akka://System@"+server_address+"/user/Server");
      System.out.println("Connected to server!"+"\n"+"server address: " + server_address +"\n" + "User address: " + user_host+":"+ user_port+"\n");

      while (!exit) {
        if(userName == null) {
          while( option!=1 && option!=2 ){
            System.out.println("You are not connected, please selcect option to login:\n(1) sensor\n(2) User");
            option = Integer.parseInt(scanner.nextLine());
          }
          if( option==1 )
            connectToServer(new Connect("sensor", user_host+":"+user_port, "aaa"));
          else {
            System.out.println("You are not connected, please log in \nEnter username");
            userNameConnect = scanner.nextLine();
            System.out.println("Enter password");
            password = scanner.nextLine();
            connectToServer(new Connect(userNameConnect, user_host + ":" + user_port, password));
          }
          continue;
        }
        //sensor - send measure until got stop as input
        if(userName.contains("Sensor-")){
          while(SensorCommand == "" || !SensorCommand.equals("stop")){
            System.out.println("Enter stop and then ENTER in order to stop cycle of sending measures");
            for( int i=5; i<20; i+=5){
              System.out.println("Sending meaurement:"+i);
              sendMeasurement(i);
            }
            SensorCommand = scanner.nextLine();
          }
        }
        System.out.println("commands:\n/user show <text|file> <sensor-name|all> [day <NUMBER>]");
        command = scanner.nextLine().split(" ");
        if((userName == null) && (!command[0].equals("/user")) && (!command[1].equals("connect"))){
          System.out.println("NEED TO CONNECT FIRST!");
          continue;
        }
        try {
          switch (command[0]) {
            case "/user":
              userCommand(command);
              break;
            case "exit":
              exit = true;
              break;
          }
        }
        catch(ArrayIndexOutOfBoundsException e){
          System.out.println(illegalCmd);
        }
      }
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
    finally {
      system.terminate();
    }
  }

  //parse user commands
  private static void userCommand(String[] command){
    System.out.println("command.length:"+command.length);
    Integer day = (command.length>5 && command[4].equals("day") ) ? Integer.parseInt(command[5]) : -1;
    switch (command[1]){
      case "disconnect":
        UserMain.user.tell(new DisConnect(userName), ActorRef.noSender());
        userName=null;
        break;
      case "show":
        switch (command[2]) {
          case "text":
            UserMain.user.tell(new GetInfo(command[3],day), ActorRef.noSender());
            break;
          case "file":
            UserMain.user.tell(new GetInfoFile(command[3], day, fileToBytes(System.getProperty("user.dir"))), ActorRef.noSender());
            break;
          default:
            System.out.println(illegalCmd);
            break;
        }
        break;
      default:
        System.out.println(illegalCmd);
        break;
    }
  }

  private static byte[] fileToBytes(String path){
    try {
      File file = new File(path);
      byte[] bytesArray = new byte[(int) file.length()];
      FileInputStream fis = new FileInputStream(file);
      fis.read(bytesArray); //read file into bytes[]
      fis.close();
      return bytesArray;
    }
    catch (Exception e){
      System.out.println(path + " does not exist!");
      return null;
    }
  }

  //connect to server and get the user actor ref
  private static void connectToServer(Connect connect){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    try{
      Future<Object> answer = Patterns.ask(server, connect, timeout);
      String result = (String) Await.result(answer, timeout.duration());
      if(result.equals(connect.userName + " is not available") || result.equals("Wrong password!") ||
              result.equals(connect.userName + " is not legal admin name")){
        System.out.println(result);
      }
      else{
        if (result.contains("Sensor-")){
          String str = result.split(" ")[0];
          UserMain.user = UserMain.system.actorOf(User.props(str),str);
          UserMain.userName = str;
        } else {
          UserMain.user = UserMain.system.actorOf(User.props(connect.userName),connect.userName);
          UserMain.userName = connect.userName;
        }
        System.out.println(result);
      }
    }
    catch (Exception e){
      System.out.println("server is offline!");
    }
  }

  //send the server measurement
  private static void sendMeasurement( int measurement){
    try{
      server.tell(new SendMeasurement(userName, measurement),ActorRef.noSender());
    }
    catch (Exception e){
      System.out.println("Error in sendMeasurement:"+e);
    }
  }

  //configure the akka system with args sent via user
  private static void handleConfig(){
    try {
      String configPath = "src/main/resources/application.conf";
      String config = "akka {\r\n" +
              "loglevel = \"OFF\"\r\n" +
              "  actor {\r\n" +
              "  serialize-messages = on\r\n" +
              "  serializers {\r\n" +
              "        java = \"akka.serialization.JavaSerializer\"\r\n" +
              "  }\r\n" +
              "    provider = \"akka.remote.RemoteActorRefProvider\"\r\n" +
              "    warn-about-java-serializer-usage = false\r\n" +
              "  }\r\n" +
              "  remote {\r\n" +
              "    artery{\r\n" +
              "        enabled = on\r\n" +
              "        transport = tcp\r\n" +
              "        canonical.hostname = \"" + user_host + "\"\r\n" +
              "        canonical.port = 0\r\n" +
              "    }\r\n" +
              "    enabled-transports = [\"akka.remote.netty.tcp\"]\r\n" +
              "    netty.tcp {\r\n" +
              "      hostname = \"" + user_host + "\"\r\n" +
              "      port = 0\r\n" +
              "    }\r\n" +
              "  }\r\n" +
              "}\r\n";
        Path path = Paths.get(configPath);
        BufferedWriter writer = Files.newBufferedWriter(path);
        writer.write(config);
        writer.flush();
        writer.close();
    }
    catch (Exception e){
      System.out.println(e.getMessage());
    }
  }
  
}
