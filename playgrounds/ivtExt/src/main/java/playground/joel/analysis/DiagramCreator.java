package playground.joel.analysis;

import java.awt.Color;
import java.io.File;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Joel on 04.03.2017.
 */
public class DiagramCreator {

    static Second toTime(double time) {
        int days = (int) (time / 86400) + 1;
        int hours = (int) (time / 3600) - (days - 1) * 24;
        int minutes = (int) (time / 60) - hours * 60 - (days - 1) * 1440;
        int seconds = (int) time - minutes * 60 - hours * 3600 - (days - 1) * 86400;
        Second second = new Second(seconds, minutes, hours, days, 1, 2017); // month and year can not be zero
        return second;
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, Tensor time, Tensor values) throws Exception {
        createDiagram(directory, fileTitle, diagramTitle, time, values, 1.1);
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, Tensor time, Tensor values, Double maxRange) throws Exception {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int i = 0; i < values.length(); i++) {
            final TimeSeries series = new TimeSeries("time series " + i);
            for (int j = 0; j < time.length(); j++) {
                Second daytime = toTime(time.Get(j).number().doubleValue());

                series.add(daytime, values.Get(i, j).number().doubleValue());

            }
            dataset.addSeries(series);
        }

        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", "Value", dataset, false, false, false);
        timechart.getXYPlot().getRangeAxis().setRange(0, maxRange);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        int width = 1200; /* Width of the image */
        int height = 900; /* Height of the image */
        File timeChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(timeChart, timechart, width, height);
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }
}
