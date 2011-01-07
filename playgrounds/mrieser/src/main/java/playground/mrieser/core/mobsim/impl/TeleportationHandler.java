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

package playground.mrieser.core.mobsim.impl;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.NewSimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.MobsimKeepAlive;
import playground.mrieser.core.mobsim.features.MobsimFeature2;

/**
 * @author mrieser
 */
public class TeleportationHandler implements DepartureHandler, MobsimFeature2, MobsimKeepAlive {

	private final static Logger log = Logger.getLogger(TeleportationHandler.class);

	private final NewSimEngine simEngine;
	private final PriorityQueue<Tuple<Double, PlanAgent>> teleportationList = new PriorityQueue<Tuple<Double, PlanAgent>>(30, new TeleportationArrivalTimeComparator());
	private double defaultTeleportationTime = Time.UNDEFINED_TIME;

	public TeleportationHandler(final NewSimEngine simEngine) {
		this.simEngine = simEngine;
		this.simEngine.addKeepAlive(this);
	}

	public void setDefaultTeleportationTime(double defaultTeleportationTime) {
		this.defaultTeleportationTime = defaultTeleportationTime;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		double travelTime = leg.getTravelTime();
		if (travelTime == Time.UNDEFINED_TIME) {
			if (this.defaultTeleportationTime != Time.UNDEFINED_TIME) {
				log.warn("Leg of agent has no travel time specified. Using default teleportation time. agentId = " + agent.getPlan().getPerson().getId());
				travelTime = this.defaultTeleportationTime;
			} else {
				log.error("Leg of agent has no travel time specified. Cannot teleport. agentId = " + agent.getPlan().getPerson().getId());
				return;
			}
		}
		double arrivalTime = this.simEngine.getCurrentTime() + travelTime;
		this.teleportationList.add(new Tuple<Double, PlanAgent>(arrivalTime, agent));
	}

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void doSimStep(double time) {
		while ((!this.teleportationList.isEmpty()) && this.teleportationList.peek().getFirst().doubleValue() <= time) {
			this.simEngine.handleAgent(this.teleportationList.poll().getSecond());
		}
	}

	@Override
	public void afterMobSim() {
	}

	@Override
	public boolean keepAlive() {
		return !this.teleportationList.isEmpty();
	}

	/*package*/ static class TeleportationArrivalTimeComparator implements Comparator<Tuple<Double, PlanAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final Tuple<Double, PlanAgent> o1, final Tuple<Double, PlanAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getPlan().getPerson().getId().compareTo(o1.getSecond().getPlan().getPerson().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}
}
