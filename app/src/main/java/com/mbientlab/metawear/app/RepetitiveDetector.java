package com.mbientlab.metawear.app;

import java.util.ArrayList;

/**
 * Created by Janelle on 03/06/2018.
 */

public class RepetitiveDetector {
    double motionFrequency = 0;
    boolean isPeriodic;
    boolean motionError;
    boolean toofast;
    int trending = 0;

    int entries = 0;

    int numMins = 0;
    int numMaxes = 0;
    double ideal_p2p;
    double upperbound;
    double lowerbound;
    double difference;
    int RepCount = 0;
    ArrayList<Double> pastEntries = new ArrayList<Double>();
    int lastMinIndex = -1;
    int lastMaxIndex = -1;
    int Reptrending = 0;
    double repMinVal = 0;
    double repMaxVal = 0;
    int newMax = 0;
    int newMin = 0;

    public double getfreq(){
        return motionFrequency;
    }

    public boolean isMotionError(){
        return motionError;
    }

    public boolean isToofast(){
        return toofast;
    }

    public int getRepCount() {return RepCount;}

    /*public double upperlimit(){return upperbound;}

    public double lowerlimit(){return lowerbound;}

    public double currPk2Pk(){return difference;}*/

    public double percentThreshold(){
        double percent;
        if(difference <= lowerbound){
            percent = 0.0;
        }
        else if(difference >= upperbound){
            percent = 100;
        }
        else{
            percent = ((upperbound - difference)/(upperbound - lowerbound))*100;
        }
        return percent;
    }

    public boolean isPeriodic(ArrayList<Double> data) {
        ArrayList<Integer> min = new ArrayList<Integer>();
        ArrayList<Integer> max = new ArrayList<Integer>();
//        ArrayList<Double> frequency = new ArrayList<Double>();
        isPeriodic = false;
        motionError = false;
        toofast = false;

        int downsample = 40;
        int p = 0;
        int k = 0;
        entries++;

        if(entries%downsample == 0){
            checkForReps(data.get(0), entries);
        }

        for(int i = 0; i < (data.size()-downsample); i+=40){
            if (trending == 0) { //no known trend yet
                if (data.get(i) < data.get(i + downsample)) //i + downsampled because data(0) more recent
                    trending = -1;
                else
                    trending = 1;
            }
            else if(trending == -1) { //trending downward so looking for a min
                if (data.get(i) > data.get(i + downsample)) {
                    min.add(k, i);
                    if (k != 0) {
                        double min_difference = min.get(k) - min.get(k - 1);
                        if (min_difference > 200 && min_difference < 600) {
                            //isPeriodic true
                            isPeriodic = true;
                            double frequency_val = (1 / (min_difference / 100));
                            motionFrequency = (frequency_val + motionFrequency)/2;
                            motionError = true;
                            if (min_difference > 500)
                                toofast = false;
                            else if (min_difference < 300)
                                toofast = true;
                            else
                                motionError = false;
                        }
                    }
                    trending = 1;
                    k++;
                }
                else if (data.get(i).equals(data.get(i + downsample))){
                    trending = 0;
                    motionFrequency = (0 + motionFrequency)/2;
                }
            }
            else if(trending == 1) {// trending upward so looking for max
                if (data.get(i) < data.get(i + downsample)) {
                    max.add(p, i);
                    if (p != 0) {
                        double max_difference = max.get(p) - max.get(p - 1);
                        if (max_difference > 200 && max_difference < 600) {
                            //isPeriodic true
                            isPeriodic = true;
                            double frequency_val = (1 / (max_difference / 100));
                            motionFrequency = (frequency_val + motionFrequency)/2;
                            motionError = true;
                            if (max_difference > 500)
                                toofast = false;
                            else if (max_difference < 300)
                                toofast = true;
                            else
                                motionError = false;
                        }
                    }
                    trending = -1;
                    p++;
                }
                else if (data.get(i).equals(data.get(i + downsample))){
                    trending = 0;
                    motionFrequency = (0 + motionFrequency)/2;
                }
            }

        }
        return isPeriodic;
    }

    public void checkForReps(double newEntry, int i){
        pastEntries.add(0, newEntry);
        for(int l = pastEntries.size(); l > 1024; l--){
            pastEntries.remove(l);
        }
        if(Reptrending == 0 && pastEntries.size() > 1) { //no known trend yet
            if (pastEntries.get(0) < pastEntries.get(1))
                Reptrending = -1;
            else
                Reptrending = 1;
        }
        else if(Reptrending == -1) { //trending downward so looking for a min
            if (pastEntries.get(0) > pastEntries.get(1)) {
                if (lastMinIndex != -1) {
                    int min_difference = i - lastMinIndex;
                    if (min_difference > 150 && min_difference < 600) {
                        repMinVal = pastEntries.get(1);
                        numMins++;
                        newMin = 1;
                    }
                }
                Reptrending = 1;
                lastMinIndex = i;
            }
        }
        else if(Reptrending == 1) { //trending downward so looking for a min
            if (pastEntries.get(0) < pastEntries.get(1)) {
                if (lastMaxIndex != -1) {
                    int max_difference = i - lastMaxIndex;
                    if (max_difference > 150 && max_difference < 600) {
                        repMaxVal = pastEntries.get(1);
                        numMaxes++;
                        newMax = 1;
                    }
                }
                Reptrending = -1;
                lastMaxIndex = i;
            }
        }
        if(newMax == 1 && newMin == 1){
            if(numMaxes == numMins && numMaxes == 1) {
                difference = (repMaxVal - repMinVal);
                ideal_p2p = difference;
                RepCount = 1;
                newMax = 0;
                newMin = 0;
            }
            else if (numMaxes == numMins && numMaxes != 0) {
                difference = repMaxVal - repMinVal;
                lowerbound = ideal_p2p - (ideal_p2p * 0.2);
                upperbound = (ideal_p2p * 0.2) + ideal_p2p;
                if (numMaxes < 4 && difference < upperbound && difference > lowerbound) {
                    ideal_p2p = (ideal_p2p + difference) / 2;
                    RepCount++;
                } else if (difference < upperbound && difference > lowerbound)
                    RepCount++;
                else {
                    numMaxes = 1;
                    numMins = 1;
                    RepCount = 1;
                    ideal_p2p = difference;
                }
                newMax = 0;
                newMin = 0;
            }
        }
    }
}