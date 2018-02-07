package com.mbientlab.metawear.app;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

/**
 * Created by Person on 2018-01-27.
 */

public class InitialPeriodicMotionDetector extends PatientFragment {

    static double Fs = 100;
    static int arraySize = 1024;

    public static void main(String args[]) {

        //public float[] pitch_data is from PatientFragment --> will ultimately use this data instead of dataPhi
        double[] dataPhi = {};
        double[] dataPhiReal = new double[arraySize];
        Complex[] dataPhiComplex = new Complex[arraySize];
        Complex[] dataPhiFrequency = new Complex[arraySize];
        double[] dataPhiFrequencySpectrum = new double[arraySize];

        
        double[] dataTheta = {};
        double[] dataThetaReal = new double[arraySize];
        Complex[] dataThetaComplex = new Complex[arraySize];
        Complex[] dataThetaFrequency = new Complex[arraySize];
        double[] dataThetaFrequencySpectrum = new double[arraySize];

        double[] dataPsi = {};
        double[] dataPsiReal = new double[arraySize];
        Complex[] dataPsiComplex = new Complex[arraySize];
        Complex[] dataPsiFrequency = new Complex[arraySize];
        double[] dataPsiFrequencySpectrum = new double[arraySize];

        //Analysis for Phi
        dataPhiComplex = convertToComplex(dataPhi, dataPhiComplex);
        dataPhiFrequency = fft(dataPhiComplex);
        changeToFrequencySpectrum(dataPhiFrequency, dataPhiFrequencySpectrum);
        double phiPeak = findPeak(dataPhiFrequencySpectrum);

        //Analysis for Theta
        dataThetaComplex = convertToComplex(dataTheta, dataThetaComplex);
        dataThetaFrequency = fft(dataThetaComplex);
        changeToFrequencySpectrum(dataThetaFrequency, dataThetaFrequencySpectrum);
        double thetaPeak = findPeak(dataThetaFrequencySpectrum);

        //Analysis for Psi
        dataPsiComplex = convertToComplex(dataPsi, dataPsiComplex);
        dataPsiFrequency = fft(dataPsiComplex);
        changeToFrequencySpectrum(dataPsiFrequency, dataPsiFrequencySpectrum);
        double psiPeak = findPeak(dataPsiFrequencySpectrum);

        //Determine the significant angle
        double[] movementDirection = (phiPeak > thetaPeak && phiPeak > psiPeak) ? dataPhiFrequencySpectrum: (thetaPeak > phiPeak && thetaPeak > psiPeak) ? dataThetaFrequencySpectrum: dataPsiFrequencySpectrum;

        //find the relative time domain peak in that data set
        //Will use function from RepetitiveMotionPeriodicDetector

        double[] firstPeriodicMovement;
    }


    public static Complex[] convertToComplex(double[] data, Complex[] dataComplex){
        //Convert polar data into complex numbers
        //Using radius to be 1
        int index = 0;

        for (double val : data) {
            dataComplex[index] = new Complex();
            dataComplex[index].setRe(Math.cos(Math.toRadians(val)));
            dataComplex[index].setIm(Math.sin(Math.toRadians(val)));
            index++;
        }
        return dataComplex;
    }

    //Need to update to remove threshold
    public static double findPeak(double[] dataFrequencySpectrum){
        //Frequencies are in Hertz
        double peakVal = 0;
        int countAboveThreshold = 0;
        double threshold = 30.0;

        for(int i = 0; i < arraySize; i++){
            if(dataFrequencySpectrum[i] > threshold){
                countAboveThreshold++;
                if(peakVal < dataFrequencySpectrum[i]){
                    peakVal = dataFrequencySpectrum[i];
                }
            }
        }
        System.out.println("Maximum frequency power is: " + peakVal + " and number of values above power frequency of 30 are: " + countAboveThreshold);
        return peakVal;
    }

    public static double[] changeToFrequencySpectrum(Complex[] dataFrequency, double[] dataFrequencySpectrum){
        //convert into frequency power spectrum
        double factor = Fs/arraySize; //sampling f of 100Hz, divided by fft length of arraysize - 256
        for(int i = 0; i < arraySize; i++){
            dataFrequencySpectrum[i] = java.lang.Math.sqrt(java.lang.Math.pow(dataFrequency[i].re(),2) + java.lang.Math.pow(dataFrequency[i].im(),2));
            System.out.println("Current frequency power is: " + dataFrequencySpectrum[i] + " n: " + (i) + " and frequency is: " + factor*i + "Hz");
        }
        return dataFrequencySpectrum;
    }

    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        // base case
        if (n == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw new IllegalArgumentException("n is not a power of 2");
        }

        // fft of even terms
        Complex[] even = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd  = even;  // reuse the array
        for (int k = 0; k < n/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

}
