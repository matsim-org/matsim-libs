package playground.joel.data;

import java.io.*;
import java.util.NavigableMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.ChartUtilities;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Joel on 04.03.2017.
 */
public class TimeDiagramCreator {

    static double keyToTime(String key) {
        // extracts the first number/bin start from a key of the form "xxxxxx - xxxxxx"
        String asd[] = key.split(" ");
        return Double.parseDouble(asd[0]);
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, NavigableMap<String, Double> map)throws Exception
    {
        final TimeSeries series = new TimeSeries( "time series");
        for(String key: map.keySet()) {
            try
            {
                int hours = (int) (keyToTime(key)/3600);
                int minutes = (int) (keyToTime(key)/60) - hours*60;
                int seconds = (int) keyToTime(key) - minutes*60 - hours*3600;
                Second time = new Second(seconds, minutes, hours, 1, 1, 2017); // day, month and year can not be zero
                series.add(time, map.get(key));
                GlobalAssert.that(!series.isEmpty());
            }
            catch ( SeriesException e  )
            {
                System.err.println( "Error adding to series" );
            }
        }

        /* example from https://www.tutorialspoint.com/jfreechart/jfreechart_timeseries_chart.htm
        Second current = new Second();
        double value = 100.0;
        for ( int i = 0 ; i < 4000 ; i++ )
        {
            try
            {
                value = value + Math.random( ) - 0.5;
                series.add( current , new Double( value ) );
                current = ( Second ) current.next( );
            }
            catch ( SeriesException e )
            {
                System.err.println( "Error adding to series" );
            }
        }*/

        final XYDataset dataset=( XYDataset )new TimeSeriesCollection(series);
        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Seconds", "Value", dataset,false,false,false);

        int width = 800; /* Width of the image */
        int height = 600; /* Height of the image */
        File timeChart = new File( directory,fileTitle + ".png" );
        ChartUtilities.saveChartAsPNG( timeChart, timechart, width, height );
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }
}
