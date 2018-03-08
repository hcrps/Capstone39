package com.mbientlab.metawear.app;

import java.util.ArrayList;

public class RepetitiveDetector {
    double motionFrequency = 0;
    boolean isPeriodic;
    boolean motionError;
    boolean toofast;
    int trending = 0;

    public double getfreq(){
        return motionFrequency;
    }

    public boolean isMotionError(){
        return motionError;
    }

    public boolean isToofast(){
        return toofast;
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
}