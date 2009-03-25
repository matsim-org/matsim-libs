/* *********************************************************************** *
 * project: org.matsim.*
 * GroupedEvacuationPlansGeneratorAndNetworkTrimmer.java
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

package playground.gregor.groupedevac.controler;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sims.evacbase.EvacuationPlansGeneratorAndNetworkTrimmer;

public class GroupedEvacuationPlansGeneratorAndNetworkTrimmer extends EvacuationPlansGeneratorAndNetworkTrimmer {

	Logger log = Logger.getLogger(GroupedEvacuationPlansGeneratorAndNetworkTrimmer.class);
	
	//evacuation Nodes an Link
	private final static String saveLinkId = "el1";
	private final static String saveNodeAId = "en1";
	private final static String saveNodeBId = "en2";

	//	the positions of the evacuation nodes - for now hard coded
	// Since the real positions of this nodes not really matters
	// and for the moment we are going to evacuate Padang only,
	// the save nodes are located east of the city.
	// Doing so, the visualization of the resulting evacuation network is much clearer in respect of coinciding links.
	private final static String saveAX = "662433";
	private final static String saveAY = "9898853";
	private final static String saveBX = "662433";
	private final static String saveBY = "9898853";
	
	public void createEvacuationPlans(final Population plans, final NetworkLayer network) {
		PlansCalcRoute router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());

		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		this.log.info("  - removing all persons outside the evacuation area");
		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Activity)pers.getPlans().get(0).getPlanElements().get(0)).getLink().getId();

			if (network.getLink(id) == null) {
				it.remove();
			}
		}

		// the remaining persons plans will be routed
		this.log.info("  - generating evacuation plans for the remaining persons");
		it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			if (pers.getPlans().size() != 1 ) {
				Gbl.errorMsg("For each agent only one initial evacuation plan is allowed!");
			}

			Plan plan = pers.getPlans().get(0);

			if (plan.getPlanElements().size() != 1 ) {
				Gbl.errorMsg("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}

			Leg leg = new org.matsim.core.population.LegImpl(BasicLeg.Mode.car);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			plan.addLeg(leg);

			Activity actB = new org.matsim.core.population.ActivityImpl("h", new CoordImpl(12000.0, -12000.0), network.getLink(saveLinkId));
			plan.addAct(actB);

			router.run(plan);
			
			Link el = getEvacLink(leg);
			Leg eLeg = getELeg(leg);
			Activity actEvac = new org.matsim.core.population.ActivityImpl("h",el);
			
			plan.removeAct(2);
			plan.addLeg(eLeg);
			plan.addAct(actEvac);
		}
	}

	private Leg getELeg(final Leg leg) {
		List<Node> nodeRoute = ((NetworkRoute) leg.getRoute()).getNodes();
		nodeRoute.remove(nodeRoute.size()-1);
		Leg l = new org.matsim.core.population.LegImpl(BasicLeg.Mode.car);
		NetworkRoute route = new NodeNetworkRoute();
		route.setNodes(nodeRoute);
		l.setRoute(route);
		return l;
	}

	private Link getEvacLink(final Leg leg) {
		List<Link> lr = ((NetworkRoute) leg.getRoute()).getLinks();
		
		
		return lr.get(lr.size()-1);
	}

	
	
}
