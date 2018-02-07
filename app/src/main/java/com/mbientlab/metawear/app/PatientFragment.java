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

    public float[] pitch_data = new float[capacity]; //this will be used to store unfiltered data
    public float[] roll_data = new float[capacity]; //this will be used to store unfiltered data
    public float[] yaw_data = new float[capacity]; //this will be used to store unfiltered data

    //this has the definitions for the b values for convolution, gathered from MATLAB's fir1
    public double[] pitch_b = {0.000779956276928257, 0.000840665872371555, 0.000894497959742675,
            0.000922928033512174, 0.000893085626721506, 0.00076116965958513, 0.000478545449042225,
            -5.73837960208593E-19, -0.000706696809086334, -0.00165102681769932, -0.00281099159279643,
            -0.00412665195241372, -0.00549703415158719, -0.00678121936932106, -0.00780404235062265,
            -0.00836635873181521, -0.00825934535557941, -0.00728182559480732, -0.00525921393862945,
            -0.00206239663729071, 0.00237525704322728, 0.00804450547898497, 0.0148519892234465,
            0.0226182590142883, 0.0310826856028395, 0.0399152567398694, 0.0487346385904925,
            0.0571312865009099, 0.0646938920482515, 0.0710371002022807, 0.0758282593670764,
            0.0788109978766488, 0.0798236534708593, 0.0788109978766488, 0.0758282593670764,
            0.0710371002022807, 0.0646938920482515, 0.0571312865009099, 0.0487346385904925,
            0.0399152567398694, 0.0310826856028395, 0.0226182590142883, 0.0148519892234465,
            0.00804450547898497, 0.00237525704322728, -0.00206239663729071, -0.00525921393862945,
            -0.00728182559480732, -0.00825934535557941, -0.00836635873181521, -0.00780404235062265,
            -0.00678121936932106, -0.00549703415158719, -0.00412665195241372, -0.00281099159279643,
            -0.00165102681769932, -0.000706696809086334, -5.73837960208593E-19, 0.000478545449042225,
            0.00076116965958513, 0.000893085626721506, 0.000922928033512174, 0.000894497959742675,
            0.000840665872371555, 0.000779956276928257};
    public double[] roll_b = {0.000779956276928257, 0.000840665872371555, 0.000894497959742675,
            0.000922928033512174, 0.000893085626721506, 0.00076116965958513, 0.000478545449042225,
            -5.73837960208593E-19, -0.000706696809086334, -0.00165102681769932, -0.00281099159279643,
            -0.00412665195241372, -0.00549703415158719, -0.00678121936932106, -0.00780404235062265,
            -0.00836635873181521, -0.00825934535557941, -0.00728182559480732, -0.00525921393862945,
            -0.00206239663729071, 0.00237525704322728, 0.00804450547898497, 0.0148519892234465,
            0.0226182590142883, 0.0310826856028395, 0.0399152567398694, 0.0487346385904925,
            0.0571312865009099, 0.0646938920482515, 0.0710371002022807, 0.0758282593670764,
            0.0788109978766488, 0.0798236534708593, 0.0788109978766488, 0.0758282593670764,
            0.0710371002022807, 0.0646938920482515, 0.0571312865009099, 0.0487346385904925,
            0.0399152567398694, 0.0310826856028395, 0.0226182590142883, 0.0148519892234465,
            0.00804450547898497, 0.00237525704322728, -0.00206239663729071, -0.00525921393862945,
            -0.00728182559480732, -0.00825934535557941, -0.00836635873181521, -0.00780404235062265,
            -0.00678121936932106, -0.00549703415158719, -0.00412665195241372, -0.00281099159279643,
            -0.00165102681769932, -0.000706696809086334, -5.73837960208593E-19, 0.000478545449042225,
            0.00076116965958513, 0.000893085626721506, 0.000922928033512174, 0.000894497959742675,
            0.000840665872371555, 0.000779956276928257};
    public double[] yaw_b = {0.000779956276928257, 0.000840665872371555, 0.000894497959742675,
            0.000922928033512174, 0.000893085626721506, 0.00076116965958513, 0.000478545449042225,
            -5.73837960208593E-19, -0.000706696809086334, -0.00165102681769932, -0.00281099159279643,
            -0.00412665195241372, -0.00549703415158719, -0.00678121936932106, -0.00780404235062265,
            -0.00836635873181521, -0.00825934535557941, -0.00728182559480732, -0.00525921393862945,
            -0.00206239663729071, 0.00237525704322728, 0.00804450547898497, 0.0148519892234465,
            0.0226182590142883, 0.0310826856028395, 0.0399152567398694, 0.0487346385904925,
            0.0571312865009099, 0.0646938920482515, 0.0710371002022807, 0.0758282593670764,
            0.0788109978766488, 0.0798236534708593, 0.0788109978766488, 0.0758282593670764,
            0.0710371002022807, 0.0646938920482515, 0.0571312865009099, 0.0487346385904925,
            0.0399152567398694, 0.0310826856028395, 0.0226182590142883, 0.0148519892234465,
            0.00804450547898497, 0.00237525704322728, -0.00206239663729071, -0.00525921393862945,
            -0.00728182559480732, -0.00825934535557941, -0.00836635873181521, -0.00780404235062265,
            -0.00678121936932106, -0.00549703415158719, -0.00412665195241372, -0.00281099159279643,
            -0.00165102681769932, -0.000706696809086334, -5.73837960208593E-19, 0.000478545449042225,
            0.00076116965958513, 0.000893085626721506, 0.000922928033512174, 0.000894497959742675,
            0.000840665872371555, 0.000779956276928257};

    int sizeofpitchb = pitch_b.length;
    int sizeofpitchdata = pitch_data.length;
    int numpitchrows = (sizeofpitchdata + sizeofpitchb) - 1; //minus 1 since arrays start at 0
    float[] pitch_filtered = new float[numpitchrows]; //y is longer than x by b-1 post convolution

    int sizeofrollb = roll_b.length;
    int sizeofrolldata = roll_data.length;
    int numrollrows = (sizeofrolldata + sizeofrollb) - 1; //minus 1 since arrays start at 0
    float[] roll_filtered = new float[numrollrows]; //y is longer than x by b-1 post convolution

    int sizeofyawb = yaw_b.length;
    int sizeofyawdata = yaw_data.length;
    int numyawrows = (sizeofyawdata + sizeofyawb) - 1; //minus 1 since arrays start at 0
    float[] yaw_filtered = new float[numyawrows]; //y is longer than x by b-1 post convolution

    public PatientFragment() {
        super(R.string.navigation_fragment_patient, R.layout.fragment_patientdata, -1f, 1f);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Intent intent = getActivity().getIntent();
        ((TextView) view.findViewById(R.id.textpatientname)).setText(intent.getStringExtra(PatientName.EXTRA_PATIENT_NAME));

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
            //this clears slot 0 and eliminates the least recent data point
            for (int i = (capacity - 1); i > 0; i--) {
                pitch_data[i] = pitch_data[i - 1];
                roll_data[i] = roll_data[i - 1];
                yaw_data[i] = yaw_data[i - 1];
            }

            //store each angle as the first entry in the array and flip data
            pitch_data[0] = angles.pitch(); //this should return the current pitch as a float
            pitch_flipped = FlipCheck(pitch_data, pitch_flipped);
            if (pitch_flipped == 1) { //if we get that the current value is flipped, we must add 360
                pitch_data[0] = pitch_data[0] + 360;
            }

            roll_data[0] = angles.roll(); //this should return the current roll as a float
            roll_flipped = FlipCheck(roll_data, roll_flipped);
            if (roll_flipped == 1) { //if we get that the current value is flipped, we must add 360
                roll_data[0] = roll_data[0] + 360;
            }

            yaw_data[0] = angles.yaw(); //this should return the current yaw as a float
            yaw_flipped = FlipCheck(yaw_data, yaw_flipped);
            if (yaw_flipped == 1) { //if we get that the current value is flipped, we must add 360
                yaw_data[0] = yaw_data[0] + 360;
            }

            //call the Convolution function, filtered data is separate as we want to fourier unfiltered
            pitch_filtered = Convolution(pitch_b, pitch_data);
            roll_filtered = Convolution(roll_b, roll_data);
            yaw_filtered = Convolution(yaw_b, yaw_data);


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

    //This function performs the convolution as a part of the FIR filter, first we must set our x
    //to a 2D array, each row corresponds to 1 set of values that will be multiplied by b
    //for example if we want [1 2 3 4 5 6] to be multiplied by [0.1 0.2 0.3] our 2D array would be
    //[1 0 0]
    //[2 1 0]
    //[3 2 1]
    //[4 3 2]
    //[5 4 3]
    //[6 5 4]
    //[0 6 5]
    //[0 0 6]
    //this function returns our y post convolution of y = b*x
    private static float[] Convolution(double[] b, float[] data) {
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
                sum = (multi[r][c] * (float)b[c]) + sum;
            }
            y[r] = sum;
        }

        //The code below was used to print out the results of convolution and was used for debugging
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

    //this function is used to resolve the fact that the raw Euler Angles from the sensor do are
    //being flipped when they are over 360 degrees, for example 361 degrees would be sent as 1 degree
    public static int FlipCheck(float[] data, int alreadyflipped) {
        int i = 0; //this is used as an index to get the previous data point and current
        int flip; //this returned, 0 is false, 1 is true
        float lastval = data[i + 1];
        float currval = data[i];

        //if the data is already flipped we are checking for the point when the current angle is
        //less than 360 and the last was over 360
        if (alreadyflipped == 1) {  //alreadyflipped is passed in
            if ((lastval > 360) && (currval > 300))
                flip = 0; //if it is no longer flipped, we return false
            else
                flip = 1; //otherwise we return true
        }

        //if the last point was not flipped then we are checking if the last point was over 300
        //and the current is under 50--> I can show all this logic graphically if it's confusing
        else {
            if ((lastval > 300) && (currval < 50)) {
                flip = 1;
            } else
                flip = 0;
        }

        return flip; //basically returning a true or false on whether the data is still flipped
    }

}
