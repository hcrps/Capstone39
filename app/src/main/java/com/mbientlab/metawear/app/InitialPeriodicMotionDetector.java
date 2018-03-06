package com.mbientlab.metawear.app;

import com.github.mikephil.charting.data.Entry;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by janellesomerville on 2018-03-02.
 */

public class InitialPeriodicMotionDetector {

    int Fs;
    int arraySize;

    Complex[] dataPhiComplex;
    Complex[] dataPhiFrequency;
    private double[] dataPhiFrequencySpectrum;

    Complex[] dataThetaComplex;
    Complex[] dataThetaFrequency;
    private double[] dataThetaFrequencySpectrum;

    Complex[] dataPsiComplex;
    Complex[] dataPsiFrequency;
    double[] dataPsiFrequencySpectrum;

    private double significantPeak;
    private double magnitude;
    private String peakDetected;

    public String getPeakDetected(){
        return this.peakDetected;
    }

    /*----------------------------------*/
    ArrayList<Double> dataPhiFrequencySpectrumList = new ArrayList<Double>();
    ArrayList<Complex> dataPhiComplexList = new ArrayList<Complex>();
    ArrayList<Complex> dataPhiFrequencyList = new ArrayList<Complex>();

    ArrayList<Double> dataThetaFrequencySpectrumList = new ArrayList<Double>();
    ArrayList<Complex> dataThetaComplexList = new ArrayList<Complex>();
    ArrayList<Complex> dataThetaFrequencyList = new ArrayList<Complex>();

    ArrayList<Double> dataPsiFrequencySpectrumList = new ArrayList<Double>();
    ArrayList<Complex> dataPsiComplexList = new ArrayList<Complex>();
    ArrayList<Complex> dataPsiFrequencyList = new ArrayList<Complex>();


    public ArrayList<Double> getPitchFreq(){
        return dataPhiFrequencySpectrumList;
    }

    public ArrayList<Double> getRollFreq(){
        return dataThetaFrequencySpectrumList;
    }

    public ArrayList<Double> getYawFreq(){
        return dataPsiFrequencySpectrumList;
    }

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

        //CREATE SIZE FILLED WITH ZEROS
        this.dataPhiComplex = new Complex[arraySize];
        this.dataPhiFrequencySpectrum = new double[arraySize];

        this.dataThetaComplex = new Complex[arraySize];
        dataThetaFrequencySpectrum = new double[arraySize];

        this.dataPsiComplex = new Complex[arraySize];
        this.dataPsiFrequencySpectrum = new double[arraySize];


        Complex temp = new Complex(0.0,0.0);

