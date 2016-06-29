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

import org.matsim.api.core.v01.TransportMode;
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
public class DynamicODDemandCollector implements PlanAlgorithm {

	private final DynamicODMatrix odm;
	private final ActivityToZoneMapping mapping;
	private int counter = 0;
	private final Map<String, Integer> modeCounts = new HashMap<String, Integer>();
	
	public DynamicODDemandCollector(final DynamicODMatrix odm, final ActivityToZoneMapping actToZoneMapping) {
		this.odm = odm;
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
					
					Integer cnt = this.modeCounts.get(mode);
					if (cnt == null) {
						this.modeCounts.put(mode, 1);
					} else {
						this.modeCounts.put(mode, 1 + cnt.intValue());
					}
					
					if (mode.equals(TransportMode.car)) {
						double tripStartTime = lastAct.getEndTime();
						if (tripStartTime == Time.UNDEFINED_TIME) {
							tripStartTime = lastAct.getStartTime() + lastAct.getMaximumDuration();
						}
						
						this.odm.addTrip(tripStartTime, lastZoneId, zoneId);
						this.counter++;
					}
				}

				lastLeg = null;
				lastAct = act;
				lastZoneId = zoneId;
				idx++;
			}
		}
	}
	
	public int getCounter() {
		return counter;
	}
	
	public Map<String, Integer> getModeCounts() {
		return this.modeCounts;
	}

}
