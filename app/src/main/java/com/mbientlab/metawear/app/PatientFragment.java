/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.AccRange;
import com.mbientlab.metawear.module.SensorFusionBosch.GyroRange;
import com.mbientlab.metawear.module.SensorFusionBosch.Mode;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by kahlan on 01/24/2018.
 */

public class PatientFragment extends PatientFragmentBase {
    private static final float SAMPLING_PERIOD = 1 / 100f;

    private final ArrayList<Entry> x0 = new ArrayList<>(), x1 = new ArrayList<>(), x2 = new ArrayList<>(), x3 = new ArrayList<>();
    private SensorFusionBosch sensorFusion;

    //this are new definitions added by Janelle
    //private int index = 0; //used to index the circular arrays
    private int capacity = 100; //this is the maximum number of entries we will have in the circular arrays

    private int pitch_flipped = 0; //set to 1 when the data is flipped
    private int roll_flipped = 0; //set to 1 when the data is flipped
    private int yaw_flipped = 0; //set to 1 when the data is flipped

    public float[] pitch_data = new float[capacity];
    public float[] roll_data = new float[capacity];
    public float[] yaw_data = new float[capacity];

    public float[] pitch_b = {};
    public float[] roll_b = {};
    public float[] yaw_b = {};

    int sizeofpitchb = pitch_b.length;
    int sizeofpitchdata = pitch_data.length;
    int numpitchrows = (sizeofpitchdata + sizeofpitchb) - 1;
    float[] pitch_filtered = new float[numpitchrows];

    int sizeofrollb = roll_b.length;
    int sizeofrolldata = roll_data.length;
    int numrollrows = (sizeofrolldata + sizeofrollb) - 1;
    float[] roll_filtered = new float[numrollrows];

    int sizeofyawb = yaw_b.length;
    int sizeofyawdata = yaw_data.length;
    int numyawrows = (sizeofyawdata + sizeofyawb) - 1;
    float[] yaw_filtered = new float[numyawrows];

    public PatientFragment() {
        super(R.string.navigation_fragment_patient, R.layout.fragment_patientdata, -1f, 1f);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Intent intent = getActivity().getIntent();
        ((TextView) view.findViewById(R.id.textpatientname)).setText(intent.getStringExtra(PatientName.PATIENT_NAME));

        final YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaxValue(360f);
        leftAxis.setAxisMinValue(-360f);

        refreshChart(false);
    }

