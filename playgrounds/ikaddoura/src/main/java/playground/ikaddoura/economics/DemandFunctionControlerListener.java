/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ikaddoura.economics;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * @author ikaddoura
 *
 */

public class DemandFunctionControlerListener implements IterationEndsListener {
	
	private int demand = 0;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			
			Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
			
			if (leg.getMode().equals(TransportMode.car)) {
				
				demand++;
			}
		}
	}

	public int getDemand() {
		return demand;
	}
	
}
