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

import java.util.ArrayList;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

/**
 * Assigns each activity in a plan a link where the activity takes place
 * based on the coordinates given for the activity.
 *
 * @author mrieser
 */
public class XY2Links extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final NetworkLayer network;

	public XY2Links(final NetworkLayer network) {
		super();
		this.network = network;
	}

	/** Assigns links to each activity in all plans of the person. */
	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			processPlan(plan);
		}
	}

	/** Assigns links to each activity in the plan. */
	public void run(final Plan plan) {
		processPlan(plan);
	}

	private void processPlan(final Plan plan) {
		ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			Act act = (Act)actslegs.get(j);
			Link link = this.network.getNearestLink(act.getCoord());
			if (null == link) {
				throw new RuntimeException("For person id="+plan.getPerson().getId()+": getNearestLink returned Null! act="+act);
			}
			act.setLink(link);
		}
	}
}
