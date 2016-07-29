/* *********************************************************************** *
 * project: org.matsim.*
 * PersonPrepareForSim.java
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

package org.matsim.core.population.algorithms;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

/**
 * Performs several checks that persons are ready for a mobility simulation.
 * It is intended to run only once after the initial plans are read from file,
 * as we expect that no "damage" happens to the plans during the iterations.
 * <br/>
 * Currently, this only checks that in all plans the act's have a link assigned
 * and that all plans have valid routes, calculating missing links and
 * routes if required. Additionally, it will output a warning to the
 * log if a person has no plans at all.
 *
 * @author mrieser
 */
public class PersonPrepareForSim extends AbstractPersonAlgorithm {

	private final PlanAlgorithm router;
	private final XY2Links xy2links;
	private final Network network;

	private static final Logger log = Logger.getLogger(PersonPrepareForSim.class);

	/*
	 * To be used by the controller which creates multiple instances of this class which would
	 * create multiple copies of a car-only-network. Instead, we can create that network once in
	 * the Controller and re-use it for each new instance. cdobler, sep'15
	 */
	public PersonPrepareForSim(final PlanAlgorithm router, final Scenario scenario, final Network carOnlyNetwork) {
		super();
		this.router = router;
		this.network = scenario.getNetwork();
		if (NetworkUtils.isMultimodal(carOnlyNetwork)) {
			throw new RuntimeException("Expected carOnlyNetwork not to be multi-modal. Aborting!");
		}
		this.xy2links = new XY2Links(carOnlyNetwork, scenario.getActivityFacilities());
	}
	
	public PersonPrepareForSim(final PlanAlgorithm router, final Scenario scenario) {
		super();
		this.router = router;
		this.network = scenario.getNetwork();
		Network net = this.network;
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. XY2Links will only use car links.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			net = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		}
		this.xy2links = new XY2Links(net, scenario.getActivityFacilities());
	}

	@Override
	public void run(final Person person) {
		// first make sure we have a selected plan
		Plan selectedPlan = person.getSelectedPlan();
		if (selectedPlan == null) {
			// the only way no plan can be selected should be when the person has no plans at all
			log.warn("Person " + person.getId() + " has no plans!");
			return;
		}

		// make sure all the plans have valid act-locations and valid routes
		for (Plan plan : person.getPlans()) {
			boolean needsXY2Links = false;
			boolean needsReRoute = false;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.getLinkId() == null) {
						needsXY2Links = true;
						needsReRoute = true;
						break;
					}
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute() == null) {
						needsReRoute = true;
					}
					else if (Double.isNaN(leg.getRoute().getDistance())){
						Double dist = null;
						if (leg.getRoute() instanceof NetworkRoute){
							/* So far, 1.0 is always used as relative position on start and end link. 
							 * This means that the end link is considered in route distance and the start link not.
							 * tt feb'16
							 */
							double relativePositionStartLink = 1.0;
							double relativePositionEndLink  = 1.0;
							dist = RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), relativePositionStartLink, relativePositionEndLink, this.network);
						}
						if (dist != null){
							leg.getRoute().setDistance(dist);
						}
					}
				}
			}
			if (needsXY2Links) {
				this.xy2links.run(plan);
			}
			if (needsReRoute) {
				this.router.run(plan);
			}
		}

	}

}
