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
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
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
		NewRetailersLocation nrl = new NewRetailersLocation(links);
		Facilities facilities = controler.getFacilities();
		this.retailers = new Retailers (facilities);
		this.retailersToBeRelocated = Retailers.selectRetailersForRelocation(retailers,40);
		System.out.println("  rtbr = " + this.retailersToBeRelocated.getRetailers().values());
		Iterator<Facility> iter_fac = this.retailersToBeRelocated.getRetailers().values().iterator();
				
		while (iter_fac.hasNext()) {
			Facility f = iter_fac.next();
			Link link = nrl.findLocation();
			Coord coord = link.getCenter();
			//f.setLocation(coord);
			System.out.println("  Facility Link = " + f.getLink());
			System.out.println("  Facility Coord = " + f.getCenter());
			//facilities.getFacilities().get(f.getId()).setLocation(coord);//controler.getFacilities().g
			//facilities.getFacilities().get(f.getId()).addUpMapping(link);
		}
		System.out.println("  Controler facilities = " + facilities.getFacilities().values());
	}
			
	public void notifyBeforeMobsim (BeforeMobsimEvent event) {
		//TODO: Implement
		Controler controler = event.getControler();
		Iterator<Person> per_iter = controler.getPopulation().getPersons().values().iterator();
		
		while (per_iter.hasNext()) {
			Person person = per_iter.next();
			Plan plan = person.getSelectedPlan();
			//plan.
			//person.removePlan(plan);
		}
		//set routes to null of selected plans
		// router
	}

}
