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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts activity ends per zone.
 * At the moment, this takes places once, before the mobsim starts. Thus, we do not account for time allocation of activities..
 * Alternatively, one could implement ActivityEndEventHandler and recalculate every iteration. This might be computionally expensive and may not be worth the effort
 * (depending on the time mutation range).
 *
 *  TODO:test
 *
 * @author tschlenther
 */
public class ActivityLocationBasedZonalDemandAggregator implements ZonalDemandAggregator, StartupListener {

	private final DrtZonalSystem zonalSystem;
	private final int timeBinSize;
	private final Map<Double, Map<String, MutableInt>> activityEndsPerTimeBinAndZone = new HashMap<>();
	private final Scenario scenario;

	public ActivityLocationBasedZonalDemandAggregator(MatsimServices services, DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.scenario = services.getScenario();
		this.zonalSystem = zonalSystem;
		timeBinSize = drtCfg.getMinCostFlowRebalancing().get().getInterval();
		//self registration
		services.addControlerListener(this);
	}

	public Map<String, MutableInt> getExpectedDemandForTimeBin(double time) {
		Double bin = getBinForTime(time);
		return activityEndsPerTimeBinAndZone.getOrDefault(bin, Collections.emptyMap());
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		activityEndsPerTimeBinAndZone.clear();
		prepareZones();

		//go through the selected plans, grab all planned activity end times and save them into activityEndsPerTimeBinAndZone
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			person.getSelectedPlan().getPlanElements().stream()
					.filter(planElement -> planElement instanceof Activity)
					.filter(activity -> ! TripStructureUtils.isStageActivityType(((Activity) activity).getType()))
					.forEach(planElement -> {
						Activity act = (Activity) planElement;
						double endTime;
						if(act.getEndTime().isDefined()) {
							endTime = act.getEndTime().seconds();
						} else {
							if(act.getStartTime().isUndefined() || act.getMaximumDuration().isUndefined()){
								//if this is the last activity, we can use qsim end time or set endTime to 36h
								if(person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size() -1).equals(act)){
									endTime = scenario.getConfig().qsim().getEndTime().isDefined() ? scenario.getConfig().qsim().getEndTime().seconds() : 36*3600;
								} else {
									throw new RuntimeException("activity " + act + " of person " + person + " has no information on end time and it can not " +
											"be derived out of start time and maximum duration. As the activity is not the last in the plan, we do not know how to deal with that...");
								}
							} else {
								endTime = act.getStartTime().seconds() + act.getMaximumDuration().seconds(); //this fails intentionally if one of startTime and maximumDuration is not set
							}
						}
						String zoneId = zonalSystem.getZoneForLinkId(act.getLinkId());
						Double bin = getBinForTime(endTime);

						if (zoneId == null) {
							Logger.getLogger(getClass()).error("No zone found for linkId " + act.getLinkId().toString());
							return;
						}
						if (activityEndsPerTimeBinAndZone.containsKey(bin)) {
							this.activityEndsPerTimeBinAndZone.get(bin).get(zoneId).increment();
						} else
							Logger.getLogger(getClass())
									.error("Time " + Time.writeTime(endTime) + " / bin " + bin + " is out of boundary");
					});
		}
	}

	private void prepareZones() {
		for (int i = 0; i < (3600 / timeBinSize) * 36; i++) {
			Map<String, MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			activityEndsPerTimeBinAndZone.put(Double.valueOf(i), zonesPerSlot);
		}
	}

	private Double getBinForTime(double time) {
		return Math.floor(time / timeBinSize);
	}
}
