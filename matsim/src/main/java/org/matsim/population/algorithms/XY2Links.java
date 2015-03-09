/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
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

package org.matsim.population.algorithms;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.List;

/**
 * Assigns each activity in a plan a link where the activity takes place
 * based on the coordinates given for the activity.
 *
 * @author mrieser
 */
public class XY2Links extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final NetworkImpl network;
	private final ActivityFacilities facilities;
	private final boolean hasFacilities;

	/**
	 * When running XY2Links with given facilities, the linkIds of activities are
	 * taken from the facilities where they are performed. This ensures that the
	 * activity is performed at the same link where the facility is located. 
	 */
	public XY2Links(final Network network, final ActivityFacilities facilities) {
		super();
		this.network = (NetworkImpl) network;
		this.facilities = facilities;
		hasFacilities = (facilities != null);
	}

	public XY2Links(final Scenario scenario) {
		this(scenario.getNetwork(), scenario.getActivityFacilities());
	}
	
	@Deprecated
	public XY2Links(final Network network) {
		this(network, null);
	}

	/** Assigns links to each activity in all plans of the person. */
	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			processPlan(plan);
		}
	}

	/** Assigns links to each activity in the plan. */
	@Override
	public void run(final Plan plan) {
		processPlan(plan);
	}

	private void processPlan(final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		for (PlanElement planElement : planElements) {
			if (planElement instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) planElement;
				
				if (hasFacilities) {
					if (act.getFacilityId() != null) {
						ActivityFacility facility = facilities.getFacilities().get(act.getFacilityId()); 
						if (facility != null) act.setLinkId(facility.getLinkId());
						if (act.getLinkId() != null) continue; // (*)
					}
				}
				
				if ( act.getLinkId() != null ) {
					// there may be activities in a plan that have a link and others that have a coordinate.  
					// Those that have a link do not need a new link.  In addition, they may not even have a 
					// coordinate.  kai/dominik, nov'11
					
					// this could presumably be combined with (*).  kai/dominik, nov'11
					continue ;
				}

				// If the linkId is still null get nearest link from the network
//				Link link = this.network.getNearestLinkExactly(act.getCoord());
				Link link = NetworkUtils.getNearestLink(this.network, act.getCoord());
				// getNearestLinkExactly not necessarily better than getNearestLink.  E.g.
				// n--n-----------------------------n
				// A home location slightly to the right of the middle node will take:
				// * the left link with getNearestLink
				// * the right link with getNearestLinkExactly
				// kai/dominik, jan'13
				
				
				if (null == link) {
					throw new RuntimeException("For person id="+plan.getPerson().getId()+": getNearestLink returned Null! act="+act);
				}
				act.setLinkId(link.getId());				
			}
			else continue;
		}
	}
}
