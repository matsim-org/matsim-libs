/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOccupancyCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package occupancy;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

public class FacilitiesOccupancyCalculator implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private EventsToFacilityOccupancy eventsToFacilityOccupancy = null;
	final TreeMap<Id, FacilityOccupancy> facilityOccupancies;

	//--------------------------------------------------------------------------------------------------

	public FacilitiesOccupancyCalculator(TreeMap<Id, FacilityOccupancy> facilityOccupancies) {
		this.facilityOccupancies = facilityOccupancies;
	}


	@Override
	public void notifyStartup(final StartupEvent event) {
		this.eventsToFacilityOccupancy = new EventsToFacilityOccupancy();
		event.getControler().getEvents().addHandler(this.eventsToFacilityOccupancy);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.eventsToFacilityOccupancy.reset(event.getIteration());
	}



	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
	}

}