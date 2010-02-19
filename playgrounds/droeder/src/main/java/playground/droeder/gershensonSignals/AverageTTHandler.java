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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * @author droeder
 *
 */
public class AverageTTHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
		AgentArrivalEventHandler, AgentDepartureEventHandler{
	
	private static final Logger log = Logger.getLogger(AverageTTHandler.class);

	private double travelTime = 0.0;
	private double[] averageTime = new double[100];
	private double[] iteration = new double[100];
	
	private int popSize;
	
	public AverageTTHandler(int popSize) {
		this.popSize = popSize;
	}
	
	private double getAverageTravelTime() {
		return this.travelTime / this.popSize;
	}
	
	public void reset(int iteration) {
		this.travelTime = 0.0;
	}
	
	public void setAverageIterationTime(int iteration){
		this.averageTime[iteration] = (getAverageTravelTime()/60);
		this.iteration[iteration] = (double) iteration;
		
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
}

