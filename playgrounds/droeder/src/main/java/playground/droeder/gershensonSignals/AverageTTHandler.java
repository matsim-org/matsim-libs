/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.gershensonSignals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.charts.XYLineChart;

import playground.dgrether.analysis.charts.DgAxisBuilder;
import playground.dgrether.analysis.charts.DgDefaultAxisBuilder;

/**
 * @author droeder
 *
 */
public class AverageTTHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentDepartureEventHandler{
	
	private static final Logger log = Logger.getLogger(AverageTTHandler.class);
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

	private double travelTime = 0.0;
	private double[] averageTime = new double[100];
	private double[] iteration = new double[100];
	
	private Map<Integer, Double> avTimeOverIteration;
	
	private XYSeriesCollection dataset = new XYSeriesCollection();;
	
	private int popSize;
	
	public AverageTTHandler(int popSize) {
		this.popSize = popSize;
	}
	
	private double getAverageTravelTime() {
		return this.travelTime / this.popSize;
	}
	
	public void reset(int iteration) {
		this.travelTime = 0.0;
		avTimeOverIteration = new HashMap<Integer, Double>();
	}
	
	public void setAverageIterationTime(int iteration){
		this.averageTime[iteration] = (getAverageTravelTime()/60);
		this.iteration[iteration] = (double) iteration;
		
		this.avTimeOverIteration.put(Integer.valueOf(iteration), Double.valueOf((getAverageTravelTime()/60)));
		log.error(Integer.valueOf(iteration)+ " " + avTimeOverIteration.get(Integer.valueOf(iteration)));
	}
	
	public void handleEvent(LinkEnterEvent event) {
		this.travelTime -= event.getTime();
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		this.travelTime += event.getTime();
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		this.travelTime += event.getTime();
	}
	
	public void handleEvent(AgentDepartureEvent event) {
		this.travelTime -= event.getTime();
	}
	public void writeChart(String filename) {
		
		XYLineChart chart = new XYLineChart("average travel times Denver", "iteration", "time [min]");
		chart.addSeries("adaptiv", this.iteration, this.averageTime);
		chart.saveAsPng(filename + ".png", 800, 600);
	}
	
	private XYSeriesCollection createDataSet(){
		 XYSeries series = new XYSeries("Series name", false, true);
		 for (Entry<Integer, Double> e: avTimeOverIteration.entrySet()){
			 series.add(e.getKey(), e.getValue());
		 }
		 this.dataset.addSeries(series);
		return dataset;
	}
	
	private XYSeriesCollection getDataSet(){
		if (this.dataset == null){
			createDataSet();
		}
		return dataset;
	}
	
	private JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		
		ValueAxis xAxis = this.axisBuilder.createValueAxis("Iteration");
		xAxis.setRange(0, 12);
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Time [min]");
		yAxis.setRange(0, 60);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
	    plot.setDataset(0, this.getDataSet());
	    JFreeChart chart = new JFreeChart("", plot);
	    chart.setBackgroundPaint(ChartColor.WHITE);
	    chart.setTextAntiAlias(true);
//	    chart.removeLegend();
		    
	    return chart;
	}
	
	public void writeChart2(String filename){
		JFreeChart jchart = this.createChart();
		filename += ".png";
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), jchart, 1200, 800, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}

