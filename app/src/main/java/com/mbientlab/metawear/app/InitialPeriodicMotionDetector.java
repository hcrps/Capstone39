package com.mbientlab.metawear.app;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

/**
 * Created by Person on 2018-01-27.
 */

public class InitialPeriodicMotionDetector {

    private int Fs;
    private int arraySize;
    public float significantPeak;
    private Complex[] dataPhiComplex;
    private Complex[] dataPhiFrequency;
    private float[] dataPhiFrequencySpectrum;

    private Complex[] dataThetaComplex;
    private Complex[] dataThetaFrequency;
    private float[] dataThetaFrequencySpectrum;

    private Complex[] dataPsiComplex;
    private Complex[] dataPsiFrequency;
    private float[] dataPsiFrequencySpectrum;
    public float[] firstPeriodicMovement;

    //enum Angle {PHI, THETA, PSI};
    //Angle currAngle = PHI;
    //instead reference phi =1, theta = 2, and psi = 3

    public InitialPeriodicMotionDetector(){}

    public void finalize() {
        System.out.println("Detector is being destroyed");
    }

    public InitialPeriodicMotionDetector(int Fs, int arraySize){
        this.Fs = Fs;
        this.arraySize = arraySize;

        this.dataPhiComplex = new Complex[arraySize];
        this.dataPhiFrequencySpectrum = new float[arraySize];

        this.dataThetaComplex = new Complex[arraySize];
        dataThetaFrequencySpectrum = new float[arraySize];

        this.dataPsiComplex = new Complex[arraySize];
        this.dataPsiFrequencySpectrum = new float[arraySize];

    }

    public float[] getFreqPitch(){
        return dataPhiFrequencySpectrum;
    }
    public float[] getFreqRoll(){
        return dataThetaFrequencySpectrum;
    }
    public float[] getFreqYaw(){
        return dataPsiFrequencySpectrum;
    }

    public int getFs(){
        return Fs;
    }

    public int getArraySize (){
        return arraySize;
    }

    public float getPeak (){
        return significantPeak;
    }

    public boolean isPeriodic(float[] dataPhi, float[] dataTheta, float[] dataPsi) {

        //Analysis for Phi
        //this.currAngle = PHI;
        convertToComplex(dataPhi, 1);
        this.dataPhiFrequency = fft(dataPhiComplex);
        changeToFrequencySpectrum(1);
        float phiPeak = findPeak(1);

        //Analysis for Theta
        //this.currAngle = THETA;
        convertToComplex(dataTheta, 2);
        this.dataThetaFrequency = fft(dataThetaComplex);
        changeToFrequencySpectrum(2);
        float thetaPeak = findPeak(2);

        //Analysis for Psi
        //this.currAngle = PSI;
        convertToComplex(dataPsi, 3);
        this.dataPsiFrequency = fft(dataPsiComplex);
        changeToFrequencySpectrum(3);
        float psiPeak = findPeak(3);

        //Determine the significant angle
        float[] movementDirection = (phiPeak > thetaPeak && phiPeak > psiPeak) ? dataPhiFrequencySpectrum: (thetaPeak > phiPeak && thetaPeak > psiPeak) ? dataThetaFrequencySpectrum: dataPsiFrequencySpectrum;
        significantPeak = (phiPeak > thetaPeak && phiPeak > psiPeak) ? phiPeak: (thetaPeak > phiPeak && thetaPeak > psiPeak) ? thetaPeak: psiPeak;

        if(significantPeak > 0){
            //find the relative time domain peak in that data set
            //Will use function from RepetitiveMotionPeriodicDetector to find rep of new periodic movement and fill that value into firstPeriodicMovement
            //float[] firstPeriodicMovement;
            return true;
        }
        return false;
    }

