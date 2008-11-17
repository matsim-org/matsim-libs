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
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.evacuation.EvacuationPlansGeneratorAndNetworkTrimmer;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.population.RouteImpl;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.geometry.CoordImpl;

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
	
	@Override
	public void createEvacuationPlans(final Population plans, final NetworkLayer network) {
		PlansCalcRoute router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());

		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		this.log.info("  - removing all persons outside the evacuation area");
		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Act)pers.getPlans().get(0).getActsLegs().get(0)).getLink().getId();

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

			if (plan.getActsLegs().size() != 1 ) {
				Gbl.errorMsg("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}

			Leg leg = new Leg(BasicLeg.Mode.car);
			leg.setNum(0);
			leg.setDepartureTime(0.0);
			leg.setTravelTime(0.0);
			leg.setArrivalTime(0.0);
			plan.addLeg(leg);

			Act actB = new Act("h", new CoordImpl(12000.0, -12000.0), network.getLink(saveLinkId));
			plan.addAct(actB);

			router.run(plan);
			
			Link el = getEvacLink(leg);
			Leg eLeg = getELeg(leg);
			Act actEvac = new Act("h",el);
			
			plan.removeAct(2);
			plan.addLeg(eLeg);
			plan.addAct(actEvac);
		}
	}

	private Leg getELeg(final Leg leg) {
		List<Node> nodeRoute = leg.getRoute().getRoute();
		nodeRoute.remove(nodeRoute.size()-1);
		Leg l = new Leg(BasicLeg.Mode.car);
		Route route = new RouteImpl();
		route.setRoute(nodeRoute);
		l.setRoute(route);
		return l;
	}

	private Link getEvacLink(final Leg leg) {
		Link [] lr = leg.getRoute().getLinkRoute();
		
		
		return lr[lr.length-1];
	}

	
	
}
