/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.impl;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.utils.collections.Tuple;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanAgent;

/**
 * @author mrieser
 */
public class TeleportationHandler implements DepartureHandler, Steppable {

	// TODO TeleportationHandler also needs something like isFinished()

	private final NewSimEngine simEngine;
	private final PriorityQueue<Tuple<Double, PlanAgent>> teleportationList = new PriorityQueue<Tuple<Double, PlanAgent>>(30, new TeleportationArrivalTimeComparator());

	public TeleportationHandler(final NewSimEngine simEngine) {
		this.simEngine = simEngine;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		double arrivalTime = this.simEngine.getCurrentTime() + leg.getTravelTime();
		this.teleportationList.add(new Tuple<Double, PlanAgent>(arrivalTime, agent));
	}

	@Override
	public void doSimStep(double time) {
		while ((!this.teleportationList.isEmpty()) && this.teleportationList.peek().getFirst().doubleValue() <= time) {
			this.simEngine.handleAgent(this.teleportationList.poll().getSecond());
		}
	}

	private static class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, PlanAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		public int compare(final Tuple<Double, PlanAgent> o1, final Tuple<Double, PlanAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getPlan().getPerson().getId().compareTo(o1.getSecond().getPlan().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}
}
