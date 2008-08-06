/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package playground.dgrether.cmcf;

import org.matsim.utils.charts.XYLineChart;


/**
 * @author dgrether
 *
 */
public class TTInOutflowGraphWriter {

	public static void writeTTChart(TTInOutflowEventHandler handler, String basePath) {
		XYLineChart ttimeChart = new XYLineChart("Travel times at time t on link " + handler.getLinkId(), "time", "travel time");
		double firstTimeStep = handler.getTravelTimesMap().firstKey();
		double lastTimeStep = handler.getTravelTimesMap().lastKey();
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);
		double timeSteps[] = new double[numberTimeSteps];
		double travelTimes[] = new double[numberTimeSteps];
		
		double lastTT = handler.getTravelTimesMap().get(handler.getTravelTimesMap().firstKey());
		
		for (int i = 0; i < numberTimeSteps; i++) {
			timeSteps[i] = i + firstTimeStep;
			Double tt = handler.getTravelTimesMap().get(i + firstTimeStep);
			if (tt != null) {
				travelTimes[i] = tt;
				lastTT = tt;
				System.out.println(i + firstTimeStep + " tt is " + tt + " eventEnter at: " + (i + firstTimeStep - tt));
			}
			else {
				travelTimes[i] = lastTT;
			}

		}
		ttimeChart.addSeries("travel times", timeSteps, travelTimes);
		ttimeChart.saveAsPng(basePath + "/" + "ttlink" + handler.getLinkId() + ".png", 600, 800);
	}
	
	
	public static void writeInOutFlowChart(TTInOutflowEventHandler handler, String basePath) {
		double firstInflowTimeStep = handler.getInflowMap().firstKey();
		double lastInflowTimeStep = handler.getInflowMap().lastKey();
		double firstOutflowTimeStep = handler.getOutflowMap().firstKey();
		double lastOutflowTimeStep = handler.getOutflowMap().lastKey();
		System.out.println("firstin: " + firstInflowTimeStep);
		System.out.println("firstout: " + firstOutflowTimeStep);
		System.out.println("lastin: " + lastInflowTimeStep);
		System.out.println("lastOut: " + lastOutflowTimeStep);
		double firstTimeStep = Math.min(firstInflowTimeStep, firstOutflowTimeStep);
		double lastTimeStep = Math.max(lastInflowTimeStep, lastOutflowTimeStep);
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);
		double timesteps[] = new double[numberTimeSteps];
		double inflow[] = new double[numberTimeSteps];
		double outflow[] = new double[numberTimeSteps];
		
		int index;
		Integer out, in;
		double lastOut = 0.0;
		double lastIn = 0.0;
		
		for (double i = firstTimeStep; i < lastTimeStep; i++) {
			index = (int) (i - firstTimeStep);
			timesteps[index] = i;
			in = handler.getInflowMap().get(i);
			out = handler.getOutflowMap().get(i);
			if (in != null) {
				lastIn = in;
			}
			else {
				lastIn = 0.0;
			}
			if (out != null) {
				lastOut = out;
			}
			else {
				lastOut = 0.0;
			}
			if (index != 0) {
				inflow[index] = inflow[index-1] + lastIn;
				outflow[index] = outflow[index-1] + lastOut;				
			}
			else {
				inflow[index] = lastIn;
				outflow[index] = lastOut;
			}
		}
		//writing in and outlflow
		XYLineChart inoutflowChart = new XYLineChart("In and outflows on link " + handler.getLinkId(), "time", "flow");
		inoutflowChart.addSeries("inflows", timesteps, inflow);
		inoutflowChart.addSeries("outflows", timesteps, outflow);
		inoutflowChart.saveAsPng(basePath + "/" + "inoutflowOfLink" + handler.getLinkId() +".png", 600, 800);
//		inoutflowChart2.saveAsPng(basePath + "/" + "inoutlink42.png", 600, 800);
	
	}	
	
}
