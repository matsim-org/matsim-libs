/* *********************************************************************** *
 * project: org.matsim.*
 * EvacPlansAndNetworkGen.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.shelters;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.facilities.BasicOpeningTime.DayType;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.gregor.sims.evacbase.EvacuationPlansGeneratorAndNetworkTrimmer;

public class EvacPlansAndNetworkGen extends EvacuationPlansGeneratorAndNetworkTrimmer {
	private final String shelters = "../inputs/padang/network_v20080618/shelters.shp";
	Logger log = Logger.getLogger(EvacPlansAndNetworkGen.class);
	public void createEvacuationPlans(final Population plans, final NetworkLayer network) {
		
		
		setUpFacilities();
		
		PlansCalcRoute router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());
		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Activity)pers.getPlans().get(0).getPlanElements().get(0)).getLink().getId();

			if (network.getLink(id) == null) {
				it.remove();
			}
		}

		Facilities facs = (Facilities) Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		
		Facility fac = facs.getFacilities().get(new IdImpl("shelter0"));
		// the remaining persons plans will be routed
		for (Person person : plans.getPersons().values()) {
			if (person.getPlans().size() != 1 ) {
				throw new RuntimeException("For each agent only one initial evacuation plan is allowed!");
			}

			Plan plan = person.getPlans().get(0);

			if (plan.getPlanElements().size() != 1 ) {
				throw new RuntimeException("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}
			
			person.createKnowledge("evac location");
			Activity fact = plan.getFirstActivity();
			Facility f = facs.getFacilities().get(fact.getLinkId());
			fact.setFacility(f);
			ActivityOption a = f.getActivityOption("h");
			person.getKnowledge().addActivity(a, true);
			Leg leg = new org.matsim.core.population.LegImpl(TransportMode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			plan.addLeg(leg);
			Activity act = new org.matsim.core.population.ActivityImpl("evacuated",fac);
			person.getKnowledge().addActivity(fac.getActivityOption("evacuated"),false);
						
			
			act.setLink(fac.getLink());
			plan.addActivity(act);
			router.run(plan);
		}
	}
	private void setUpFacilities() {
		Facilities facs = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		NetworkLayer n = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		new SheltersReader(n,facs).read(this.shelters);
		for (Link link : n.getLinks().values()) {
			if (link.getId().toString().contains("shelter")) {
				continue;
			}
			Facility fac = facs.createFacility(new IdImpl(link.getId().toString()), link.getCoord());
			ActivityOption act = fac.createActivityOption("h");
			act.addOpeningTime(new OpeningTimeImpl(DayType.wk,0,3600*30));
		}
		new WorldConnectLocations().run(Gbl.getWorld());
	}


}
