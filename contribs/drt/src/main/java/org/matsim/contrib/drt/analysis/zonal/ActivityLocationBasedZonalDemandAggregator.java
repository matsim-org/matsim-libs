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

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates all activity ends per iteration and returns the numbers from the previous iteration
 * as expected demand for the current iteration. This will lead to rebalancing target locations related to activity volume per zone.
 *
 * @author tschlenther
 */
public class ActivityLocationBasedZonalDemandAggregator implements ZonalDemandAggregator, ActivityEndEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final int timeBinSize;
	private final Set<Id<Person>> persons;
	private final Map<Double, Map<String, MutableInt>> actEnds = new HashMap<>();
	private final Map<Double, Map<String, MutableInt>> activityEndsPerTimeBinAndZone = new HashMap<>();

	public ActivityLocationBasedZonalDemandAggregator(EventsManager eventsManager, Set<Id<Person>> personsToBeMonitored, DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.zonalSystem = zonalSystem;
		timeBinSize = drtCfg.getMinCostFlowRebalancing().get().getInterval();
		this.persons = personsToBeMonitored;
		//self-registration
		eventsManager.addHandler(this);
	}

	public Map<String, MutableInt> getExpectedDemandForTimeBin(double time) {
		Double bin = getBinForTime(time);
		return activityEndsPerTimeBinAndZone.getOrDefault(bin, Collections.emptyMap());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		//we need to filter activity types because we do not want to position vehicles at every arbitrary activity hotspot
		//this filter here is now adopted to the Open Berlin Scenario
		//TODO replace by subpopulation filter or get actTypes from somewhere else (config)?
		if(TripStructureUtils.isStageActivityType(event.getActType()) ||
				(event.getActType().contains("freight")) ||
				(event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) ||
				(event.getActType().equals(DrtActionCreator.DRT_STOP_NAME)) ||
				(event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)) ||
				(event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) ||
				(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)))
			return;

		Double bin = getBinForTime(event.getTime());
		String zoneId = zonalSystem.getZoneForLinkId(event.getLinkId());
		if (zoneId == null) {
			Logger.getLogger(getClass()).error("No zone found for linkId " + event.getLinkId().toString());
			return;
		}
		if (actEnds.containsKey(bin)) {
			this.actEnds.get(bin).get(zoneId).increment();
		} else
			Logger.getLogger(getClass())
					.error("Time " + Time.writeTime(event.getTime()) + " / bin " + bin + " is out of boundary");
	}

	@Override
	public void reset(int iteration) {
		activityEndsPerTimeBinAndZone.clear();
		activityEndsPerTimeBinAndZone.putAll(actEnds);
		actEnds.clear();
		prepareZones();
	}


	private void prepareZones() {
		for (int i = 0; i < (3600 / timeBinSize) * 36; i++) {
			Map<String, MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			actEnds.put(Double.valueOf(i), zonesPerSlot);
		}
	}

	private Double getBinForTime(double time) {
		return Math.floor(time / timeBinSize);
	}


}
