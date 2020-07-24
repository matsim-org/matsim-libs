/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.drt.analysis.zonal;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Approximates population size per zone by counting first activites per zone in the selected plans.
 * Should lead lead to rebalancing target values dependent on number of inhabitants.
 *
 * @author tschlenther
 */
public final class FirstActivityCountAsZonalDemandAggregator implements ZonalDemandAggregator {

	private final DrtZonalSystem zonalSystem;
	private final Map<Double, Map<String, MutableInt>> actEnds = new HashMap<>();
	private Map<String, Integer> zonalDemand = new HashMap<>();
	private final Map<Double, Map<String, MutableInt>> activityEndsPerTimeBinAndZone = new HashMap<>();
	private static final MutableInt ZERO =  new MutableInt(0);

	public FirstActivityCountAsZonalDemandAggregator(DrtZonalSystem zonalSystem, Population population) {
		this.zonalSystem = zonalSystem;
		prepareZones();
		countFirstActsPerZone(population);
	}

	private void countFirstActsPerZone(Population population) {
		population.getPersons().values().stream()
				.map(person -> person.getSelectedPlan().getPlanElements().get(0))
				.forEach(element -> {
					if (! (element instanceof Activity) ) throw new RuntimeException("first plan element is not an activity");
					Activity activity = (Activity) element;
					String zone = zonalSystem.getZoneForLinkId(activity.getLinkId());
					Integer oldDemandValue = this.zonalDemand.get(zone);
					this.zonalDemand.put(zone, oldDemandValue + 1);
				});
	}

	public ToIntFunction<String> getExpectedDemandForTimeBin(double time) {
		return zoneId -> this.zonalDemand.getOrDefault(zoneId, 0).intValue();
	}

	private void prepareZones() {
		for (String zone : zonalSystem.getZones().keySet()) {
			zonalDemand.put(zone, 0);
		}
	}

}
