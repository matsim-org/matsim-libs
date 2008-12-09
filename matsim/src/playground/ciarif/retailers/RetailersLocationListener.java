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
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.utils.geometry.Coord;
/*import org.matsim.interfaces.basic.v01.BasicLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.log4j.Logger;//import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;
import org.matsim.world.Location;*/


public class RetailersLocationListener implements IterationStartsListener, BeforeMobsimListener{

	private Retailers retailersToBeRelocated;
	private Retailers retailers;

	public RetailersLocationListener(Retailers retailersToBeRelocated) {
		this.retailersToBeRelocated = retailersToBeRelocated;
	}

	public RetailersLocationListener(
			TreeMap<Id, Facility> retailersToBeRelocated) {
		// TODO Auto-generated constructor stub
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler controler = event.getControler();
		Map<Id,Link> links = controler.getNetwork().getLinks();
		Facilities facilities = controler.getFacilities();
		this.retailers = new Retailers (facilities);
		this.retailersToBeRelocated = Retailers.selectRetailersForRelocation(retailers);
		System.out.println("  rtbr = " + retailersToBeRelocated.getRetailers());
		
		Iterator<Facility> iter_fac = this.retailersToBeRelocated.getRetailers().values().iterator();
				
		while (iter_fac.hasNext()) {
			Facility f = iter_fac.next();
			int key = newRetailersLocation (links);//Location location = facilities.getLocation(f.getId());
			
			System.out.println("  Links = " + links);
			System.out.println("  Facility Link = " + f.getLink());
			System.out.println("  Facility Coord = " + f.getCenter());
			System.out.println("  Link key = " + key);
			IdImpl id = new IdImpl(110);
			Link l = links.get(id);
			System.out.println("  Link = " + l);
			Coord coord = links.get(id).getCenter();
			System.out.println("  Link Coord = " + coord);
			//f.setLocation(links.get(id).getCenter());
			links.remove(id);
			System.out.println("  Facility Link = " + f.getLink());
			System.out.println("  Facility Coord = " + f.getCenter());
		}
	}
			
	// Might it be a separate class instead of a method? For example 
	// a class where more different methods would implement different
	// ways (algorithms) to choose the new locations.
	private int newRetailersLocation (Map<Id,Link> links){
		int rd = MatsimRandom.random.nextInt(links.size()-1);
		return rd;
	}
	
	public void notifyBeforeMobsim (BeforeMobsimEvent event) {
		//TODO: Implement
		//set routes to null of selected plans
		// router
	}

}
