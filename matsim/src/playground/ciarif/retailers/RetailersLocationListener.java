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
 *
 *
 * @author ciarif
 */

import java.util.Iterator;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.algorithms.WorldConnectLocations;
import org.matsim.router.*;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;


public class RetailersLocationListener implements IterationStartsListener, BeforeMobsimListener{

	private Retailers retailersToBeRelocated;
	private Retailers retailers;

	public RetailersLocationListener(final Retailers retailersToBeRelocated) {
		this.retailersToBeRelocated = retailersToBeRelocated;
	}

	RetailersSummaryWriter rs = new RetailersSummaryWriter("output/triangle/output_retailers.txt", this.retailers);

	public void notifyIterationStarts(final IterationStartsEvent event) {
		Controler controler = event.getControler();
		new WorldConnectLocations().run(Gbl.getWorld());
		Map<Id,Link> links = controler.getNetwork().getLinks();
		NewRetailersLocation nrl = new NewRetailersLocation(links);
		Facilities facilities = controler.getFacilities();
		this.retailers = new Retailers (facilities);
		this.retailersToBeRelocated = Retailers.selectRetailersForRelocation(this.retailers,40);
		Iterator<Facility> iter_fac = this.retailersToBeRelocated.getRetailers().values().iterator();

		while (iter_fac.hasNext()) {
			Facility f = iter_fac.next();
			Link link = nrl.findLocation();
			Coord coord = link.getCenter();
			f.moveTo(coord);
			//f.setLocation(coord);
			facilities.getFacilities().get(f.getId()).moveTo(coord);
			this.retailers.getRetailers().put(f.getId(),facilities.getFacilities().get(f.getId()));
		}
		this.rs.write(this.retailers);
		
		Iterator<Person> pers_iter = controler.getPopulation().getPersons().values().iterator();

		while (pers_iter.hasNext()) {
			
			Iterator<Plan> plan_iter = pers_iter.next().getPlans().iterator();

			while (plan_iter.hasNext()) {
					
				Iterator<Leg> leg_iter = plan_iter.next().getIteratorLeg();
			
				while (leg_iter.hasNext()) {
							
					leg_iter.next().setRoute(null);
				}
			}
		}
	}

	public void notifyBeforeMobsim (final BeforeMobsimEvent event) {
		
		Controler controler = event.getControler();
		Iterator<Person> pers_iter = controler.getPopulation().getPersons().values().iterator();

		while (pers_iter.hasNext()) {
			
			final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
			PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
			NetworkLayer network = controler.getNetwork();
			preprocess.run(network);
			PlansCalcRouteLandmarks pcrl = new PlansCalcRouteLandmarks(network, preprocess, timeCostCalc, timeCostCalc);
			pcrl.run(pers_iter.next());
			pcrl.run(pers_iter.next().getSelectedPlan());
			//PlansCalcRouteDijkstra pcrd = new PlansCalcRouteDijkstra(network, timeCostCalc, timeCostCalc);
			//pcrd.run(pers_iter.next());
			//pcrd.run(pers_iter.next().getSelectedPlan());
		}
	}	
			
}
