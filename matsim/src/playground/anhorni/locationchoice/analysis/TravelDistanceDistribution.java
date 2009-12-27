/* *********************************************************************** *
 * project: org.matsim.*
 * TravelStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.preprocess.plans.modifications.DistanceBins;

/**
 * @author anhorni
 */

public class TravelDistanceDistribution implements StartupListener, IterationEndsListener, ShutdownListener {

	private PopulationImpl population;	
	private DistanceBins shopDistanceBins;
	private DistanceBins leisureDistanceBins;

	public void notifyStartup(final StartupEvent event) {
		this.population = (PopulationImpl) event.getControler().getPopulation();
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		
		shopDistanceBins = new DistanceBins(1000.0, 100.0 * 1000.0, "car");
		leisureDistanceBins = new DistanceBins(1000.0, 100.0 * 1000.0, "car");
				
		for (Person person : this.population.getPersons().values()) {
			PlanImpl selectedPlan = (PlanImpl) person.getSelectedPlan();
		
			final List<?> actslegs = selectedPlan.getPlanElements();	
			for (int j = 1; j < actslegs.size(); j=j+2) {			
				if (actslegs.get(j) instanceof LegImpl) {
					LegImpl leg = (LegImpl) actslegs.get(j);
					ActivityImpl act = (ActivityImpl)actslegs.get(j+1);
					
					if (!leg.getMode().toString().equals("car")) {
						continue;
					}
					
					double dist = ((CoordImpl)act.getCoord()).calcDistance(((ActivityImpl)actslegs.get(j-1)).getCoord()) / 1000.0;
					
					// act type
					String actType = act.getType();
					if (actType.startsWith("shop")) {
						shopDistanceBins.addDistance(dist);
					}
					else if (actType.startsWith("leisure")) {
						leisureDistanceBins.addDistance(dist);
					}
				}	
			}				
		}
		this.shopDistanceBins.plotDistribution(event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(), "shopDistanceDistributions"), "");
		this.leisureDistanceBins.plotDistribution(event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(), "leisureDistanceDistributions"), "");
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
	}
}
