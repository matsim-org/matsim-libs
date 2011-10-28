/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.vehtrajectories.DynamicTravelTimeMatrix;

/**
 * @author mrieser
 */
public class PlanScorer {

	private final DynamicTravelTimeMatrix matrix;
	private final ActivityToZoneMapping actToZoneMapping;

	public PlanScorer(final DynamicTravelTimeMatrix matrix, final ActivityToZoneMapping actToZoneMapping) {
		this.matrix = matrix;
		this.actToZoneMapping = actToZoneMapping;
	}

	public void calculateScore(final Plan plan) {
		double score = 0.0;

		String[] zoneIds = this.actToZoneMapping.getAgentActivityZones(plan.getPerson().getId());
		int actCounter = 0;

		Activity lastAct = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				// TODO

				lastAct = act;
				actCounter++;
			}
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				double tripStartTime = lastAct.getEndTime();
				if (tripStartTime == Time.UNDEFINED_TIME) {
					tripStartTime = lastAct.getStartTime() + lastAct.getMaximumDuration();
				}
				String fromZone = zoneIds[actCounter - 1];
				String toZone = zoneIds[actCounter];

				if (TransportMode.car.equals(leg.getMode())) {
					double travTime = this.matrix.getAverageTravelTime(tripStartTime, fromZone, toZone);
					if (!Double.isNaN(travTime)) {
						score = score - travTime; // TODO value of time etc
					}
				}

				// TODO other modes

			}
		}

		plan.setScore(score);
	}
}
