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

package playground.mrieser.svi.replanning;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.DynamicODMatrix;

/**
 * @author mrieser
 */
public class MultimodalDynamicODDemandCollector implements PlanAlgorithm {

	private final double maxTime;
	private final int timeBinSize;
	private final ActivityToZoneMapping mapping;
	private final Map<String, DynamicODMatrix> matrices = new HashMap<String, DynamicODMatrix>();
	
	public MultimodalDynamicODDemandCollector(final int timeBinSize, final double maxTime, final ActivityToZoneMapping actToZoneMapping) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.mapping = actToZoneMapping;
	}

	@Override
	public void run(final Plan plan) {
		Activity lastAct = null;
		String lastZoneId = null;
		int idx = 0;
		String[] activityZones = this.mapping.getAgentActivityZones(plan.getPerson().getId());
		Leg lastLeg = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				lastLeg = leg;
			}
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				String zoneId = activityZones[idx];

				if (lastAct != null && lastLeg != null) {
					String mode = lastLeg.getMode();
										
					double tripStartTime = lastAct.getEndTime();
					if (tripStartTime == Time.UNDEFINED_TIME) {
						tripStartTime = lastAct.getStartTime() + lastAct.getMaximumDuration();
					}

					DynamicODMatrix odm = this.matrices.get(mode);
					if (odm == null) {
						odm = new DynamicODMatrix(timeBinSize, maxTime);
						this.matrices.put(mode, odm);
					}
					odm.addTrip(tripStartTime, lastZoneId, zoneId);
				}

				lastLeg = null;
				lastAct = act;
				lastZoneId = zoneId;
				idx++;
			}
		}
	}

	public Map<String, DynamicODMatrix> getDemand() {
		return this.matrices;
	}

}