        for(int i=0; i < arraySize; i++){
            dataPhiFrequencySpectrumList.add(0.0);
            dataThetaFrequencySpectrumList.add(0.0);
            dataPsiFrequencySpectrumList.add(0.0);
            dataPhiComplexList.add(temp);
            dataPsiComplexList.add(temp);
            dataThetaComplexList.add(temp);

        }

    }

    public int getFs(){
        return Fs;
    }

    public int getArraySize (){
        return arraySize;
    }

    public double getFreq (){
        return significantPeak;
    }

    public double getMag (){
        return magnitude;
    }

    public boolean isPeriodic(ArrayList<Double> dataPhi, ArrayList<Double> dataTheta, ArrayList<Double> dataPsi) {
        this.dataPhiFrequencySpectrumList.clear();
        this.dataThetaFrequencySpectrumList.clear();
        this.dataPsiFrequencySpectrumList.clear();
        this.dataPhiFrequencyList.clear();
        this.dataThetaFrequencyList.clear();
        this.dataPsiFrequencyList.clear();
        //Analysis for Phi
        //this.currAngle = PHI;

        convertToComplex(dataPhi, 1);

        //Convert dataPhiFrequencyList to array of Complex
        //for(int i=0; i < arraySize; i++){
        for(int i=0; i < arraySize; i++){
            this.dataPhiComplex[i] = this.dataPhiComplexList.get(i);
        }
        this.dataPhiFrequency = fft(dataPhiComplex);
        //Convert dataPhiComplexList back to an ArrayList of Complex type

        for(int i=0; i < arraySize; i ++){
            this.dataPhiFrequencyList.add(i, this.dataPhiFrequency[i]);
        }

        //System.out.print("finished pitch");


        changeToFrequencySpectrum(1);
        double[] phi = findPeak(1);
        //System.out.print("HEREE "+ phi[0] + " " + phi[1]);
        double phiPeak = phi[1];
        double phiMag = phi[0];
        //boolean resultPhi = findPeak2(1);

        //Analysis for Theta
        //this.currAngle = THETA;
        convertToComplex(dataTheta, 2);
        //this.dataThetaFrequencyList = fft(dataThetaComplexList);

        //boolean resultTheta = findPeak2(2);*/
        for(int i=0; i < arraySize; i++){
            this.dataThetaComplex[i] = this.dataThetaComplexList.get(i);
        }
        this.dataThetaFrequency = fft(this.dataThetaComplex);
        //this.dataPhiFrequencyList = fft(dataPhiComplexList);
        //Convert dataPhiComplexList back to an ArrayList of Complex type

        for(int i=0; i < arraySize; i ++){
            this.dataThetaFrequencyList.add(this.dataThetaFrequency[i]);
        }
        changeToFrequencySpectrum(2);
        double[] theta = findPeak(2);
        double thetaPeak = theta[1];
        double thetaMag = theta[0];

        // System.out.print("finished roll");

        //Analysis for Psi
        //this.currAngle = PSI;
        convertToComplex(dataPsi, 3);
        //this.dataPsiFrequencyList = fft(dataPsiComplexList);
        for(int i=0; i < arraySize; i++){
            this.dataPsiComplex[i] = this.dataPsiComplexList.get(i);
        }
        this.dataPsiFrequency = fft(this.dataPsiComplex);
        //this.dataPhiFrequencyList = fft(dataPhiComplexList);
        //Convert dataPhiComplexList back to an ArrayList of Complex type

        for(int i=0; i < arraySize; i ++){
            this.dataPsiFrequencyList.add(this.dataPsiFrequency[i]);
        }
        /*for(int i = 0; i < arraySize; i++) {
            System.out.print(this.dataPhiFrequencyList.get(i) + ", ");
        }
        System.out.print("\n");*/
        changeToFrequencySpectrum(3);
        double[] psi = findPeak(3);
        double psiPeak = psi[1];
        //double psiPeak[] = findPeak(3);
        double psiMag = psi[0];
        // System.out.print(" PHI PEAK: " + phiPeak +" THETA PEAK: " + thetaPeak + " PSI PEAK: " + psiPeak +"\n");
        //boolean resultPsi =  findPeak2(3);

        //Determine the significant angle
        ArrayList<Double> movementDirection = (phiPeak > thetaPeak && phiPeak > psiPeak) ? dataPhiFrequencySpectrumList: (thetaPeak > phiPeak && thetaPeak > psiPeak) ? dataThetaFrequencySpectrumList: dataPsiFrequencySpectrumList;
        if(phiPeak > thetaPeak && phiPeak > psiPeak) {
            this.significantPeak = phiPeak;
            this.magnitude = phiMag;
            this.peakDetected = "Pitch";
        }
        else if(thetaPeak > phiPeak && thetaPeak > psiPeak) {
            this.significantPeak = thetaPeak;
            this.magnitude = thetaMag;
            this.peakDetected = "Roll";
        }
        else if (psiPeak != 0.0){
            this.significantPeak = psiPeak;
            this.magnitude = psiMag;
            this.peakDetected = "Yaw";
        }
        //System.out.println("SIG PEAK: " + this.significantPeak);

        //System.out.println(significantPeak);

        /*if(resultPhi || resultPsi || resultTheta){
            System.out.println("Peak at Phi" + resultPhi + "Peak at Theta" + resultTheta + "Peak at Psi" + resultPsi);
            return true;
        }*/

        if(significantPeak > 0 && magnitude > 10){
            //find the relative time domain peak in that data set
            //Will use function from RepetitiveMotionPeriodicDetector to find rep of new periodic movement and fill that value into firstPeriodicMovement
            //double[] firstPeriodicMovement;
            //System.out.println("Found a peak");
            return true;
        }
        return false;
    }

    /*
    This function converts euler angles into a complex array of data which consists of real and imaginary data
    Input: raw data array and empty Complex arra
    Output: array of Complex data
     */
    public void convertToComplex(ArrayList<Double> data, int currAngle){
        //Convert polar data into complex numbers
        //Using radius to be 1
        int index = 0;
        double val = data.get(0);
        switch(currAngle){
            case 1:
                //for (double val : data) {
                this.dataPhiComplexList.add(index, new Complex((double)Math.cos(Math.toRadians(val)), (double)Math.sin(Math.toRadians(val))));
                this.dataPhiComplexList.remove(arraySize);
                //index++;
                //}
                break;
            case 2:
                //for (double val : data) {
                this.dataThetaComplexList.add(index, new Complex((double)Math.cos(Math.toRadians(val)), (double)Math.sin(Math.toRadians(val))));
                this.dataThetaComplexList.remove(arraySize);
                //index++;
                //}
                break;
            case 3:
                //for (double val : data) {
                this.dataPsiComplexList.add(index, new Complex((double)Math.cos(Math.toRadians(val)), (double)Math.sin(Math.toRadians(val))));
                this.dataPsiComplexList.remove(arraySize);
                //index++;
                //}
                break;
        }
    }

    //Need to update to remove threshold
    /*
    This function finds the peak values from an array which contains magnitude of the frequency spectrum
    Input: array that contains the frequency spectrum and respective magnitudes
    Output: peak value
     */
    private double[] findPeak(int currAngle){
        //Frequencies are in Hertz
        double magnitude = 0;
        double peakVal = 0;
        double factor = (double) Fs/ (double) arraySize;
        double delta_f = 0.04625;
        int maxIndex = (int)((0.5/delta_f)+1);//(100 + (arraySize/2)); //0.01*100 = 1Hz
        int minIndex = (int) (0.2/delta_f);
        double locationOfPeak = 0;
        int smallestStart = 1;
        int countAboveThreshold = 0;
        double threshold = 130;

        int min = maxIndex;
        switch(currAngle){
            case 1:
                for(int i = 1; i <maxIndex; i++) {
                    if (this.dataPhiFrequencySpectrumList.get(i) > this.dataPhiFrequencySpectrumList.get(i - 1)) {
                        min = i;
                        break;
                    }
                }
                if(min > minIndex){
                    for(int i=min;i<maxIndex;i++){
                        if(this.dataPhiFrequencySpectrumList.get(i) < this.dataPhiFrequencySpectrumList.get(i-1)){
                            peakVal = (i*delta_f);
                            magnitude = this.dataPhiFrequencySpectrumList.get(i);
                            break;
                        }
                    }
                }
                break;
               /*for(int i = smallestStart; i < (int)(maxFrequency/factor); i++){
                    if(this.dataPhiFrequencySpectrumList.get(i) > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataPhiFrequencySpectrumList.get(i)){
                            peakVal = this.dataPhiFrequencySpectrumList.get(i);
                            locationOfPeak = (double)i*Fs/arraySize;
                        }
                    }
                }
                break;*/
            case 2:
                for(int i = 1; i <maxIndex; i++) {
                    if (this.dataThetaFrequencySpectrumList.get(i) > this.dataThetaFrequencySpectrumList.get(i - 1)) {
                        min = i;
                        break;
                    }
                }
                if(min > minIndex){
                    for(int i=min;i<maxIndex;i++){
                        if(this.dataThetaFrequencySpectrumList.get(i) < this.dataThetaFrequencySpectrumList.get(i-1)){
                            peakVal = (i*delta_f);
                            magnitude = this.dataThetaFrequencySpectrumList.get(i);
                            break;
                        }
                    }
                }
                break;
                /*for(int i = smallestStart; i < (int)(maxFrequency/factor); i++){
                    if(this.dataThetaFrequencySpectrumList.get(i) > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataThetaFrequencySpectrumList.get(i)){
                            peakVal = this.dataThetaFrequencySpectrumList.get(i);
                            locationOfPeak = (double)i*Fs/arraySize;
                        }
                    }
                }
                break;*/
            case 3:
                for(int i = 1; i <maxIndex; i++) {
                    if (this.dataPsiFrequencySpectrumList.get(i) > this.dataPsiFrequencySpectrumList.get(i - 1)) {
                        min = i;
                        //System.out.print("Iteration: "+ Double.toString(this.dataPsiFrequencySpectrumList.get(0)) + " MIN: " + min);
                        break;
                    }
                }
                //System.out.print("MIN:" + min + " CurrAngle: " + currAngle + "\n");
                if(min > minIndex){
                    for(int i=min;i<maxIndex;i++){
                        if(this.dataPsiFrequencySpectrumList.get(i) < this.dataPsiFrequencySpectrumList.get(i-1)){
                            peakVal = (i*delta_f);
                            magnitude = this.dataPsiFrequencySpectrumList.get(i);
                            //System.out.print(" MAX: " + i + " Peak Val: " + peakVal + "Mag: " + magnitude + "\n");
                            //System.out.print("MIN:" + min + " CurrAngle: " + peakVal + "\n");
                            break;
                        }
                    }
                }
                break;
                /*for(int i = smallestStart; i < (int)(maxFrequency/factor); i++){
                    if(this.dataPsiFrequencySpectrumList.get(i) > threshold){
                        countAboveThreshold++;
                        if(peakVal < this.dataPsiFrequencySpectrumList.get(i)){
                            peakVal = this.dataPsiFrequencySpectrumList.get(i);
                            locationOfPeak = (double)i*Fs/arraySize;
                        }
                    }
                }
                break;*/
        }
        //System.out.println("Maximum frequency power is: " + peakVal + " and number of values above power frequency of 30 are: " + countAboveThreshold);
        //return peakVal;
        double arrayout[] = {magnitude, peakVal};
       /* if (currAngle == 3) {
            System.out.println("Peak at " + peakVal + " Hz " + currAngle + " Mag " + magnitude);
            System.out.print("ARRAY OUT: " + arrayout[0] + " " + arrayout[1] + "\n");
        }*/
        return arrayout;
    }


    /*
    This function converts a complex array of data which consists of real and imaginary data into
    an array that contains the magnitude of the fourier spectrum
    Input: Complex data array
    Output: double data array
     */
    public void changeToFrequencySpectrum(int currAngle){
        //convert into frequency power spectrum
        double factor = (double) Fs/arraySize; //sampling f of 100Hz, divided by fft length of arraysize - 256

        switch(currAngle){
            case 1:
                for(int i = 0; i < arraySize; i++){
                    this.dataPhiFrequencySpectrumList.add(i,(double) java.lang.Math.sqrt(java.lang.Math.pow(this.dataPhiFrequencyList.get(i).re(),2) + java.lang.Math.pow(this.dataPhiFrequencyList.get(i).im(),2)));
                    //System.out.println(dataFrequencySpectrum[i]);
                    //System.out.println("Current frequency power is: " + dataFrequencySpectrum[i] + " n: " + (i) + " and frequency is: " + factor*i + "Hz");
                }
                break;
            case 2:
                for(int i = 0; i < arraySize; i++){
                    this.dataThetaFrequencySpectrumList.add(i, (double) java.lang.Math.sqrt(java.lang.Math.pow(this.dataThetaFrequencyList.get(i).re(),2) + java.lang.Math.pow(this.dataThetaFrequencyList.get(i).im(),2)));
                    //System.out.println(dataFrequencySpectrum[i]);
                    //System.out.println("Current frequency power is: " + dataFrequencySpectrum[i] + " n: " + (i) + " and frequency is: " + factor*i + "Hz");
                }
                break;
            case 3:
                for(int i = 0; i < arraySize; i++){
                    this.dataPsiFrequencySpectrumList.add(i, (double) java.lang.Math.sqrt(java.lang.Math.pow(this.dataPsiFrequencyList.get(i).re(),2) + java.lang.Math.pow(this.dataPsiFrequencyList.get(i).im(),2)));
                    //System.out.println(dataFrequencySpectrum[i]);
                    //System.out.println("Current frequency power is: " + dataFrequencySpectrum[i] + " n: " + (i) + " and frequency is: " + factor*i + "Hz");
                }
               /* System.out.print("Spec: ");
                for(int i = 0; i < arraySize; i++) {
                    System.out.print(this.dataPsiFrequencySpectrumList.get(i) + ", ");
                }
                System.out.print("\n");*/
                break;
        }
    }

    /*
    This function converts a complex array of data which consists of real and imaginary data into
    an array that contains the frequency spectrum - the fast fourier transform recursively
    Input: Complex data array
    Output: Complex data array
     */
    public Complex[] fft(Complex[] x) {
        int n = x.length;
        // System.out.println("N: " + n);

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
            double kth = (double) (-2 * k * Math.PI / n);
            Complex wk = new Complex((double)Math.cos(kth), (double)Math.sin(kth));
            y[k]       = q[k].plus(wk.times(r[k]));
            y[k + n/2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }
}