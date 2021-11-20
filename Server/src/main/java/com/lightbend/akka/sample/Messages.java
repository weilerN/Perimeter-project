package com.lightbend.akka.sample;

import akka.actor.ActorRef;

public class Messages {

    // -------------- General
    static public class Connect implements java.io.Serializable{
        public final String userName;
        public final String password;
        public final String address;

        public Connect(String userName, String address, String password) {
            this.address = address;
            this.userName = userName;
            this.password = password;
        }
    }

    static public class DisConnect implements java.io.Serializable{
        public final String userName;

        public DisConnect(String userName) {
            this.userName = userName;
        }
    }

    // -------------- sensor
    static public class SendMeasurement implements java.io.Serializable{
        public final String sensorName;
        public final int measurement;

        public SendMeasurement(String name, int m) {
            this.sensorName = name;
            this.measurement = m;
        }
    }
    
    // -------------- User
    static public class GetInfo implements java.io.Serializable{
        public final String infoOf;
        public final Integer date;

        public GetInfo(String infoOf, Integer d) {
            this.infoOf = infoOf;
            this.date=d;
        }
    }

    static public class GetInfoFile extends GetInfo implements java.io.Serializable{
        public final byte[] file;

        public GetInfoFile(String infoOf, Integer d, byte[] file) {
            super(infoOf,d);
            this.file = file;
        }
    }

}