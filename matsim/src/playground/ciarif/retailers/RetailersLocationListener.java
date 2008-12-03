/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
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

package playground.ciarif.retailers;

/**
 * Detect facilities' locations before to enter the main loop
 *
 * @author ciarif
 */
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class RetailersLocationListener implements IterationStartsListener, BeforeMobsimListener{

	
	public TreeMap<Id, NewRetailerLocation> newRetailersLocations;
	private EventsToFacilityRelocate eventsToFacilityRelocate;

	public RetailersLocationListener(TreeMap<Id, NewRetailerLocation> newRetailersLocations) {
		this.newRetailersLocations = newRetailersLocations;
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler controler = event.getControler();
		this.eventsToFacilityRelocate = new EventsToFacilityRelocate(controler.getFacilities(),
				this.newRetailersLocations);		// TODO Auto-generated method stub
	}
	
	public void notifyBeforeMobsim (BeforeMobsimEvent event) {
		//TODO: Implement
	}

}
