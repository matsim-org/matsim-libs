package playground.joel.data;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import playground.clruch.utils.GlobalAssert;
import playground.joel.helpers.KeyMap;

import java.awt.*;
import java.io.File;
import java.util.NavigableMap;

/**
 * Created by Joel on 04.03.2017.
 */
public class DiagramCreator {

    static Second toTime(double time) {
        int days = (int) (time/86400) + 1;
        int hours = (int) (time/3600) - (days - 1)*24;
        int minutes = (int) (time/60) - hours*60 - (days - 1)*1440;
        int seconds = (int) time - minutes*60 - hours*3600 - (days - 1)*86400;
        Second second = new Second(seconds, minutes, hours, days, 1, 2017); // month and year can not be zero
        return second;
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, NavigableMap<Long, Double> map) throws Exception
    {
        final TimeSeries series = new TimeSeries( "time series");
        for(Long key: map.keySet()) {
            try
            {
                Second time = toTime(key.doubleValue());
                series.add(time, map.get(key));
                GlobalAssert.that(!series.isEmpty());
            }
            catch ( SeriesException e  )
            {
                System.err.println( "Error adding to series" );
            }
        }


        final XYDataset dataset=( XYDataset )new TimeSeriesCollection(series);
        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", "Value", dataset,false,false,false);
        timechart.getXYPlot().getRangeAxis().setRange(0, 1.1);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        int width = 800; /* Width of the image */
        int height = 600; /* Height of the image */
        File timeChart = new File( directory,fileTitle + ".png" );
        ChartUtilities.saveChartAsPNG( timeChart, timechart, width, height );
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle,
                                     NavigableMap<Long, Double> map1, NavigableMap<Long, Double> map2, NavigableMap<Long, Double> map3, Double maxWait) throws Exception
    {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        final TimeSeries series1 = new TimeSeries( "time series 1");
        final TimeSeries series2 = new TimeSeries( "time series 2");
        final TimeSeries series3 = new TimeSeries( "time series 3");

        GlobalAssert.that((map1.keySet().size() == map2.keySet().size()) && (map1.keySet().size() == map3.keySet().size()));
        for(Long key: map1.keySet()) {
            try
            {
                Second time = toTime(key.doubleValue());

                series1.add(time, map1.get(key));
                GlobalAssert.that(!series1.isEmpty());
                series2.add(time, map2.get(key));
                GlobalAssert.that(!series2.isEmpty());
                series3.add(time, map3.get(key));
                GlobalAssert.that(!series3.isEmpty());

                dataset.addSeries(series1);
                dataset.addSeries(series2);
                dataset.addSeries(series3);
            }
            catch ( SeriesException e  )
            {
                System.err.println( "Error adding to series" );
            }
        }

        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", "Value", dataset,false,false,false);
        timechart.getXYPlot().getRangeAxis().setRange(0, maxWait);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        int width = 800; /* Width of the image */
        int height = 600; /* Height of the image */
        File timeChart = new File( directory,fileTitle + ".png" );
        ChartUtilities.saveChartAsPNG( timeChart, timechart, width, height );
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }
}
