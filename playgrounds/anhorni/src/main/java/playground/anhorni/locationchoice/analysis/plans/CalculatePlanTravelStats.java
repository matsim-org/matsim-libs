/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.plans;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;


public class CalculatePlanTravelStats implements StartupListener, IterationEndsListener, ShutdownListener {

	List<TravelStats> stats = new Vector<TravelStats>();

	public CalculatePlanTravelStats(final boolean createPNG) {	
		
		boolean wayThere[] = {false, true};
		
		for (int w = 0; w < wayThere.length; w++) {
		
			this.stats.add(new TravelStats(createPNG, new PlanLegsTravelDistanceCalculator(true), "distance", wayThere[w]));
			this.stats.add(new TravelStats(createPNG, new PlanLegsTravelDistanceCalculator(false), "distance", wayThere[w]));
			this.stats.add(new TravelStats(createPNG, new PlanLegsTravelTimeCalculator(), "time", wayThere[w]));
				
			String [] modes = {"car", "pt", "bike", "walk"};
			for (int j = 0; j < modes.length; j++) {
				
				PlanLegsTravelMeasureCalculator calculatorDistance = new PlanLegsTravelDistanceCalculator(true);
				calculatorDistance.setMode(modes[j]);
				this.stats.add(new TravelStats(createPNG, calculatorDistance, "distance", wayThere[w]));
				
				if (modes[j].equals("car")) {
					PlanLegsTravelMeasureCalculator calculatorDistanceRoute = new PlanLegsTravelDistanceCalculator(false);
					calculatorDistanceRoute.setMode("car");
					this.stats.add(new TravelStats(createPNG, calculatorDistanceRoute, "distance", wayThere[w]));
				}
		
				PlanLegsTravelMeasureCalculator calculatorTime = new PlanLegsTravelTimeCalculator();
				calculatorTime.setMode(modes[j]);
				this.stats.add(new TravelStats(createPNG, calculatorTime, "time", wayThere[w]));
				
				String [] actTypes = {"shop", "leisure", "work", "education"};			
				for (int i = 0; i < actTypes.length; i++) {				
					PlanLegsTravelMeasureCalculator calculatorDistanceActType = new PlanLegsTravelDistanceCalculator(true);
					calculatorDistanceActType.setMode(modes[j]);
					calculatorDistanceActType.setActType(actTypes[i]);
					
					this.stats.add(
							new TravelStats(
							createPNG, 
							calculatorDistanceActType,
							"distance", 
							wayThere[w]
							));
					
					if (modes[j].equals("car")) {
						PlanLegsTravelMeasureCalculator calculatorDistanceActTypeRoute = new PlanLegsTravelDistanceCalculator(false);
						calculatorDistanceActTypeRoute.setMode("car");
						calculatorDistanceActTypeRoute.setActType(actTypes[i]);
						
						this.stats.add(
								new TravelStats(
								createPNG, 
								calculatorDistanceActTypeRoute,
								"distance", 
								wayThere[w]
								));
					}
					
					PlanLegsTravelMeasureCalculator calculatorTimeActType = new PlanLegsTravelTimeCalculator();
					calculatorTimeActType.setMode(modes[j]);
					calculatorTimeActType.setActType(actTypes[i]);
					
					this.stats.add(
							new TravelStats(
							createPNG, 
							calculatorTimeActType,
							"time", 
							wayThere[w]
							));
				}	
			}	
		}
	}

	public void notifyStartup(final StartupEvent event) {
		Iterator<TravelStats> calculator_it = this.stats.iterator();
		while (calculator_it.hasNext()) {
			TravelStats calculator = calculator_it.next();
			calculator.notifyStartup(event);
		}	
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Iterator<TravelStats> calculator_it = this.stats.iterator();
		while (calculator_it.hasNext()) {
			TravelStats calculator = calculator_it.next();
			calculator.notifyIterationEnds(event);
		}
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
	}
}
