package util.charts;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

public class ChartUtil {
	
	public static XYDataset createTimeDataset(double[] timeValues, double[][] yValues, String[] names) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();
    	for(int y=0; y<yValues.length; y++) {
    		TimeSeries tS = new TimeSeries(names[y]);
    		for(int t=0; t<timeValues.length; t++) {
    			Date d=new Date((long) (timeValues[t]*1000));
    			tS.add(new Millisecond(d),yValues[y][t]);
    		}
    		dataset.addSeries(tS);
    	}
    	return dataset;
    }

    /**
     * Creates a sample chart.
     *
     * @param dataset the dataset.
     *
     * @return The chart.
     */
    public static JFreeChart createChart(XYDataset dataset, String title) {
    	JFreeChart chart = ChartFactory.createTimeSeriesChart(title,"Time(s)","Variable",dataset,true,true,false);
		chart.setBackgroundPaint(Color.white);
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("s"));
		return chart;
	}
}
