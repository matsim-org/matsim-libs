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

package playground.wdoering.grips.evacuationanalysis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

public class EvacuationTimeGraphPanel extends AbstractGraphPanel {
	
	//inherited field:
	//protected EventData data
	
	//TODO: GRAPH graph;
	
	public EvacuationTimeGraphPanel(int width, int height)
	{
		this.setGraphSize(width, height);
//		this.setBackground(new Color(80,140,220));
		drawGraph();
	}

	
	@Override
	public void drawGraph()
	{
		//if data is not set yet: do nothing
		if (data==null)
			return;
		
		//example usage of data
		System.out.println("EVACUATION TIME GRAPH");
		System.out.println("cell size:" + data.getCellSize());
		System.out.println("time sum:" + data.getTimeSum());
		System.out.println("arrivals:" + data.getArrivals());
		
		List<Tuple<Double,Integer>> arrivalTimes = data.getArrivalTimes(); //eine liste mit den ankunftzeiten
		int arrivalTimeCount = arrivalTimes.size(); //anzahl der elemente in der liste
		
		double [] xs = new double[arrivalTimeCount];
		double [] ys = new double[arrivalTimeCount];
		
		XYLineChart chart = new XYLineChart("Evakuierungszeit", "x", "y");
		for (int i = 0; i < arrivalTimeCount; i++)
		{
			xs[i] = arrivalTimes.get(i).getFirst();
			ys[i] = arrivalTimes.get(i).getSecond();
		}		
		
		chart.addSeries("123", xs, ys);
		
		JFreeChart freeChart = chart.getChart();
//		HistogramDataset histogram = new HistogramDataset();
//		
//		histogram.setType(HistogramType.SCALE_AREA_TO_1);
//        histogram.addSeries("Histogram",ys,uniqueArrivalTimes);
//		JFreeChart histogramChart = ChartFactory.createHistogram("123", "time", "arrivals", histogram, PlotOrientation.VERTICAL, false, false, false);
//		ChartPanel chartPanel = new ChartPanel(histogramChart);
		ChartPanel chartPanel = new ChartPanel(freeChart);
		
		chartPanel.setPreferredSize(new Dimension(this.width, this.height));
		
		this.add(chartPanel);
		this.validate();
		this.setSize(this.width,this.height);
		
	}

}

class TimeArrivals
{
	private double time;
	private int arrivals;
	
	public TimeArrivals(double time, int arrivals)
	{
		this.time = time;
		this.arrivals = arrivals;
	}
	
	public double getTime() {
		return time;
	}
	
	public int getArrivals() {
		return arrivals;
	}
	
}
