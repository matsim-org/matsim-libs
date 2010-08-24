/* *********************************************************************** *
 * project: org.matsim.*
 * CountsSimRealPerHourGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.crossborder.verification;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class TGZMCompare {

	private int[] xTripsPerHour ;
	private double[] aggregatedVolumePerHour ;
	private JFreeChart chart;

	public TGZMCompare(int[] xTripsPerHour, double[] aggregatedVolumePerHour){
		this.xTripsPerHour=xTripsPerHour;
		this.aggregatedVolumePerHour=aggregatedVolumePerHour;
	}
	

	public JFreeChart createChart(String actType) {

		XYSeriesCollection dataset0=new XYSeriesCollection();
		XYSeries series0=new XYSeries(actType+ " Trips MATSim");
		XYSeries series1=new XYSeries(actType+ " Trips TGZM");

        for (int i=0; i<24; i++) {
				double realVal = this.aggregatedVolumePerHour[i];
				int calcVal=this.xTripsPerHour[i];
				series0.add(i, calcVal);
				series1.add(i, realVal);				
        }
        dataset0.addSeries(series0);
        dataset0.addSeries(series1);

		String title="Compare TGZM and MATSim volumes per hour";
		this.chart = ChartFactory.createXYLineChart(
		title,
		"hour", // x axis label
		"Trips", // y axis label
		dataset0, // data
		PlotOrientation.VERTICAL,
		true, // include legend
		true, // tooltips
		false // urls
		);

		XYPlot plot=this.chart.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setLinesVisible(true);
		renderer.setSeriesPaint(0, Color.blue);
		renderer.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));		
		renderer.setSeriesPaint(1, Color.black);
		renderer.setSeriesShape(1, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
		
		plot.setRenderer(0, renderer);	

		return this.chart;
	}
}


