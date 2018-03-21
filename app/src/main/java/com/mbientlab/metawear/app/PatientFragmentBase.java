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

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.module.Led;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.mbientlab.metawear.app.ProgressItem;

/**
 * Created by kahlan on 02/02/2018.
 */
public abstract class PatientFragmentBase extends ModuleFragmentBase {
    protected Led ledModule;
    protected final ArrayList<String> chartXValues= new ArrayList<>();
    protected LineChart chart;
    protected int sampleCount;
    protected long prevUpdate = -1;

    protected boolean isPeriodic = false;
    protected boolean motionError = false;
    protected boolean toofast = false;

    protected int numReps = 0;
    private TextView repsText;

    static int REP_DELAY = 50;
    private TextView pitchText;
    private TextView rollText;
    private TextView yawText;
    private TextView isPText;
    private TextView errorText;

    protected float min, max;
    protected Route streamRoute = null;

    private byte globalLayoutListenerCounter= 0;
    private final int layoutId;

    private final Handler chartHandler= new Handler();

    protected float text1, text2, text3, freqtext;
    protected double percentMotion;

    private CustomSeekBar freqseekbar;
    private float totalSpan = 500; // from 0 to 0.5 Hz
    private float slowspan = 50; // less than 0.10
    private float slowmidspan = 100;
    private float greenSpan = 200;
    private float fastmidspan = 100;
    private float fastspan;
    private ArrayList<ProgressItem> freqprogressItemList;
    private ProgressItem mProgressItem;

    private CustomSeekBar moveseekbar;
    private float movetotalSpan = 100;
    private float low = 50; // less than 0.10
    private float middle = 30;
    private float high;
    private ArrayList<ProgressItem> moveprogressItemList;

    RepetitiveDetector motion;


    protected PatientFragmentBase(int sensorResId, int layoutId, float min, float max) {
        super(sensorResId);
        this.layoutId= layoutId;
        this.min= min;
        this.max= max;
    }

    private void moveViewToLast() {
        chart.setVisibleXRangeMinimum(120);
        chart.setVisibleXRangeMaximum(120);
        chart.moveViewToX(Math.max(0f, chartXValues.size() - 1));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(layoutId, container, false);
        final View scrollView = v.findViewById(R.id.scrollView);
        if (scrollView != null) {
            globalLayoutListenerCounter= 1;
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    LineChart.LayoutParams params = chart.getLayoutParams();
                    params.height = scrollView.getHeight();
                    chart.setLayoutParams(params);

                    globalLayoutListenerCounter--;
                    if (globalLayoutListenerCounter < 0) {
                        scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // SEEKBAR
        freqseekbar = ((CustomSeekBar) view.findViewById(R.id.freqcustomSeekBar));
        moveseekbar = ((CustomSeekBar) view.findViewById(R.id.movecustomSeekBar));
        initDataToSeekbar();
        //

        chart = (LineChart) view.findViewById(R.id.data_chart);

        initializeChart();
        resetData(false);
        chart.invalidate();
        chart.setDescription(null);
        chart.setBackgroundColor(Color.rgb(68,71,90));
        chart.setDrawGridBackground(false);
        chart.getAxisLeft().setTextColor(Color.WHITE); // left y-axis
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getLegend().setTextColor(Color.WHITE);

        repsText = (TextView) view.findViewById(R.id.layout_one_text);
        repsText.setText(getString(R.string.label_reps, numReps));

        textUpdateHandler.post( new RptUpdater() );

        ((Switch) view.findViewById(R.id.sample_control)).setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                motion = new RepetitiveDetector();
                setup();
                if (ledModule != null){
                    ledModule.stop(true);
                }
            } else {
                if (ledModule != null){
                    ledModule.stop(true);
                }
                isPeriodic = false;
                motionError = false;
                toofast = false;
                percentMotion = 0;
                chart.setVisibleXRangeMinimum(120);
                chart.setVisibleXRangeMaximum(120);
                sampleCount = 0;
                numReps = 0;
                chart.getData().getDataSetByIndex(0).clear();
                chart.getData().getDataSetByIndex(1).clear();
                chartXValues.clear();
                clean();
                if (streamRoute != null) {
                    streamRoute.remove();
                    streamRoute = null;
                }
            }
        });
    }

    private void initDataToSeekbar() {
        freqprogressItemList = new ArrayList<ProgressItem>();
        // red span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = ((slowspan / totalSpan) * 100);
        mProgressItem.colour = Color.rgb(255,85,85);
        freqprogressItemList.add(mProgressItem);
        // blue span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (slowmidspan / totalSpan) * 100;
        mProgressItem.colour = Color.rgb(241,250,140);
        freqprogressItemList.add(mProgressItem);
        // green span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (greenSpan / totalSpan) * 100;
        mProgressItem.colour = Color.rgb(80,250,123);
        freqprogressItemList.add(mProgressItem);
        // yellow span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (fastmidspan / totalSpan) * 100;
        mProgressItem.colour = Color.rgb(241,250,140);
        freqprogressItemList.add(mProgressItem);
        // greyspan
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (fastspan / totalSpan) * 100;
        mProgressItem.colour = Color.rgb(255,85,85);
        freqprogressItemList.add(mProgressItem);

        freqseekbar.initData(freqprogressItemList);
        freqseekbar.invalidate();

        moveprogressItemList = new ArrayList<ProgressItem>();
        // red span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = ((low / movetotalSpan) * 100);
        mProgressItem.colour = Color.rgb(255,85,85);
        moveprogressItemList.add(mProgressItem);
        // blue span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (middle / movetotalSpan) * 100;
        mProgressItem.colour = Color.rgb(241,250,140);
        moveprogressItemList.add(mProgressItem);
        // green span
        mProgressItem = new ProgressItem();
        mProgressItem.progressItemPercentage = (high / movetotalSpan) * 100;
        mProgressItem.colour = Color.rgb(80,250,123);
        moveprogressItemList.add(mProgressItem);

        moveseekbar.initData(moveprogressItemList);
        moveseekbar.invalidate();
    }

    protected void refreshChart(boolean clearData) {
        if (ledModule != null){
            ledModule.stop(true);
        }
        chart.resetTracking();
        chart.clear();
        resetData(true);
        chart.invalidate();
        chart.fitScreen();
        chart.setVisibleXRangeMinimum(120);
        chart.setVisibleXRangeMaximum(120);
    }

    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setStartAtZero(false);
        leftAxis.setAxisMaxValue(max);
        leftAxis.setAxisMinValue(min);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);
    }

    protected abstract void setup();
    protected abstract void clean();
    protected abstract void resetData(boolean clearData);

    private Handler textUpdateHandler = new Handler();

    class RptUpdater implements Runnable {
        public void run() {
            updatetext();
            textUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY );
        }
    }
    public void updatetext(){
        repsText.setText(getString(R.string.label_reps, numReps));
        if (isPeriodic){
            freqseekbar.setProgress((int) (freqtext * 200.0));
            moveseekbar.setProgress((int) percentMotion);
//            if (motionError){
//                if (toofast) {
//                    errorText.setText(R.string.label_too_fast);
//                }
//                else {
//                    errorText.setText(R.string.label_too_slow);
//                }
//            }
//            else{
//                errorText.setText(R.string.label_no_error);
//            }
        }
        else{
//            errorText.setText(R.string.label_no_motion);
            freqseekbar.setProgress(0);
            moveseekbar.setProgress(0);
        }
    }

}