    /*
    This function converts euler angles into a complex array of data which consists of real and imaginary data
    Input: raw data array and empty Complex arra
    Output: array of Complex data
     */
    private void convertToComplex(float[] data, int currAngle){
        //Convert polar data into complex numbers
        //Using radius to be 1
        int index = 0;
        switch(currAngle){
            case 1:
                for (float val : data) {
                    this.dataPhiComplex[index] = new Complex();
                    this.dataPhiComplex[index].setRe((float)Math.cos(Math.toRadians(val)));
                    this.dataPhiComplex[index].setIm((float)Math.sin(Math.toRadians(val)));
                    index++;
                }
                break;
            case 2:
                for (float val : data) {
                    this.dataThetaComplex[index] = new Complex();
                    this.dataThetaComplex[index].setRe((float)Math.cos(Math.toRadians(val)));
                    this.dataThetaComplex[index].setIm((float)Math.sin(Math.toRadians(val)));
                    index++;
                }
                break;
            case 3:
                for (float val : data) {
                    this.dataPsiComplex[index] = new Complex();
                    this.dataPsiComplex[index].setRe((float)Math.cos(Math.toRadians(val)));
                    this.dataPsiComplex[index].setIm((float)Math.sin(Math.toRadians(val)));
                    index++;
                }
                break;
        }
    }

    //Need to update to remove threshold
    /*
    This function finds the peak values from an array which contains magnitude of the frequency spectrum
    Input: array that contains the frequency spectrum and respective magnitudes
    Output: peak value
     */
    private float findPeak(int currAngle){
        //Frequencies are in Hertz
        float peakVal = 0;
        int countAboveThreshold = 0;
        float threshold = 150;
        switch(currAngle){
            case 1:
                for(int i = 10; i < arraySize; i++){
                    if(this.dataPhiFrequencySpectrum[i] > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataPhiFrequencySpectrum[i]){
                            peakVal = this.dataPhiFrequencySpectrum[i];
                        }
                    }
                }
                break;
            case 2:
                for(int i = 10; i < arraySize; i++){
                    if(this.dataThetaFrequencySpectrum[i] > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataThetaFrequencySpectrum[i]){
                            peakVal = this.dataThetaFrequencySpectrum[i];
                        }
                    }
                }
                break;
            case 3:
                for(int i = 10; i < arraySize; i++){
                    if(this.dataPsiFrequencySpectrum[i] > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataPsiFrequencySpectrum[i]){
                            peakVal = this.dataPsiFrequencySpectrum[i];
                        }
                    }
                }
                break;
        }
        return peakVal;
    }

    /*
    This function converts a complex array of data which consists of real and imaginary data into
    an array that contains the magnitude of the fourier spectrum
    Input: Complex data array
    Output: float data array
     */
    private void changeToFrequencySpectrum(int currAngle){
        //convert into frequency power spectrum
        float factor = (float) Fs/arraySize; //sampling f of 100Hz, divided by fft length of arraysize - 256

        switch(currAngle){
            case 1:
                for(int i = 0; i < arraySize; i++){
                    this.dataPhiFrequencySpectrum[i] = (float) java.lang.Math.sqrt(java.lang.Math.pow(this.dataPhiFrequency[i].re(),2) + java.lang.Math.pow(this.dataPhiFrequency[i].im(),2));
                }
                break;
            case 2:
                for(int i = 0; i < arraySize; i++){
                    this.dataThetaFrequencySpectrum[i] = (float) java.lang.Math.sqrt(java.lang.Math.pow(this.dataThetaFrequency[i].re(),2) + java.lang.Math.pow(this.dataThetaFrequency[i].im(),2));
                }
                break;
            case 3:
                for(int i = 0; i < arraySize; i++){
                    this.dataPsiFrequencySpectrum[i] = (float) java.lang.Math.sqrt(java.lang.Math.pow(this.dataPsiFrequency[i].re(),2) + java.lang.Math.pow(this.dataPsiFrequency[i].im(),2));}
                break;
        }
    }

    /*
    This function converts a complex array of data which consists of real and imaginary data into
    an array that contains the frequency spectrum - the fast fourier transform recursively
    Input: Complex data array
    Output: Complex data array
     */
    private Complex[] fft(Complex[] x) {
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
            float kth = (float) (-2 * k * Math.PI / n);
            Complex wk = new Complex((float)Math.cos(kth), (float)Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }
}