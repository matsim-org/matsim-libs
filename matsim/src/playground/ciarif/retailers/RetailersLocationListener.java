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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.BasicLocation;
//import org.matsim.gbl.Gbl;
//import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
//import org.matsim.locationchoice.facilityload.FacilityPenalty;
//import org.matsim.utils.io.IOUtils;
//import org.matsim.world.algorithms.WorldBottom2TopCompletion;
//import org.matsim.world.algorithms.WorldCheck;
//import org.matsim.world.algorithms.WorldValidation;
import org.matsim.world.Location;
import org.matsim.network.Link;

public class RetailersLocationListener implements IterationStartsListener, BeforeMobsimListener{

	
	private TreeMap<Id, Facility> retailersToBeRelocated;

	public RetailersLocationListener(TreeMap<Id, Facility> retailersToBeRelocated) {
		this.retailersToBeRelocated = retailersToBeRelocated;
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler controler = event.getControler();
		Facilities facilities = controler.getFacilities();
		Iterator<Facility> iter_fac = this.retailersToBeRelocated.values().iterator();
		while (iter_fac.hasNext()) {
			Facility f = iter_fac.next();
			Map<Id,Link> links = controler.getNetwork().getLinks();
			Link link = newRetailersLocation (links);//Location location = facilities.getLocation(f.getId());
	
		}
	}
			
	// Might it be a separate class instead of a method? For example 
	// a class where more different methods would implement different
	// ways (algorithms) to choose the new locations.
	
	Link newRetailersLocation (Map<Id,Link> links){
		int rd = MatsimRandom.random.nextInt(links.size()-1);
		Link link = links.get(rd);
		return link;
	}
	
	public void notifyBeforeMobsim (BeforeMobsimEvent event) {
		//TODO: Implement
		//set routes to null of selected plans
		// router
	}

}
