package com.mbientlab.metawear.app;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Janelle on 3/2/18.
 */

public class Filtration {

    public static ArrayList<Double> Filter(ArrayList<Double> data, int capacity, String angle){
        int pitch_flip = 0;
        int roll_flip = 0;
        int yaw_flip = 0;
        double pitchB[] = {0.0115586129393857, 0.0269127184226144, 0.0689577752425094, 0.124811024018249,
                0.172306936575946,0.190905865602590,0.172306936575946,0.124811024018249,0.0689577752425094,
                0.0269127184226144,0.0115586129393857};
        double rollB[] = {0.0115586129393857, 0.0269127184226144, 0.0689577752425094, 0.124811024018249,
                0.172306936575946,0.190905865602590,0.172306936575946,0.124811024018249,0.0689577752425094,
                0.0269127184226144,0.0115586129393857};
        double yawB[] = {0.0115586129393857, 0.0269127184226144, 0.0689577752425094, 0.124811024018249,
                0.172306936575946,0.190905865602590,0.172306936575946,0.124811024018249,0.0689577752425094,
                0.0269127184226144,0.0115586129393857};
        double filtered_data[] = new double[capacity * 5]; //DOWNSAMPLE1: remove *5
        double filtered_data_down[] = new double[capacity]; //DOWNSAMPLE1: delete this variable
        double data_down[] = new double[capacity]; //DOWNSAMPLE1: delete this variable
        //double filtered_data[] = new double[capacity];
        //double filtered_data_down[] = new double[(capacity/5)+1]; //DOWNSAMPLE2: delete this variable

        if (angle == "pitch") {
            pitch_flip = PitchFlipCheck(data, pitch_flip);
            if (pitch_flip == 1) {
                data.add(0, data.get(0) + 360);
            }
            if (pitch_flip == 2) {
                data.add(0, (360 - data.get(0)) * -1);
            }
            //filtered_data = Convolution(pitchB, data);
        }

        else if (angle == "roll") {
            roll_flip = FlipCheck(data, roll_flip);
            if (roll_flip == 1) {
                data.add(0,data.get(0) + 360);
            }
            if (roll_flip == 2) {
                data.add(0,(360 - data.get(0)) * -1);
            }
            //filtered_data = Convolution(rollB, data);
        }

        else if (angle == "yaw") {
            yaw_flip = FlipCheck(data, yaw_flip);
            if (yaw_flip == 1) {
                data.add(0,data.get(0) + 360);
            }
            if (yaw_flip == 2) {
                data.add(0,(360 - data.get(0)) * -1);
            }
            //filtered_data = Convolution(yawB, data);
        }

        /*for (int i = 0; i < (capacity*5); i++){ //DOWNSAMPLE1: remove entire for loop DOWNSAMPLE2: remove entire for loop
            //for (int i = 0; i < (capacity); i++){ //DOWNSAMPLE2: remove entire for loop
            if((i%5) == 0){
                if (data[i] != 0) //DOWNSAMPLE1: delete this line
                    data_down[i/5] = data[i]; //DOWNSAMPLE1: delete this line
                if (filtered_data[i] != 0)
                    filtered_data_down[i/5] = filtered_data[i];
            }
        }*/

        ArrayList<Double> result = new ArrayList<Double>(data);
        //double[][] result = new double[][]{data_down, filtered_data_down}; //DOWNSAMPLE1: return data, filtered_data
        //double[][] result = new double[][]{data, filtered_data_down}; //DOWNSAMPLE2: return data, filtered_data

        return result;

    }


    public static int PitchFlipCheck(ArrayList<Double> data, int alreadyFlipped) {
        int i = 0; //this is used as an index to get the previous data point and current
        int flip; //this returned, 0 is false, 1 is true
        double lastVal = data.get(i + 1);
        double currVal = data.get(i);

        //if the data is already flipped we are checking for the point when the current angle is
        //less than 360 and the last was over 360
        if (alreadyFlipped == 1) {  //alreadyFlipped is passed in
            if ((lastVal > 100) && (currVal < 0))
                flip = 1;
            else
                flip = 0; //otherwise we return true
        }

        else if (alreadyFlipped == 2) {
            if ((lastVal < 0) && (currVal > 100)){
                flip = 2;
            }
            else
                flip = 0;
        }
        //if the last point was not flipped then we are checking if the last point was over 300
        //and the current is under 50--> I can show all this logic graphically if it's confusing
        else {
            if ((lastVal > 100) && (currVal < 0)) {
                flip = 1;
            }

            else if ((lastVal < 0) && (currVal > 100)){
                flip = 2;
            }

            else
                flip = 0;
        }

        return flip; //basically returning a true or false on whether the data is still flipped
    }

    public static int FlipCheck(ArrayList<Double> data, int alreadyFlipped) {
        int i = 0; //this is used as an index to get the previous data point and current
        int flip; //this returned, 0 is false, 1 is true
        double lastVal = data.get(i + 1);
        double currVal = data.get(i);

        //if the data is already flipped we are checking for the point when the current angle is
        //less than 360 and the last was over 360
        if (alreadyFlipped == 1) {  //alreadyFlipped is passed in
            if ((lastVal > 360) && (currVal > 300))
                flip = 0; //if it is no longer flipped, we return false
            else
                flip = 1; //otherwise we return true
        }

        else if (alreadyFlipped == 2) {
            if ((lastVal < 0) && (currVal < 100)){
                flip = 0;
            }
            else
                flip = 2;
        }
        //if the last point was not flipped then we are checking if the last point was over 300
        //and the current is under 50--> I can show all this logic graphically if it's confusing
        else {
            if ((lastVal > 300) && (currVal < 50)) {
                flip = 1;
            }

            else if ((lastVal < 50) && (currVal > 300)){
                flip = 2;
            }

            else
                flip = 0;
        }

        return flip; //basically returning a true or false on whether the data is still flipped
    }


    /*This function will return the data array after the FIR filter has been applied*/
    private static double[] Convolution(double[] b, double[] data) {
        int sizeofb = b.length; //the size of b
        int sizeofdata = data.length; //the number of data points we are storing
        int numrows = (sizeofdata + sizeofb) - 1; //the number of rows depends on the number of delays
        double[][] multi = new double[numrows][sizeofb]; //a 2-d matrix to store a matrix with 0s for convolution
        double[] y = new double[sizeofdata];
        int r = 0; //used to index rows
        int c = 0; //used to index columns


        for (r = 0; r < sizeofdata; r++) { //add zeros before the first data point and shift along
            for (c = 0; ((c <= r) && (c < sizeofb)); c++) {
                multi[r][c] = data[r - c];
            }
        }

        for (r = (sizeofdata - 1); r < numrows; r++) { //add zeros once the last data point has moved through
            for (c = 0; c < sizeofb; c++) {
                if ((r - c) < sizeofdata) {
                    multi[r][c] = data[r - c];
                }
            }
        }

        for (r = 0; r < numrows; r++) { //multiply each row by b and sum
            double sum = 0;
            if (r < sizeofdata) {
                for (c = 0; c < sizeofb; c++) {
                    sum = (multi[r][c] * b[c]) + sum;
                }
                y[r] = sum;
            }
        }

        return y;

    }


}


