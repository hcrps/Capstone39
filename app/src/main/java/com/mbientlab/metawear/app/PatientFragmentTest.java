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
import android.content.Intent;
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

/**
 * Created by kahlan on 01/24/2018.
 */

public class PatientFragmentTest extends SensorFragment {
    private static final float SAMPLING_PERIOD = 1 / 100f;

    private final ArrayList<Entry> x0 = new ArrayList<>(), x1 = new ArrayList<>(), x2 = new ArrayList<>(), x3 = new ArrayList<>();
    private SensorFusionBosch sensorFusion;
    private int srcIndex = 0;

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

    public PatientFragmentTest() {
        super(R.string.navigation_fragment_patient, R.layout.fragment_patientdatatest, -1f, 1f);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Spinner configuration
        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_sensor_fusion_data);

        Spinner fusionModeSelection = (Spinner) view.findViewById(R.id.config_option_spinner);
        fusionModeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                srcIndex = position;

                final YAxis leftAxis = chart.getAxisLeft();
                if (position == 0) {
                    // raw euler angles
                    leftAxis.setAxisMaxValue(360f);
                    leftAxis.setAxisMinValue(-360f);
                } else if (position == 1)  {
                    // filtered euler angles
                    leftAxis.setAxisMaxValue(360f);
                    leftAxis.setAxisMinValue(-360f);
                } else if (position == 2)  {
                    // TODO
                    leftAxis.setAxisMaxValue(360f);
                    leftAxis.setAxisMinValue(-360f);
                } else {
                    // TODO
                    leftAxis.setAxisMaxValue(360f);
                    leftAxis.setAxisMinValue(-360f);
                }

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_patient_data_test, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fusionModeSelection.setAdapter(spinnerAdapter);
    }

    @Override
    protected void setup() {
        sensorFusion.configure()
                .mode(Mode.NDOF)
                .accRange(AccRange.AR_16G)
                .gyroRange(GyroRange.GR_2000DPS)
                .commit();
        if (srcIndex == 0) {
            sensorFusion.eulerAngles().addRouteAsync(source -> source.stream((data, env) -> {
                LineData chartData = chart.getData();

                final EulerAngles angles = data.value(EulerAngles.class);
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * SAMPLING_PERIOD));
                chartData.addEntry(new Entry(angles.heading(), sampleCount), 0);
                chartData.addEntry(new Entry(angles.pitch(), sampleCount), 1);
                chartData.addEntry(new Entry(angles.roll(), sampleCount), 2);
                chartData.addEntry(new Entry(angles.yaw(), sampleCount), 3);

                sampleCount++;

                updateChart();
            })).continueWith(task -> {
                streamRoute = task.getResult();
                sensorFusion.eulerAngles().start();
                sensorFusion.start();

                return null;
            });
        } else if (srcIndex == 1){
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
        } else if (srcIndex == 2){

        } else {

        }
    }

    @Override
    protected void clean() {
        sensorFusion.stop();
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = String.format("time,heading,pitch,roll,yaw%n");
        String filename = String.format(Locale.US, "%s_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", getContext().getString(sensorResId), Calendar.getInstance());

        try {
            FileOutputStream fos = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());

            LineData data = chart.getLineData();
            LineDataSet x0DataSet = data.getDataSetByIndex(0), x1DataSet = data.getDataSetByIndex(1),
                    x2DataSet = data.getDataSetByIndex(2), x3DataSet = data.getDataSetByIndex(3);
            for (int i = 0; i < data.getXValCount(); i++) {
                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f,%.3f%n", (i * SAMPLING_PERIOD),
                        x0DataSet.getEntryForXIndex(i).getVal(), x1DataSet.getEntryForXIndex(i).getVal(),
                        x2DataSet.getEntryForXIndex(i).getVal(), x3DataSet.getEntryForXIndex(i).getVal()).getBytes());
            }
            fos.close();
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

}