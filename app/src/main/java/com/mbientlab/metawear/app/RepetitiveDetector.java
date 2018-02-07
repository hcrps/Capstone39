package com.mbientlab.metawear.app;

/**
 * Created by Andrew Hollister on 2/6/2018.
 */

public class RepetitiveDetector {

    public static double getDiff(double perf, double ex){
        double diff = perf - ex;
        return diff;
    }

    //Returns a 1x2 array that has the Maximum value in index 0 and the Index of that value in index 1
    public static double[] getGoldMax(double[] a){
        double[] max=new double[2];
        max[0]=a[0];
        for(int i=0;i<a.length;i++){
            if(a[i]>max[0]){max[0]=a[i]; max[1]=i;}
        }
        return max;
    }

    //Returns a 1x2 array that has the Minimum value in index 0 and the Index of that value in index 1
    public static double[] getGoldMin(double[] b){
        double[] min = new double[2];
        min[0]=b[0];
        for(int i=0;i<b.length;i++){
            if(b[i]<min[0]){min[0]=b[i]; min[1]=i;}
        }
        return min;
    }

    //Extends an array a by the length of array exp--used to extend the perfect rep to each recorded rep in exp data
    //Might not be used !
    public static double[][] extendArr(double[] a, double[][] exp){
        double[][] arr = new double[2][exp[0].length];
        for(int i = 0; i < exp[0].length; i++){
            arr[0][i]=a[0];
            arr[1][i]=(i+1)*a[1];
        }
        return arr;
    }
//The following array bound logic is currently untested
    public static double[][] getRealMax(double [] c, int len){
        double[][] maxArray = new double[2][len];
        for(int i = 0; i< len-4; i++){
            int k = 0;
            for(int j = -3; j<3; j++) {
                double[] slope = new double[7];
                if (i <= 2) {
                    j = -i;
                }
                else {
                    slope[k] = c[i + j + 1] - c[i + j];
                    k++;
                }
                double back = (slope[0] + slope[1] + slope[2] + slope[3]) / 4;
                double front = (slope[4] + slope[5] + slope[6]) / 3;
                if(front < 0 && back > 0){
                    maxArray[0][i] = c[i];
                    maxArray[1][i] = i;
                }
            }
        }
        return maxArray;
    }

    public static double[][] getRealMin(double [] c, int len){
        double[][] minArray = new double[2][len];
        for(int i = 0; i< len-4; i++){
            int k = 0;
            for(int j = -3; j<3; j++) {
                double[] slope = new double[7];
                if (i <= 2) {
                    j = -i;
                }
                else {
                    slope[k] = c[i + j + 1] - c[i + j];
                    k++;
                }
                double back = (slope[0] + slope[1] + slope[2] + slope[3]) / 4;
                double front = (slope[4] + slope[5] + slope[6]) / 3;
                if(front > 0 && back < 0){
                    minArray[0][i] = c[i];
                    minArray[1][i] = i;
                }
            }
        }
        return minArray;
    }

    public double[][] characterizeReal(double[] arr, boolean flag) {
        int len = arr.length;
        double[][] max = new double[2][len];
        double[][] min = new double[2][len];
        for (int i = 2; i < len-2; ++i) {
            double leading = 0;
            double following = 0;
            leading = arr[i+1] - arr[i];
            following = arr[i] - arr[i-1];

            if(leading<0 && following>0){
                double leading2 = arr[i+2] - arr[i+1];
                if(leading2<0){

                    max[0][i]=arr[i];
                    max[1][i]=i;
                }
            }
            else if(following<0 && leading>0){
                double following2 = arr[i-1] - arr[i-2];
                if(following2<0){
                    min[0][i]=arr[i];
                    min[1][i]=i;
                }
            }
        }
        if(flag){
            return min;
        }
        else{
            return max;
        }
    }
}
