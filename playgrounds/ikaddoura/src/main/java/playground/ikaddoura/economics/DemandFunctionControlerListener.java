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
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author ikaddoura
 *
 */

public class DemandFunctionControlerListener implements ShutdownListener {
	
	private int demand = 0;

	public int getDemand() {
		return demand;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
        for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			
			Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
			
			if (leg.getMode().equals(TransportMode.car)) {
				demand++;
			}
		}
	}
}
