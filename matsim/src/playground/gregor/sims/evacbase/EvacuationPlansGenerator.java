/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGeneratorAndNetworkTrimmer.java
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

package playground.gregor.sims.evacbase;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.groups.EvacuationConfigGroup.Scenario;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 *@author glaemmel
 */
public class EvacuationPlansGenerator {

	private final static Logger log = Logger.getLogger(EvacuationPlansGenerator.class);

	
	private TravelCost tc = null;

	protected Network network;

	protected Population pop;

	private Link saveLink;

	public EvacuationPlansGenerator(Population pop, Network network, Link saveLink) {
		this.network = network;
		this.pop = pop;
		this.saveLink = saveLink;
	}
	
	/**
	 * Generates an evacuation plan for all agents inside the evacuation area.
	 * Agents outside the evacuation are will be removed from the plans.
	 *
	 * @param plans
	 * @param network
	 */
	public void run() {
		PlansCalcRoute router;
		if (this.tc != null) {
			router = new PlansCalcRoute(network, this.tc, new FreespeedTravelTimeCost());
		} else {
			router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());	
		}

		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		log.info("  - removing all persons outside the evacuation area");
		Iterator<Person> it = pop.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Activity)pers.getPlans().get(0).getPlanElements().get(0)).getLink().getId();

			if (network.getLink(id) == null) {
				it.remove();
			}
		}
		
		

		// the remaining persons plans will be routed
		log.info("  - generating evacuation plans for the remaining persons");
		EvacuationStartTimeCalculator c = getEndCalculatorTime();
		
		final Coord saveCoord = new CoordImpl(12000.0, -12000.0);
		for (Person person : this.pop.getPersons().values()) {
			if (person.getPlans().size() != 1 ) {
				throw new RuntimeException("For each agent only one initial evacuation plan is allowed!");
			}

			Plan plan = person.getPlans().get(0);

			if (plan.getPlanElements().size() != 1 ) {
				throw new RuntimeException("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}
			plan.getFirstActivity().setEndTime(c.getEvacuationStartTime(plan.getFirstActivity()));
			
			Leg leg = new org.matsim.core.population.LegImpl(TransportMode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			plan.addLeg(leg);

			plan.addActivity(new org.matsim.core.population.ActivityImpl("h", saveCoord, saveLink));

			router.run(plan);
		}

	}

	protected EvacuationStartTimeCalculator getEndCalculatorTime() {
			return new StaticEvacuationStartTimeCalculator(3*3600);
	}


	/**
	 * This method allows to set a travel cost calculator. If not set a free speed travel cost calculator
	 * will be instantiated automatically  
	 * @param tc
	 */
	public void setTravelCostCalculator(final TravelCost tc) {
		this.tc  = tc;
	}

}
