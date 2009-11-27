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
public class InOutGraphWriter {
	
	private List<TTInOutflowEventHandler> ttmapList = new ArrayList<TTInOutflowEventHandler>();


	public void addInOutEventHandler(TTInOutflowEventHandler map) {
		this.ttmapList.add(map);
	}
	
	public void writeInOutChart(String basePath, int iterationNumber) {
		XYLineChart inoutflowChart = new XYLineChart("In and outflows", "time", "flow");
//		ttimeChart.
		double firstTimeStep = 0.0;
		double lastTimeStep = 0.0;
		//compute max range of tt
		for (TTInOutflowEventHandler handler : this.ttmapList){
			if (!handler.getInflowMap().isEmpty()) {
				if (firstTimeStep > handler.getInflowMap().firstKey())
					firstTimeStep = handler.getInflowMap().firstKey();
				if (lastTimeStep < handler.getInflowMap().lastKey())
					lastTimeStep = handler.getInflowMap().lastKey();				
			}
			if (!handler.getOutflowMap().isEmpty()) {
				if (firstTimeStep > handler.getOutflowMap().firstKey())
					firstTimeStep = handler.getOutflowMap().firstKey();
				if (lastTimeStep < handler.getOutflowMap().lastKey())
					lastTimeStep = handler.getOutflowMap().lastKey();				
			}
	  }
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);

		for (TTInOutflowEventHandler handler : this.ttmapList){
			if (handler.getOutflowMap().isEmpty())
				continue;

			double timeSteps[] = new double[numberTimeSteps];
			double inflow[] = new double[numberTimeSteps];
			double outflow[] = new double[numberTimeSteps];	
			
			double lastOut;
			double lastIn;
			
			if (firstTimeStep < handler.getInflowMap().firstKey()) {
				lastIn = 0.0;
			}
			else {
				lastIn = handler.getInflowMap().get(handler.getInflowMap().firstKey());	
			}
			//outflows
			if (firstTimeStep < handler.getOutflowMap().firstKey()) {
				lastOut = 0.0;
			}
			else {
				lastOut = handler.getOutflowMap().get(handler.getOutflowMap().firstKey());	
			}
			
			
			int index;
			Integer currentOut, currentIn;
			for (double i = firstTimeStep; i < lastTimeStep; i++) {
				index = (int) (i - firstTimeStep);
				timeSteps[index] = i;
				currentIn = handler.getInflowMap().get(i);
				currentOut = handler.getOutflowMap().get(i);
				
				if (currentIn != null) {
					lastIn = currentIn;
				}
				else {
					lastIn = 0.0;
				}
				if (currentOut != null) {
					lastOut = currentOut;
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
			inoutflowChart.addSeries("inflow on link " + handler.getLinkId(), timeSteps, inflow);
			inoutflowChart.addSeries("outflow on link " + handler.getLinkId(), timeSteps, outflow);
		}
		inoutflowChart.saveAsPng(basePath + "/" + iterationNumber + ".inoutflow.png", 600, 800);
		
		
	}
	
}
