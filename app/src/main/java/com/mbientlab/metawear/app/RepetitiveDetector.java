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
    double[] motionFrequencyAll = new double[3];
    boolean[] motionErrorAll = new boolean[3];
    boolean[] toofastAll = new boolean[3];
    boolean[] isPeriodicAll = new boolean[3];
    int[] trending = new int[3];
    double[] averageAmplitudes = new double[3];

    boolean calibrationComplete = false;

    int totalReps = 0;

    int chosenAngle = -1;

    int entries = 0;

    double threshold = 20;

    int[] numMins = new int[3];
    int[] numMaxes = new int[3];
    double ideal_p2p;
    double upperbound;
    double lowerbound;
    double[] difference = new double[3];
    int RepCount = 0;
    ArrayList<Double> pastPitchEntries = new ArrayList<Double>();
    ArrayList<Double> pastRollEntries = new ArrayList<Double>();
    ArrayList<Double> pastYawEntries = new ArrayList<Double>();
    int[] lastMinIndex = {-1,-1,-1};
    int[] lastMaxIndex = {-1,-1,-1};
    int[] lastCountedMin = {-1,-1,-1};
    int[] lastCountedMax = {-1,-1,-1};
    int[] Reptrending = new int[3];
    double[] repMinVal = new double[3];
    double[] repMaxVal = new double[3];
    int[] newMax = new int[3];
    int[] newMin = new int[3];

    int resetCalib = 0;

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

    public int getResetCalib() {return resetCalib;}

    public int getChosenAngle() {return chosenAngle;}

    public boolean getcalibrationComplete() {return  calibrationComplete;}

    public double percentThreshold(){
        double percent;
        if(chosenAngle != -1) {
            if (difference[chosenAngle] <= lowerbound) {
                percent = 0.0;
            } else if (difference[chosenAngle] >= upperbound) {
                percent = 100;
            } else {
                percent = ((upperbound - difference[chosenAngle]) / (upperbound - lowerbound)) * 100;
            }
        }
        else
            percent = 0;
        return percent;
    }

    public boolean isPeriodic(ArrayList<Double> pitch, ArrayList<Double> roll, ArrayList<Double> yaw) {
//        ArrayList<Double> frequency = new ArrayList<Double>();
        isPeriodic = false;
        motionError = false;
        toofast = false;

        entries++;
        resetCalib = 0;
        int downsample = 40;

        if(entries%downsample == 0){
            checkForRepsAllAngles(pitch.get(0), roll.get(0), yaw.get(0), entries);
        }


        isPeriodicCheck(pitch,downsample, 0);
        isPeriodicCheck(roll,downsample, 1);
        isPeriodicCheck(yaw,downsample, 2);

        if(chosenAngle == -1) {
            if ((averageAmplitudes[0] > averageAmplitudes[1]) && (averageAmplitudes[0] > averageAmplitudes[2])) {
                isPeriodic = isPeriodicAll[0];
                motionFrequency = motionFrequencyAll[0];
                motionError = motionErrorAll[0];
                toofast = toofastAll[0];
            } else if (averageAmplitudes[1] > averageAmplitudes[2]) {
                isPeriodic = isPeriodicAll[1];
                motionFrequency = motionFrequencyAll[1];
                motionError = motionErrorAll[1];
                toofast = toofastAll[1];
            } else {
                isPeriodic = isPeriodicAll[2];
                motionFrequency = motionFrequencyAll[2];
                motionError = motionErrorAll[2];
                toofast = toofastAll[2];
            }
        }
        else{
            isPeriodic = isPeriodicAll[chosenAngle];
            motionFrequency = motionFrequencyAll[chosenAngle];
            motionError = motionErrorAll[chosenAngle];
            toofast = toofastAll[chosenAngle];
        }

        return isPeriodic;
    }

    public void isPeriodicCheck(ArrayList<Double> data, int downsample, int angle){
        int p = 0;
        int k = 0;
        ArrayList<Integer> min = new ArrayList<Integer>();
        ArrayList<Integer> max = new ArrayList<Integer>();
        ArrayList<Double> minVal = new ArrayList<Double>();
        ArrayList<Double> maxVal = new ArrayList<Double>();
        isPeriodicAll[angle] = false;
        motionFrequencyAll[angle] = 0.0;
        trending[angle] = 0;
        averageAmplitudes[angle] = 0;

        for(int i = 0; i < (data.size()-downsample); i+=40){
            if (trending[angle] == 0) { //no known trend yet
                if (data.get(i) < data.get(i + downsample)) //i + downsampled because data(0) more recent
                    trending[angle] = 1;
                else
                    trending[angle] = -1;
            }
            else if(trending[angle] == -1) { //trending downward so looking for a min
                if (data.get(i) < data.get(i + downsample)) {
                    min.add(k, i);
                    minVal.add(k, data.get(i));
                    if (k != 0) {
                        double min_difference = min.get(k) - min.get(k - 1);
                        if (min_difference > 200 && min_difference < 600) {
                            //isPeriodic true
                            isPeriodicAll[angle] = true;
                            double frequency_val = (1 / (min_difference / 100));
                            motionFrequencyAll[angle] = (frequency_val + motionFrequencyAll[angle])/2;
                            motionErrorAll[angle] = true;
                            if (min_difference > 500)
                                toofastAll[angle] = false;
                            else if (min_difference < 300)
                                toofastAll[angle] = true;
                            else
                                motionErrorAll[angle] = false;
                        }
                    }
                    trending[angle] = 1;
                    k++;
                }
                else if (data.get(i).equals(data.get(i + downsample))){
                    trending[angle] = 0;
                    //isPeriodicAll[angle] = false;
                    //motionFrequency = (0 + motionFrequency)/2;
                }
            }
            else if(trending[angle] == 1) {// trending upward so looking for max
                if (data.get(i) > data.get(i + downsample)) {
                    max.add(p, i);
                    maxVal.add(p, data.get(i));
                    if (p != 0) {
                        double max_difference = max.get(p) - max.get(p - 1);
                        if (max_difference > 200 && max_difference < 600) {
                            //isPeriodic true
                            isPeriodicAll[angle] = true;
                            double frequency_val = (1 / (max_difference / 100));
                            motionFrequencyAll[angle] = (frequency_val + motionFrequencyAll[angle]) / 2;
                            motionErrorAll[angle] = true;
                            if (max_difference > 500)
                                toofastAll[angle] = false;
                            else if (max_difference < 300)
                                toofastAll[angle] = true;
                            else
                                motionErrorAll[angle] = false;
                        }
                    }
                    trending[angle] = -1;
                    p++;
                } else if (data.get(i).equals(data.get(i + downsample))) {
                    trending[angle] = 0;
                    //isPeriodicAll[angle] = false;
                    //motionFrequency =  (0 + motionFrequency)/2;
                }
            }
            if(p == k && k == 1) {
                averageAmplitudes[angle] = (maxVal.get(p-1) - minVal.get(k-1));
            }
            else if (p == k && p != 0) {
                averageAmplitudes[angle] = (averageAmplitudes[angle] + (maxVal.get(p-1) - minVal.get(k-1)))/2;
            }
        }
    }

    public void checkForRepsAllAngles(double newPitch, double newRoll, double newYaw, int i){
        pastPitchEntries.add(0, newPitch);
        pastRollEntries.add(0, newRoll);
        pastYawEntries.add(0, newYaw);
        checkForReps(pastPitchEntries, 0, i);
        checkForReps(pastRollEntries, 1, i);
        checkForReps(pastYawEntries, 2, i);

        /*if(chosenAngle == -1){
            boolean[] ready = new boolean[3];
            for(int x = 0; x < 3; x++){
                if(numMaxes[x] == 1 && numMins[x] == 1) {
                    ready[x] = true;
                    difference[x] = (repMaxVal[x] - repMinVal[x]);
                }
            }
            if(ready[0] && difference[0]>difference[1] && difference[0]>difference[2] && difference[0] > 0.1){
                ideal_p2p = difference[0];
                RepCount = 1;
                totalReps++;
                newMax[0] = 0;
                newMin[0] = 0;
                chosenAngle = 0;
            }
            else if(ready[1] && difference[1] > difference[2] && difference[1] > 0.1){
                ideal_p2p = difference[1];
                RepCount = 1;
                totalReps++;
                newMax[1] = 0;
                newMin[1] = 0;
                chosenAngle = 1;
            }
            else if (ready[2] && difference[2] > 0.1){
                ideal_p2p = difference[2];
                RepCount = 1;
                totalReps++;
                newMax[2] = 0;
                newMin[2] = 0;
                chosenAngle = 2;
            }
        }*/

        if(chosenAngle == -1){
            boolean[] ready = new boolean[3];
            for(int x = 0; x < 3; x++){
                if(numMaxes[x] == 1 && numMins[x] == 1) {
                    ready[x] = true;
                    difference[x] = (repMaxVal[x] - repMinVal[x]);
                }
            }
            if(ready[0] && difference[0] < 0.1 || (i - lastMaxIndex[0]) > 400){
                if(ready[1] && difference[1] > difference[2] && difference[1] > 0.1) {
                    ideal_p2p = difference[1];
                    RepCount = 1;
                    totalReps++;
                    newMax[1] = 0;
                    newMin[1] = 0;
                    chosenAngle = 1;
                }
                else if (ready[2] && difference[2] > 0.1){
                    ideal_p2p = difference[2];
                    RepCount = 1;
                    totalReps++;
                    newMax[2] = 0;
                    newMin[2] = 0;
                    chosenAngle = 2;
                }

            }
            else if(ready[0]){
                    ideal_p2p = difference[0];
                    RepCount = 1;
                    totalReps++;
                    newMax[0] = 0;
                    newMin[0] = 0;
                    chosenAngle = 0;
                }
            }
          /*  if(ready[0] && difference[0]>difference[1] && difference[0]>difference[2] && difference[0] > 0.1){
                ideal_p2p = difference[0];
                RepCount = 1;
                totalReps++;
                newMax[0] = 0;
                newMin[0] = 0;
                chosenAngle = 0;
            }
            else if(ready[1] && difference[1] > difference[2] && difference[1] > 0.1){
                ideal_p2p = difference[1];
                RepCount = 1;
                totalReps++;
                newMax[1] = 0;
                newMin[1] = 0;
                chosenAngle = 1;
            }
            else if (ready[2] && difference[2] > 0.1){
                ideal_p2p = difference[2];
                RepCount = 1;
                totalReps++;
                newMax[2] = 0;
                newMin[2] = 0;
                chosenAngle = 2;
            }
        }*/

        else if(Math.abs(numMaxes[chosenAngle] - numMins[chosenAngle]) > 1){
            boolean[] ready = new boolean[3];
            for(int x = 0; x < 3; x++){
                if(numMaxes[x] == numMins[x]) {
                    ready[x] = true;
                    difference[x] = (repMaxVal[x] - repMinVal[x]);
                }
            }
            if(ready[0] && difference[0]>difference[1] && difference[0]>difference[2] && difference[0] > 0.1){
                ideal_p2p = difference[0];
                RepCount = 1;
                totalReps++;
                newMax[0] = 0;
                newMin[0] = 0;
                chosenAngle = 0;
            }
            else if(ready[1] && difference[1] > difference[2] && difference[1] > 0.1){
                ideal_p2p = difference[1];
                RepCount = 1;
                totalReps++;
                newMax[1] = 0;
                newMin[1] = 0;
                chosenAngle = 1;
            }
            else if (ready[2] && difference[2] > 0.1){
                ideal_p2p = difference[2];
                RepCount = 1;
                totalReps++;
                newMax[2] = 0;
                newMin[2] = 0;
                chosenAngle = 2;
            }
            resetCalib = 1;
        }
        else {
            if (newMax[chosenAngle] == 1 && newMin[chosenAngle] == 1) {
                if(numMaxes[chosenAngle] == numMins[chosenAngle] && numMaxes[chosenAngle] < 4){ //still in calibration
                    boolean[] ready = new boolean[3];
                    for(int x = 0; x < 3; x++){
                        if(numMaxes[x] == numMins[x] && numMins[x] >= 1) {
                            ready[x] = true;
                            difference[x] = (repMaxVal[x] - repMinVal[x]);
                        }
                    }
                    lowerbound = ideal_p2p - (ideal_p2p * (threshold*0.01));
                    upperbound = (ideal_p2p * (threshold*0.01)) + ideal_p2p;
                    if(difference[chosenAngle] >= upperbound || difference[chosenAngle] <= lowerbound){ //out of calibration bounds
                        if(ready[0] && difference[0]>difference[1] && difference[0]>difference[2]){
                            ideal_p2p = difference[0];
                            RepCount = 1;
                            totalReps++;
                            newMax[0] = 0;
                            newMin[0] = 0;
                            numMaxes[0] = 1;
                            numMins[0] = 1;
                            chosenAngle = 0;
                        }
                        else if(ready[1] && difference[1] > difference[2]){
                            ideal_p2p = difference[1];
                            RepCount = 1;
                            totalReps++;
                            newMax[1] = 0;
                            newMin[1] = 0;
                            numMaxes[1] = 1;
                            numMins[1] = 1;
                            chosenAngle = 1;
                        }
                        else if (ready[2]){
                            ideal_p2p = difference[2];
                            RepCount = 1;
                            totalReps++;
                            newMax[2] = 0;
                            newMin[2] = 0;
                            numMaxes[2] = 1;
                            numMins[2] = 1;
                            chosenAngle = 2;
                        }
                        resetCalib = 1;
                    }
                    else{ //fits in calibration
                        ideal_p2p = (ideal_p2p + difference[chosenAngle]) / 2;
                        RepCount++;
                        totalReps++;
                        newMax[chosenAngle] = 0;
                        newMin[chosenAngle] = 0;
                        if(RepCount == 3){
                            calibrationComplete = true;
                        }
                    }
                }
                else if (numMaxes[chosenAngle] == numMins[chosenAngle] && numMaxes[chosenAngle] != 0) {
                    difference[chosenAngle] = repMaxVal[chosenAngle] - repMinVal[chosenAngle];
                    lowerbound = ideal_p2p - (ideal_p2p * (threshold*0.01));
                    upperbound = (ideal_p2p * (threshold*0.01)) + ideal_p2p;
                    if (numMaxes[chosenAngle] > 4 && difference[chosenAngle] < upperbound && difference[chosenAngle] > lowerbound) {
                        ideal_p2p = (ideal_p2p + difference[chosenAngle]) / 2;
                        RepCount++;
                        totalReps++;
                    } else if (difference[chosenAngle] < upperbound && difference[chosenAngle] > lowerbound) {
                        RepCount++;
                        totalReps++;
                    } else {
                        System.out.println("Repetition out of bounds");
                    }
                    newMax[chosenAngle] = 0;
                    newMin[chosenAngle] = 0;
                }
            }
        }
    }

    public void checkForReps(ArrayList<Double> data, int angle, int i){
        for(int l = data.size(); l > 1024; l--){
            data.remove(l);
        }
        if(Reptrending[angle] == 0 && data.size() > 1) { //no known trend yet
            if (data.get(0) < data.get(1))
                Reptrending[angle] = -1;
            else
                Reptrending[angle] = 1;
        }
        else if(Reptrending[angle] == -1) { //trending downward so looking for a min
            if (data.get(0) > data.get(1)) {
                if (lastMinIndex[angle] != -1) {
                    int min_difference = i - lastMinIndex[angle];
                    if (min_difference > 150 && min_difference < 600) {
                        repMinVal[angle] = data.get(1);
                        numMins[angle]++;
                        newMin[angle] = 1;
                        lastCountedMin[angle] = i;
                    }
                }
                Reptrending[angle] = 1;
                lastMinIndex[angle] = i;
            }
        }
        else if(Reptrending[angle] == 1) { //trending downward so looking for a min
            if (data.get(0) < data.get(1)) {
                if (lastMaxIndex[angle] != -1) {
                    int max_difference = i - lastMaxIndex[angle];
                    if (max_difference > 150 && max_difference < 600) {
                        repMaxVal[angle] = data.get(1);
                        numMaxes[angle]++;
                        newMax[angle] = 1;
                        lastCountedMax[angle] = i;
                    }
                }
                Reptrending[angle] = -1;
                lastMaxIndex[angle] = i;
            }
        }
    }
}