/* *********************************************************************** *
 * project: org.matsim.*
 * Plotter.java
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
package tryouts.multiagentsimulation.hw6;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author thomas
 *
 */
public class Plotter {

	/**
	 * @param args
	 */
	public static void main(String[] args) { // TODO: über welche Eingabedaten die Veränderungen am PT ermittelen (events, plans ...)???
		
//		String eventsFileBefore = "./tnicolai/configs/brandenburg/events_before.xml.gz";
//		String eventsFileAfter = "./tnicolai/configs/brandenburg/events_after.xml.gz";
//		EventsManager before = new EventsManagerImpl();
//		EventsManager after = new EventsManagerImpl();
//		
//		String plans = 
//		
//		XYLineChart chart = new XYLineChart("Traffic link 2", "iteration", "last time");
//		chart.addSeries("in", hours, linkLeaveEventHandler.getInGoingCounts());
//		chart.addSeries("out", hours, linkLeaveEventHandler.getOutGoingCounts());
//		chart.saveAsPng(scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it."+ scenario.getConfig().controler().getLastIteration() +"/chart.png", 1920, 1080);

	}
	
	public class Before implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
//			linkDeltas.get(event.getLinkId()).delta--;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class After implements LinkLeaveEventHandler {

		@Override
		public void handleEvent(LinkLeaveEvent event) {
//			linkDeltas.get(event.getLinkId()).delta++;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}

}