    @Override
    protected void setup() {
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .accRange(AccRange.AR_16G)
                .gyroRange(GyroRange.GR_2000DPS)
                .commit();

        sensorFusion.eulerAngles().addRouteAsync(source -> source.stream((data, env) -> {
            LineData chartData = chart.getData();

            final EulerAngles angles = data.value(EulerAngles.class);
            chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * SAMPLING_PERIOD));
            chartData.addEntry(new Entry(angles.heading(), sampleCount), 0);
            chartData.addEntry(new Entry(angles.pitch(), sampleCount), 1);
            chartData.addEntry(new Entry(angles.roll(), sampleCount), 2);
            chartData.addEntry(new Entry(angles.yaw(), sampleCount), 3);

            //this is new code added by Janelle to save the data into a circular array
            //this for loop shift all the data along in the array before adding a new data point
            for (int i = (capacity - 1); i > 0; i--) {
                pitch_data[i] = pitch_data[i - 1];
                roll_data[i] = roll_data[i - 1];
                yaw_data[i] = yaw_data[i - 1];
            }

            //store each angle as the first entry in the array and flip data
            pitch_data[0] = angles.pitch();
            pitch_flipped = FlipCheck(pitch_data, pitch_flipped);
            if (pitch_flipped == 1) {
                pitch_data[0] = pitch_data[0] + 360;
            }

            roll_data[0] = angles.roll();
            if (roll_flipped == 1) {
                roll_data[0] = roll_data[0] + 360;
            }

            yaw_data[0] = angles.yaw();
            if (yaw_flipped == 1) {
                yaw_data[0] = yaw_data[0] + 360;
            }

            //call the Convolution function
            pitch_filtered = Convolution(pitch_b, pitch_data);
            roll_filtered = Convolution(roll_b, roll_data);
            yaw_filtered = Convolution(yaw_b, yaw_data);

            //this just adds the new point at the next slot in the array, not the first
            /*index++;
            if (index == capacity)
                index = 0;*/

            //the new code ends here

            sampleCount++;

            updateChart();
        })).continueWith(task -> {
            streamRoute = task.getResult();
            sensorFusion.eulerAngles().start();
            sensorFusion.start();

            return null;
        });
    }

    @Override
    protected void clean() {
        sensorFusion.stop();
    }

    @Override
    protected void resetData(boolean clearData) {
        if (clearData) {
            sampleCount = 0;
            chartXValues.clear();
            x0.clear();
            x1.clear();
            x2.clear();
            x3.clear();
        }

        ArrayList<LineDataSet> spinAxisData = new ArrayList<>();
        spinAxisData.add(new LineDataSet(x0, "heading"));
        spinAxisData.get(0).setColor(Color.BLACK);
        spinAxisData.get(0).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x1, "pitch"));
        spinAxisData.get(1).setColor(Color.RED);
        spinAxisData.get(1).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x2, "roll"));
        spinAxisData.get(2).setColor(Color.GREEN);
        spinAxisData.get(2).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x3, "yaw"));
        spinAxisData.get(3).setColor(Color.BLUE);
        spinAxisData.get(3).setDrawCircles(false);

        LineData data = new LineData(chartXValues);
        for (LineDataSet set : spinAxisData) {
            data.addDataSet(set);
        }
        data.setDrawValues(false);
        chart.setData(data);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        sensorFusion = mwBoard.getModuleOrThrow(SensorFusionBosch.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_sensor_fusion_data, R.string.config_desc_sensor_fusion_data));
    }


    private static float[] Convolution(float[] b, float[] data) {
        int sizeofb = b.length; //the size of b
        int sizeofdata = data.length; //the number of data points we are storing
        int numrows = (sizeofdata + sizeofb) - 1; //the number of rows depends on the number of delays
        float[][] multi = new float[numrows][3]; //a 2-d matrix to store a matrix with 0s for convolution
        float[] y = new float[numrows];
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
            float sum = 0;
            for (c = 0; c < sizeofb; c++) {
                sum = (multi[r][c] * b[c]) + sum;
            }
            y[r] = sum;
        }

        /*for(r = 0; r < 7; r++){
            for(c = 0; c < 3; c++){
                System.out.println(multi[r][c]);
            }
        }

        System.out.println(" ________________ ");

        for(r=0; r < 7; r++){
            System.out.println(y[r]);
        } */

        return y;

    }

    private static int FlipCheck(float[] data, int alreadyflipped) {
        int i = 0;
        int flip;
        float lastval = data[i + 1];
        float currval = data[i];

        if (alreadyflipped == 1) {
            if ((lastval < 50) && (currval > 300))
                flip = 0;
            else
                flip = 1;
        } else {
            if ((lastval > 300) && (currval < 50)) {
                flip = 1;
            } else
                flip = 0;
        }

        return flip;
    }

    public static void InitialPeriodicMotionDetector(String args[]){

        int arraySize = 256;
        //double[] dataPhi = new double[arraySize];

        double[] dataPhi = new double[arraySize];
        double[] dataPhiReal = new double[arraySize];
        double[] dataPhiComplex = new double[arraySize];
        double[] dataPhiFrequency = new double[arraySize*2];
        double[] dataPhiFrequencySpectrum = new double[arraySize];

        //Convert polar data into complex numbers
        //Using radius to be 1
        int index = 0;

        for (double phi : dataPhi) {
            dataPhiReal[index] = Math.cos(Math.toRadians(phi));
            dataPhiComplex[index] = Math.sin(Math.toRadians(phi));
            //System.out.println("Current index is: " + index);
            index++;
        }

        dataPhiFrequency = fft(dataPhiReal, dataPhiComplex, true);

        //convert into frequency power spectrum
        double factor = 0.78125; //sampling f of 100Hz, divided by fft length of 128
        for(int i = 0; i < arraySize; i=i+2){
            dataPhiFrequencySpectrum[i/2] = (java.lang.Math.pow(dataPhiFrequency[i],2) + java.lang.Math.pow(dataPhiFrequency[i+1],2));
            System.out.println("Current frequency power is: " + dataPhiFrequencySpectrum[i/2] + " n: " + (i/2) + " and frequency is: " + factor*i/2 + "Hz");
        }

        //Use temporal cutoff of 4 - as 0.78125*4 ~ 1 Hz, and continue until 5 Hz which is
        //assumption that frequencies are in radians
        double peakVal = 0;
        int countAboveThreshold = 0;
        double threshold = 30.0;

        for(int i = 10; i < arraySize; i++){
            if(dataPhiFrequencySpectrum[i] > threshold){
                countAboveThreshold++;
                if(peakVal < dataPhiFrequencySpectrum[i]){
                    peakVal = dataPhiFrequencySpectrum[i];
                    System.out.println("Current frequency power is: " + dataPhiFrequencySpectrum[i]);
                }
            }
        }
    }
    /**
     * @author Orlando Selenu
     *
     * The Fast Fourier Transform (generic version, with NO optimizations).
     *
     * @param inputReal
     *            an array of length n, the real part
     * @param inputImag
     *            an array of length n, the imaginary part
     * @param DIRECT
     *            TRUE = direct transform, FALSE = inverse transform
     * @return a new array of length 2n
     */
    public static double[] fft(double[] inputReal, double[] inputImag,
                               boolean DIRECT) {
        // - n is the dimension of the problem
        // - nu is its logarithm in base e
        int n = inputReal.length;

        // If n is a power of 2, then ld is an integer (_without_ decimals)
        double ld = Math.log(n) / Math.log(2.0);

        // Here I check if n is a power of 2. If exist decimals in ld, I quit
        // from the function returning null.
        if (((int) ld) - ld != 0) {
            System.out.println("The number of elements is not a power of 2.");
            return null;
        }

        // Declaration and initialization of the variables
        // ld should be an integer, actually, so I don't lose any information in
        // the cast
        int nu = (int) ld;
        int n2 = n / 2;
        int nu1 = nu - 1;
        double[] xReal = new double[n];
        double[] xImag = new double[n];
        double tReal, tImag, p, arg, c, s;

        // Here I check if I'm going to do the direct transform or the inverse
        // transform.
        double constant;
        if (DIRECT)
            constant = -2 * Math.PI;
        else
            constant = 2 * Math.PI;

        // I don't want to overwrite the input arrays, so here I copy them. This
        // choice adds \Theta(2n) to the complexity.
        for (int i = 0; i < n; i++) {
            xReal[i] = inputReal[i];
            xImag[i] = inputImag[i];
        }

        // First phase - calculation
        int k = 0;
        for (int l = 1; l <= nu; l++) {
            while (k < n) {
                for (int i = 1; i <= n2; i++) {
                    p = bitreverseReference(k >> nu1, nu);
                    // direct FFT or inverse FFT
                    arg = constant * p / n;
                    c = Math.cos(arg);
                    s = Math.sin(arg);
                    tReal = xReal[k + n2] * c + xImag[k + n2] * s;
                    tImag = xImag[k + n2] * c - xReal[k + n2] * s;
                    xReal[k + n2] = xReal[k] - tReal;
                    xImag[k + n2] = xImag[k] - tImag;
                    xReal[k] += tReal;
                    xImag[k] += tImag;
                    k++;
                }
                k += n2;
            }
            k = 0;
            nu1--;
            n2 /= 2;
        }

        // Second phase - recombination
        k = 0;
        int r;
        while (k < n) {
            r = bitreverseReference(k, nu);
            if (r > k) {
                tReal = xReal[k];
                tImag = xImag[k];
                xReal[k] = xReal[r];
                xImag[k] = xImag[r];
                xReal[r] = tReal;
                xImag[r] = tImag;
            }
            k++;
        }

        // Here I have to mix xReal and xImag to have an array (yes, it should
        // be possible to do this stuff in the earlier parts of the code, but
        // it's here to readibility).
        double[] newArray = new double[xReal.length * 2];
        double radice = 1 / Math.sqrt(n);
        for (int i = 0; i < newArray.length; i += 2) {
            int i2 = i / 2;
            // I used Stephen Wolfram's Mathematica as a reference so I'm going
            // to normalize the output while I'm copying the elements.
            newArray[i] = xReal[i2] * radice;
            newArray[i + 1] = xImag[i2] * radice;
        }
        return newArray;
    }

    /**
     * The reference bitreverse function.
     */
    public static int bitreverseReference(int j, int nu) {
        int j2;
        int j1 = j;
        int k = 0;
        for (int i = 1; i <= nu; i++) {
            j2 = j1 / 2;
            k = 2 * k + j1 - 2 * j2;
            j1 = j2;
        }
        return k;
    }
}
