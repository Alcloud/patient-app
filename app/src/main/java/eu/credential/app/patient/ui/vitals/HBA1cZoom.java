package eu.credential.app.patient.ui.vitals;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.androidplot.util.SeriesUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.example.administrator.credential_v020.R;

public class HBA1cZoom extends AppCompatActivity {

    //Plot define
    private XYPlot plotZoom;
    // X&Y arrays
    private ArrayList<Double> seriesList = HBA1cFragment.AddHBA1cMeasurment.getSeries();
    private ArrayList<Date> domainList = HBA1cFragment.AddHBA1cMeasurment.getDomainLabels();
    private ArrayList<Double> newSeriesList = new ArrayList<>();
    private ArrayList<Date> newDomainList = new ArrayList<>();

    private enum SeriesPeriod {
        DAY,
        WEEK,
        MONTH
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hba1c_zoom);
        initUI();
    }

    private void initUI() {
        //Toolbar settings
        Toolbar toolbar = findViewById(R.id.toolbar_hba1c);
        toolbar.setTitle(R.string.diary_toolbar_hba1c);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.mipmap.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        //Plot initialisation
        plotZoom = findViewById(R.id.plotZoomHBA1c);
        Spinner spinnerPeriod = findViewById(R.id.spinner_period_hba1c);

        ArrayAdapter<SeriesPeriod> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SeriesPeriod.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Spinner settings
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setSelection(SeriesPeriod.DAY.ordinal());
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                final SeriesPeriod selectedSize = (SeriesPeriod) arg0.getSelectedItem();
                switch (selectedSize) {
                    case DAY:
                        newDomainList.clear();
                        newSeriesList.clear();
                        for (int i = 0; i < domainList.size(); i++) {
                            if (isToday(domainList.get(i)) == 0) {
                                newDomainList.add(domainList.get(i));
                                newSeriesList.add(seriesList.get(i));
                            }
                        }
                        setBoundaries(newDomainList, domainList);
                        break;

                    case WEEK:
                        newDomainList.clear();
                        newSeriesList.clear();
                        for (int i = 0; i < domainList.size(); i++) {
                            if (isWeek(domainList.get(i))) {
                                newDomainList.add(domainList.get(i));
                                newSeriesList.add(seriesList.get(i));
                            }
                        }
                        setBoundaries(newDomainList, domainList);
                        break;

                    case MONTH:
                        newDomainList.clear();
                        newSeriesList.clear();
                        for (int i = 0; i < domainList.size(); i++) {
                            if (isMonth(domainList.get(i))) {
                                newDomainList.add(domainList.get(i));
                                newSeriesList.add(seriesList.get(i));
                            }
                        }
                        setBoundaries(newDomainList, domainList);
                        break;

                    default:
                        break;
                }
                setGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void setBoundaries(ArrayList<Date> list, ArrayList<Date> domainList) {
        if (list.size() < 1) {
            plotZoom.setDomainBoundaries(domainList.size() - list.size(), BoundaryMode.FIXED, domainList.size(), BoundaryMode.AUTO);
            Toast.makeText(getApplicationContext(), R.string.toast_zoom, Toast.LENGTH_SHORT).show();
        } else if (list.size() == 1) {
            plotZoom.setRangeBoundaries(30, 150, BoundaryMode.FIXED);
            plotZoom.setRangeStep(StepMode.SUBDIVIDE, 5);
            plotZoom.setDomainBoundaries(0, 1, BoundaryMode.FIXED);
            plotZoom.setDomainStep(StepMode.SUBDIVIDE, 1);
            plotZoom.centerOnDomainOrigin(0, 1, BoundaryMode.FIXED);
            plotZoom.redraw();
        } else if (list.size() > 1 && list.size() < HBA1cFragment.getDomainBoundarySize()) {
            plotZoom.setDomainBoundaries(0, BoundaryMode.FIXED, list.size(), BoundaryMode.AUTO);
            //plotZoom.setRangeStep(StepMode.INCREMENT_BY_VAL, 5);
            plotZoom.setDomainStep(StepMode.SUBDIVIDE, list.size());
            plotZoom.redraw();
        } else {
            plotZoom.setDomainBoundaries(domainList.size() - list.size(), BoundaryMode.FIXED, domainList.size(), BoundaryMode.AUTO);
            plotZoom.setDomainStep(StepMode.SUBDIVIDE, list.size());
            plotZoom.redraw();
        }
    }

    private void setGraph() {
        // Remove all current series from each plot
        plotZoom.clear();

        //Plot settings
        plotZoom.getLegend().setVisible(false);
        LineAndPointFormatter series1Format = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels2);
        //LineAndPointFormatter goalFormat = new LineAndPointFormatter(this, R.xml.goal_line);
        /*series1Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));*/

        // Setup our Series with the selected number of elements
        XYSeries series1 = new SimpleXYSeries(newSeriesList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Glucose");
        //XYSeries goal = new SimpleXYSeries(newGoalList, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "goal");
        if (!newSeriesList.isEmpty()) {
            plotZoom.setRangeUpperBoundary(SeriesUtils.minMax(series1).
                    getMaxY().doubleValue() + 1, BoundaryMode.FIXED);
        }
        plotZoom.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                int i = Math.round(((Number) obj).longValue());
                return dateFormat.format(HBA1cFragment.AddHBA1cMeasurment.getDomainLabels().get(i), toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });
        // add a new series' to the xyplot:
        plotZoom.addSeries(series1, series1Format);
        //plotZoom.addSeries(goal, goalFormat);
        plotZoom.redraw();
    }

    private int isToday(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String dateString = format.format(date);
        String todayString = format.format(Calendar.getInstance().getTime());

        return dateString.compareTo(todayString);
    }

    private boolean isWeek(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        DateTimeFormatter parser1 = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate then = LocalDate.parse(format.format(date), parser1);

        return Days.daysBetween(then, LocalDate.now()).getValue(0) <= 7;
    }

    private boolean isMonth(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        DateTimeFormatter parser1 = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate then = LocalDate.parse(format.format(date), parser1);

        return Days.daysBetween(then, LocalDate.now()).getValue(0) <= 31;
    }

    public void onClickShare(View v) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "My HBA1c");
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }
}
