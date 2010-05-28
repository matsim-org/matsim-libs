/* *********************************************************************** *
 * project: org.matsim.*
 * MyEventHandler2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package tryouts.examples;

import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * @author thomas
 *
 */
public class MyEventHandler3 implements LinkEnterEventHandler{
	
	private double[] volumeLink6;


	public MyEventHandler3() {
		reset(0);
	}

	public double getTravelTime(int slot) {
		return this.volumeLink6[slot];
	}
	
	private int getSlot(double time){
		return (int)time/3600;
	}

	public void reset(int iteration) {
		this.volumeLink6 = new double[24];
	}

	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(new IdImpl("6"))) {
			this.volumeLink6[getSlot(event.getTime())]++;
		}	
	}


	public void writeChart(String filename) {
		double[] hours = new double[24];
		for (double i = 0.0; i < 24.0; i++){
			hours[(int)i] = i;
		}
		XYLineChart chart = new XYLineChart("Traffic link 6", "hour", "departures");
		chart.addSeries("times", hours, this.volumeLink6);
		chart.saveAsPng(filename, 1920, 1080);
	}

}

