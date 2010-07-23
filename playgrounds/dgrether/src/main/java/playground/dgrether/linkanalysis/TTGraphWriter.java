/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.linkanalysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.charts.XYLineChart;



/**
 * @author dgrether
 *
 */
public class TTGraphWriter {
	
	private List<TTInOutflowEventHandler> ttmapList = new ArrayList<TTInOutflowEventHandler>();
	
	public TTGraphWriter() {
		
	}
	
	public void addTTEventHandler(TTInOutflowEventHandler map) {
		this.ttmapList.add(map);
	}
	
	
	
	
	
	public void writeTTChart(String basePath, int iterationNumber) {
		XYLineChart ttimeChart = new XYLineChart("Travel times at time t", "time", "travel time");
//		ttimeChart.
		double firstTimeStep = 0.0;
		double lastTimeStep = 0.0;
		//compute max range of tt
		for (TTInOutflowEventHandler handler : this.ttmapList){
			if (!handler.getTravelTimesMap().isEmpty()) {
				if (firstTimeStep > handler.getTravelTimesMap().firstKey())
					firstTimeStep = handler.getTravelTimesMap().firstKey();
				if (lastTimeStep < handler.getTravelTimesMap().lastKey())
					lastTimeStep = handler.getTravelTimesMap().lastKey();				
			}
	  }
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);

		//write chart data
		for (TTInOutflowEventHandler handler : this.ttmapList){
			if (handler.getTravelTimesMap().isEmpty())
				continue;

			double timeSteps[] = new double[numberTimeSteps];
			double travelTimes[] = new double[numberTimeSteps];			
			double lastTT;
			
			if (firstTimeStep < handler.getTravelTimesMap().firstKey()) {
				lastTT = 0.0;
			}
			else {
				lastTT = handler.getTravelTimesMap().get(handler.getTravelTimesMap().firstKey());	
			}
			
			for (int i = 0; i < numberTimeSteps; i++) {
				timeSteps[i] = i + firstTimeStep;
				Double tt = handler.getTravelTimesMap().get(i + firstTimeStep);
				if (tt != null) {
					travelTimes[i] = tt;
					lastTT = tt;
				}
				else {
					travelTimes[i] = lastTT;
				}
			}
			ttimeChart.addSeries("travel times on link " + handler.getLinkId(), timeSteps, travelTimes);
		}
		ttimeChart.saveAsPng(basePath + "/" + iterationNumber + ".ttlinks.png", 600, 800);
		
		
	}
}
