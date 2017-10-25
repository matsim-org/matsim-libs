package playground.joel.data;

import java.awt.Color;
import java.io.File;
import java.util.NavigableMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.joel.helpers.KeyMap;

/** Created by Joel on 04.03.2017.
 * refactored clruch */
public class TimeDiagramCreator {

    /* package */ static void createDiagram(File directory, String fileTitle, String diagramTitle, NavigableMap<String, Double> map) throws Exception {
        final TimeSeries series = new TimeSeries("time series");
        for (String key : map.keySet()) {
            try {
                Second time = toTime(KeyMap.keyToTime(key));
                series.add(time, map.get(key));
                GlobalAssert.that(!series.isEmpty());
            } catch (SeriesException e) {
                System.err.println("Error adding to series");
            }
        }

        final XYDataset dataset = (XYDataset) new TimeSeriesCollection(series);
        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", "Value", dataset, false, false, false);
        timechart.getXYPlot().getRangeAxis().setRange(0, 1.1);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        int width = 800; /* Width of the image */
        int height = 600; /* Height of the image */
        File timeChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(timeChart, timechart, width, height);
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }

    /* package */ static void createDiagram(File directory, String fileTitle, String diagramTitle, NavigableMap<String, Double> map1,
            NavigableMap<String, Double> map2, NavigableMap<String, Double> map3, Double maxWait) throws Exception {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        final TimeSeries series1 = new TimeSeries("time series 1");
        final TimeSeries series2 = new TimeSeries("time series 2");
        final TimeSeries series3 = new TimeSeries("time series 3");

        GlobalAssert.that((map1.keySet().size() == map2.keySet().size()) && (map1.keySet().size() == map3.keySet().size()));
        for (String key : map1.keySet()) {
            try {
                Second time = toTime(KeyMap.keyToTime(key));

                series1.add(time, map1.get(key));
                GlobalAssert.that(!series1.isEmpty());
                series2.add(time, map2.get(key));
                GlobalAssert.that(!series2.isEmpty());
                series3.add(time, map3.get(key));
                GlobalAssert.that(!series3.isEmpty());

                dataset.addSeries(series1);
                dataset.addSeries(series2);
                dataset.addSeries(series3);
            } catch (SeriesException e) {
                System.err.println("Error adding to series");
            }
        }

        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", "Value", dataset, false, false, false);
        timechart.getXYPlot().getRangeAxis().setRange(0, maxWait);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        int width = 800; /* Width of the image */
        int height = 600; /* Height of the image */
        File timeChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(timeChart, timechart, width, height);
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }

    private static Second toTime(double time) {
        int days = (int) (time / 86400) + 1;
        int hours = (int) (time / 3600) - (days - 1) * 24;
        int minutes = (int) (time / 60) - hours * 60 - (days - 1) * 1440;
        int seconds = (int) time - minutes * 60 - hours * 3600 - (days - 1) * 86400;
        Second second = new Second(seconds, minutes, hours, days, 1, 2017); // month and year can not be zero
        return second;
    }
}
