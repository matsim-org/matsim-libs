/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wdoering.grips.evacuationanalysis.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.spi.TimeZoneNameProvider;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.contrib.evacuation.model.config.ToolConfig;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

public class EvacuationTimeGraphPanel extends AbstractDataPanel {
	
	private ChartPanel chartPanel;


	//inherited field:
	//protected EventData data
	
	//TODO: GRAPH graph;
	
	public EvacuationTimeGraphPanel(int width, int height)
	{
		this.setPanelSize(width, height);
//		this.setBackground(new Color(80,140,220));
		
		drawDataPanel();
	}

	
	@Override
	public void drawDataPanel()
	{
		//if data is not set yet: do nothing
		if (data==null)
			return;
		
		//example usage of data
//		System.out.println("EVACUATION TIME GRAPH");
//		System.out.println("for event " + data.getEventName());
//		System.out.println("cell size:" + data.getCellSize());
//		System.out.println("time sum:" + data.getTimeSum());
//		System.out.println("arrivals:" + data.getArrivals());
		
		List<Tuple<Double,Integer>> arrivalTimes = data.getArrivalTimes(); //eine liste mit den ankunftzeiten
		int arrivalTimeCount = arrivalTimes.size(); //anzahl der elemente in der liste
		
		double [] xs = new double[arrivalTimeCount];
		double [] ys = new double[arrivalTimeCount];
		
		XYLineChart chart = new XYLineChart("evacuated persons", "time", "# evacuated persons");
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		TimeSeries timeSeries = new TimeSeries("evacuation time");
		
		
		
		for (int i = 0; i < arrivalTimeCount; i++)
		{
//			long timeValue = arrivalTimes.get(i).getFirst().longValue();
//			int hourInSeconds = 60*60;
//			int dayInSeconds = hourInSeconds*24;
//			
//			double timeInSeconds = arrivalTimes.get(i).getFirst();
//			int day = (int)(timeInSeconds / (dayInSeconds));
//			int hour = (int)((timeInSeconds % (dayInSeconds)) / (hourInSeconds));
//			int minute = (int)((timeInSeconds % (hourInSeconds)) / 60);
//			int second = (int)((timeInSeconds % 60));
			
//			xs[i] = hour*10000 + minute * 100 + second;
			xs[i] = 1000*60*60*23 + arrivalTimes.get(i).getFirst()*1000;
//			xs[i] = arrivalTimes.get(i).getFirst()*1000*60;
//			xs[i] = arrivalTimes.get(i).getFirst();
			ys[i] = arrivalTimes.get(i).getSecond();
			
//			System.out.println(day+":"+hour+":"+minute+":"+second);
			
//			TimeSeriesDataItem a = new TimeSeriesDataItem(new , arrivalTimes.get(i).getSecond());
			timeSeries.add(new Second(new Date((long)xs[i])), ys[i]);
		}
		
		dataset.addSeries(timeSeries);
//		chart.addSeries(data.getEventName(), xs, ys);
		
		JFreeChart freeChart = ChartFactory.createTimeSeriesChart("evacuation time", "time (hh:mm:ss)", "persons", dataset, false, false, false);
//		JFreeChart freeChart = chart.getChart();
		
		XYPlot plot = (XYPlot)freeChart.getPlot();
		((DateAxis)(plot.getDomainAxis())).setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
//		plot.getDomainAxis().setLabelFont(ToolConfig.FONT_DEFAULT_BOLD);
//		plot.getRangeAxis().setLabelFont(ToolConfig.FONT_DEFAULT_BOLD);
//		plot.getDomainAxis().setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 1, new SimpleDateFormat("HH:mm:ss")));
//		
//		DateAxis dateAxis = new DateAxis("time");
		
//		dateAxis.setTickUnit(new DateTickUnit(DateTickUnit.MINUTE, 1));
//		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
//		dateAxis.setDateFormatOverride(simpleDateFormat);
		
//		simpleDateFormat.
//		dateAxis.setDateFormatOverride();
		
//		plot.setDomainAxis(dateAxis);
		
//		plot.setDomainAxis(new NumberAxis("time (mm:ss)"));
////		plot.getDomainAxis().
//		
//		// create a custom tick unit collection...
//        final DecimalFormat formatter = new DecimalFormat("00:00:###");
//        
//        formatter.setNegativePrefix("(");
//        formatter.setNegativeSuffix(")");
//        final TickUnits standardUnits = new TickUnits();
//        
//        standardUnits.add(new NumberTickUnit(200, formatter));
//        standardUnits.add(new NumberTickUnit(500, formatter));
//        standardUnits.add(new NumberTickUnit(1000, formatter));
//        standardUnits.add(new NumberTickUnit(2000, formatter));
//        standardUnits.add(new NumberTickUnit(5000, formatter));
//        
//		plot.getDomainAxis().setStandardTickUnits(standardUnits);
		
//		((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("##:##:##"));
		
		
//		JFreeChart freeChart = ChartFactory.createXYLineChart("evacuation time", "time", "# evacuated persons", (TimeSeriesCollection) dataset, PlotOrientation.HORIZONTAL, true, true, false);
		
		
		
		freeChart.setAntiAlias(true);
		

//		XYPlot plot = (XYPlot)  freeChart.getPlot();
//		
//		 plot.setBackgroundPaint(Color.lightGray);
//		 plot.setDomainGridlinePaint(Color.white);
//		 plot.setRangeGridlinePaint(Color.white);
//		 plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
//		 plot.setDomainCrosshairVisible(true);
//		 plot.setRangeCrosshairVisible(true);
		
//        this.xaxis.setLabelFont(VisualInfo.standardFont);
//        this.xaxis.setTickLabelFont(VisualInfo.standardFont);
//        this.xaxis.setTickMarksVisible(true);
//        this.xaxis.setAutoRange(true);
//        this.xaxis.setFixedAutoRange(30000.0);
//        this.xaxis.setUpperMargin(.10);
//        this.xaxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
		
//		HistogramDataset histogram = new HistogramDataset();
//		
//		histogram.setType(HistogramType.SCALE_AREA_TO_1);
//        histogram.addSeries("Histogram",ys,uniqueArrivalTimes);
//		JFreeChart histogramChart = ChartFactory.createHistogram("123", "time", "arrivals", histogram, PlotOrientation.VERTICAL, false, false, false);
//		ChartPanel chartPanel = new ChartPanel(histogramChart);
		
		if (chartPanel!=null)
		{
			chartPanel.setChart(freeChart);
			chartPanel.repaint();
			return;
		}
		else
		{
			chartPanel = new ChartPanel(freeChart);
			chartPanel.setPreferredSize(new Dimension(this.width, this.height));
			
			this.add(chartPanel);
			this.validate();
			this.setSize(this.width,this.height);
		}
		
	}

}
