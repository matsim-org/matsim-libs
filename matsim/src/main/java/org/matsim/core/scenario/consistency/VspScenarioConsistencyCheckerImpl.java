/* *********************************************************************** *
 * project: org.matsim.*
 * VspScenarioConsistencyCheckerImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
package org.matsim.core.scenario.consistency;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

public final class VspScenarioConsistencyCheckerImpl implements ScenarioConsistencyChecker {
	private static final Logger log = LogManager.getLogger(VspScenarioConsistencyCheckerImpl.class);

	@Override
	public void checkConsistency(Scenario scenario) {
		checkActivities(scenario);
	}

	static void checkActivities(Scenario scenario) {
		log.info("start checking if activities are roughly within opening times ...");
		Counter counter = new Counter("# person ");
		final TimeTracker timeTracker = new TimeTracker(TimeInterpretation.create(scenario.getConfig()));
		double violationCnt = 0.;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			counter.incCounter();
			timeTracker.setTime(0.);
			for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(), ExcludeStageActivities)) {
				ScoringConfigGroup.ActivityParams actParams = scenario.getConfig().scoring().getActivityParams(activity.getType());

				if (actParams.getClosingTime().isDefined()) {
					if (actParams.getClosingTime().seconds() < timeTracker.getTime().seconds()) {
//							log.warn( "activity type={}; closing time was at time={}; at time={}, we are already beyond that.",
//								activity.getType(), actParams.getClosingTime().seconds() / 3600, timeTracker.getTime().seconds()/3600. );
						violationCnt++;
					}
				}
				timeTracker.addActivity(activity);
//					if ( timeTracker.getTime().isUndefined() ) {
//						log.warn( "current time={} is undefined after activity={}", timeTracker.getTime(), activity );
//					}
				if (timeTracker.getTime().isDefined() // otherwise presumably last activity of day
					&& actParams.getOpeningTime().isDefined()) {
					if (timeTracker.getTime().seconds() < actParams.getOpeningTime().seconds()) {
//							log.warn( "activity of type={} ends at time={}, this is before time={} when the activity type opens",
//								activity.getType(), timeTracker.getTime().seconds()/3600., actParams.getOpeningTime().seconds()/3600. );
						violationCnt++;
					}
				}
			}
		}
		log.warn("violationCnt={}", violationCnt);
	}
}
