package com.mbientlab.metawear.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.Switch;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import bolts.Task;

/**
 * Created by Kahlan on 1/11/18.
 */

public class CapstoneSensorDebugFragment extends SensorFragment {
    private Led ledModule;
    private int switchRouteId = -1;

    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }
    private static final float SAMPLING_PERIOD = 1/100f;

    private final ArrayList<Entry> x0 = new ArrayList<>(), x1 = new ArrayList<>(), x2 = new ArrayList<>(), x3 = new ArrayList<>();
    private SensorFusionBosch sensorFusion;
    private int srcIndex = 0;

    public CapstoneSensorDebugFragment() {
        //super(R.string.navigation_fragment_sensor_fusion, R.layout.fragment_sensor_config_spinner, -1f, 1f);
        super(R.string.navigation_fragment_sensor_fusion, R.layout.fragment_capstone, -1f, 1f);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.led_red).setOnClickListener(view1 -> {
            configureChannel(ledModule.editPattern(Led.Color.RED));
            ledModule.play();
        });
        view.findViewById(R.id.led_green).setOnClickListener(view12 -> {
            configureChannel(ledModule.editPattern(Led.Color.GREEN));
            ledModule.play();
        });
        view.findViewById(R.id.led_blue).setOnClickListener(view13 -> {
            configureChannel(ledModule.editPattern(Led.Color.BLUE));
            ledModule.play();
        });
        view.findViewById(R.id.led_stop).setOnClickListener(view14 -> ledModule.stop(true));
        /*view.findViewById(R.id.board_rssi_text).setOnClickListener(v -> mwBoard.readRssiAsync()
                .continueWith(task -> {
                    ((TextView) view.findViewById(R.id.board_rssi_value)).setText(String.format(Locale.US, "%d dBm", task.getResult()));
                    return null;
                }, Task.UI_THREAD_EXECUTOR)
        );*/

        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_sensor_fusion_data);

        Spinner fusionModeSelection = (Spinner) view.findViewById(R.id.config_option_spinner);
        fusionModeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                srcIndex = position;

                final YAxis leftAxis = chart.getAxisLeft();
                if (position == 0) {
                    leftAxis.setAxisMaxValue(1.f);
                    leftAxis.setAxisMinValue(-1.f);
                } else {
                    leftAxis.setAxisMaxValue(360f);
                    leftAxis.setAxisMinValue(-360f);
                }

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getContext(), R.array.values_fusion_data, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fusionModeSelection.setAdapter(spinnerAdapter);
    }

    @Override
    protected void setup() {
        sensorFusion.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();

        if (srcIndex == 0) {
            sensorFusion.quaternion().addRouteAsync(source -> source.stream((data, env) -> {
                LineData chartData = chart.getData();

                final Quaternion quaternion = data.value(Quaternion.class);
                chartData.addXValue(String.format(Locale.US, "%.2f", sampleCount * SAMPLING_PERIOD));
                chartData.addEntry(new Entry(quaternion.w(), sampleCount), 0);
                chartData.addEntry(new Entry(quaternion.x(), sampleCount), 1);
                chartData.addEntry(new Entry(quaternion.y(), sampleCount), 2);
                chartData.addEntry(new Entry(quaternion.z(), sampleCount), 3);

                sampleCount++;

                updateChart();
            })).continueWith(task -> {
                streamRoute = task.getResult();
                sensorFusion.quaternion().start();
                sensorFusion.start();

                return null;
            });
        } else {
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
        }
    }

    @Override
    protected void clean() {
        sensorFusion.stop();
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = (srcIndex == 0 ? String.format("time,w,x,y,z%n") : String.format("time,heading,pitch,roll,yaw%n"));
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

        ArrayList<LineDataSet> spinAxisData= new ArrayList<>();
        spinAxisData.add(new LineDataSet(x0, srcIndex == 0 ? "w" : "heading"));
        spinAxisData.get(0).setColor(Color.BLACK);
        spinAxisData.get(0).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x1, srcIndex == 0 ? "x" : "pitch"));
        spinAxisData.get(1).setColor(Color.RED);
        spinAxisData.get(1).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x2, srcIndex == 0 ? "y" : "roll"));
        spinAxisData.get(2).setColor(Color.GREEN);
        spinAxisData.get(2).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(x3, srcIndex == 0 ? "z" : "yaw"));
        spinAxisData.get(3).setColor(Color.BLUE);
        spinAxisData.get(3).setDrawCircles(false);

        LineData data= new LineData(chartXValues);
        for(LineDataSet set: spinAxisData) {
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

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.PatternEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.highIntensity((byte) 31).lowIntensity((byte) 31)
                .highTime((short) (PULSE_WIDTH >> 1)).pulseDuration(PULSE_WIDTH)
                .repeatCount((byte) -1).commit();
    }
    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG= "metaboot_warning_tag";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new CapstoneSensorDebugFragment.MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning= (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }

        Switch switchModule;
        if ((switchModule= mwBoard.getModule(Switch.class)) != null) {
            Route oldSwitchRoute;
            if ((oldSwitchRoute = mwBoard.lookupRoute(switchRouteId)) != null) {
                oldSwitchRoute.remove();
            }

            switchModule.state().addRouteAsync(source ->
                    source.stream((data, env) -> getActivity().runOnUiThread(() -> {
                        RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.switch_radio);

                        if (data.value(Boolean.class)) {
                            radioGroup.check(R.id.switch_pressed);
                            v.findViewById(R.id.switch_pressed).setEnabled(true);
                            v.findViewById(R.id.switch_released).setEnabled(false);
                        } else {
                            radioGroup.check(R.id.switch_released);
                            v.findViewById(R.id.switch_released).setEnabled(true);
                            v.findViewById(R.id.switch_pressed).setEnabled(false);
                        }
                    }))
            ).continueWith(task -> switchRouteId = task.getResult().id());
        }

        int[] ledResIds= new int[] {R.id.led_stop, R.id.led_red, R.id.led_green, R.id.led_blue};
        if ((ledModule = mwBoard.getModule(Led.class)) != null) {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } else {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
