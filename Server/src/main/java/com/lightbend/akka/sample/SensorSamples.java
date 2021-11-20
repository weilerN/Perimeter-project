package com.lightbend.akka.sample;
import java.util.*;

public class SensorSamples {
    private static int[][] db; // 0-min , 1-max, 2-sum, 3-count, 4-ver
    private static final int MIN=0;
    private static final int MAX=1;
    private static final int SUM=2;
    private static final int COUNT=3;
    private static final int VER=4;
    private static final int MAX_VALUE = Integer.MAX_VALUE;
    private static final int MIN_VALUE = Integer.MIN_VALUE;

    public SensorSamples(int day) {
        this.db = new int[7][5];
    }

    private int getIndex(int day){
        return day%7;
    }

    public void resetDay(int day){
        int index=getIndex(day);

        this.db[index][MIN]=MAX_VALUE;
        this.db[index][MAX]=MIN_VALUE;
        this.db[index][SUM]=0;
        this.db[index][COUNT]=0;
        this.db[index][VER]=day;

    }

    public void addMeasurement(int day,int measurement){
        int index= getIndex(day);
        //if days are different -> reset day before adding
        if(this.db[index][VER]!=day)
            this.resetDay(day);

        this.db[index][MIN] = Math.min(this.db[index][MIN], measurement);
        this.db[index][MAX] = Math.max(this.db[index][MAX], measurement);
        this.db[index][SUM]+= measurement;
        this.db[index][COUNT]++;
        this.db[index][VER]=day;
    }

    public String toString(){
        StringBuilder output = new StringBuilder("| min | max | avg |\n------------------\n");
        for(int i=0; i<7; i++) {
            int avg =  db[i][COUNT]>0 ? db[i][SUM] / db[i][COUNT] : 0;
            output.append("|  ").append(db[i][MIN]).append("  |  ").append(db[i][MAX]).append("  |  ").append(avg).append("  |\n");
        }
        return output.toString();
    }

    public String toStringSpecificDay(int day){
        int index= getIndex(day);
        int avg =  db[index][COUNT]>0 ? db[index][SUM] / db[index][COUNT] : 0;
        String output = "| min | max | avg |\n------------------\n"+"|  "+db[index][MIN]+"  |  "+db[index][MAX]+"  |  "+avg+"  |\n";
        return output;
    }

}