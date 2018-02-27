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
import android.os.Handler;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.widget.TextView;
import com.mbientlab.metawear.app.InitialPeriodicMotionDetector;

import org.w3c.dom.Text;

/**
 * Created by kahlan on 01/24/2018.
 */

public class PatientFragment extends PatientFragmentBase {
    private static final float SAMPLING_PERIOD = 1 / 200f;
    private final Handler chartHandler= new Handler();
    private long prevUpdate1 = -1;
    private long prevUpdate2 = -1;

    private int srcIndex = 1;

    //this are new definitions added by Janelle
    //private int index = 0; //used to index the circular arrays
    private int capacity = 256; //this is the maximum number of entries we will have in the circular arrays
    private final ArrayList<Entry> x0 = new ArrayList<>(capacity), x1 = new ArrayList<>(), x2 = new ArrayList<>(), x3 = new ArrayList<>();
    private SensorFusionBosch sensorFusion;

    private int pitch_flipped = 0; //set to 1 when the data is flipped
    private int roll_flipped = 0; //set to 1 when the data is flipped
    private int yaw_flipped = 0; //set to 1 when the data is flipped

    public float[] pitch_data = new float[capacity]; //this will be used to store unfiltered data
    public float[] roll_data = new float[capacity]; //this will be used to store unfiltered data
    public float[] yaw_data = new float[capacity]; //this will be used to store unfiltered data

    public float pitch, roll, yaw;

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

        final YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaxValue(400f);
        leftAxis.setAxisMinValue(-400f);

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

            final EulerAngles angles = data.value(EulerAngles.class);

            //store each angle as the first entry in the array and flip data
            pitch = angles.pitch(); //this should return the current pitch as a float
            roll = angles.roll();
            yaw = angles.yaw();

            // move data over
            for (int i = (capacity - 1); i > 0; i--) {
                pitch_data[i] = pitch_data[i - 1];
                roll_data[i] = roll_data[i - 1];
                yaw_data[i] = yaw_data[i - 1];
            }
            // add data point to buffer in
            pitch_data[0] = pitch;
            roll_data[0] = roll;
            yaw_data[0] = yaw;

            dataUpdate();

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
        }

        ArrayList<LineDataSet> spinAxisData = new ArrayList<>();

        spinAxisData.add(new LineDataSet(x1, "pitch"));
        spinAxisData.get(0).setColor(Color.rgb(139, 233, 253));
        spinAxisData.get(0).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x2, "roll"));
        spinAxisData.get(1).setColor(Color.rgb(80, 250, 123));
        spinAxisData.get(1).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x3, "yaw"));
        spinAxisData.get(2).setColor(Color.rgb(255, 184, 108));
        spinAxisData.get(2).setDrawCircles(false);

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

    protected void dataUpdate() {
        LineData chartData = chart.getData();
        long current = System.currentTimeMillis();
        if (prevUpdate1 == -1 || (current - prevUpdate1) >= 200) {
            numReps++;
            prevUpdate1 = current;

            // try to display text - not sure if this will work yet!
            text1 = pitch;
            text2 = roll;
            text3 = yaw;

            // THIS IS WHAT WAS WORKING IN THE LAST DEMO, do not touch
            if (srcIndex == 1) {
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * SAMPLING_PERIOD));
                chartData.addEntry(new Entry(pitch_data[0], sampleCount), 0);
                chartData.addEntry(new Entry(roll_data[0], sampleCount), 1);
                chartData.addEntry(new Entry(yaw_data[0], sampleCount), 2);
                chart.getData().notifyDataChanged();
                chart.notifyDataSetChanged();
                chartData.removeEntry(0, 1);
                chartData.removeEntry(0, 2);
                chartData.removeEntry(0, 3);

                moveViewToLast();
                sampleCount++;
            }
        }
    }
    private void moveViewToLast() {
        chart.setVisibleXRangeMinimum(100);
        chart.setVisibleXRangeMaximum(100);
        chart.moveViewToX(Math.max(0f, chartXValues.size() - 1));
    }

}
