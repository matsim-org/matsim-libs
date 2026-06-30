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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

public final class VspScenarioConsistencyCheckerImpl implements ScenarioConsistencyChecker {
	private static final Logger log = LogManager.getLogger(VspScenarioConsistencyCheckerImpl.class);

	@Override
	public void checkConsistencyBeforeRun(Scenario scenario) {

		Level lvl;

		switch (scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel()) {
			case ignore -> {
				log.info("NOT running vsp config consistency check because vsp defaults checking level is set to IGNORE");
				return;
			}
			case info -> lvl = Level.INFO;
			case warn -> lvl = Level.WARN;
			case abort -> lvl = Level.WARN;
			default -> throw new RuntimeException("not implemented");
		}
		log.info("running checkConsistency of scenario before run ...");

		boolean problem = false; // ini

		if (problem && scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel() == VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort) {
			String str = "found a situation that leads to vsp-abort.  aborting ...";
			System.out.flush();
			log.fatal(str);
			throw new RuntimeException(str);
		}
	}

	@Override
	public void checkConsistencyAfterRun(Scenario scenario) {
		Level lvl;

		switch (scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel()) {
			case ignore -> {
				log.info("NOT running vsp config consistency check because vsp defaults checking level is set to IGNORE");
				return;
			}
			case info -> lvl = Level.INFO;
			case warn -> lvl = Level.WARN;
			case abort -> lvl = Level.WARN;
			default -> throw new RuntimeException("not implemented");
		}
		log.info("running checkConsistency of scenario after run ...");

		checkActivitiesOpeningTime(scenario, lvl);
	}

	static void checkActivitiesOpeningTime(Scenario scenario, Level lvl) {
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
						log.log(lvl, "activity type={}; closing time was at time={}; at time={}, we are already beyond that.",
								activity.getType(), actParams.getClosingTime().seconds() / 3600, timeTracker.getTime().seconds()/3600. );
						violationCnt++;
					}
				}
				timeTracker.addActivity(activity);
					if ( timeTracker.getTime().isUndefined() ) {
						log.log(lvl, "current time={} is undefined after activity={}", timeTracker.getTime(), activity );
					}
				if (timeTracker.getTime().isDefined() // otherwise presumably last activity of day
					&& actParams.getOpeningTime().isDefined()) {
					if (timeTracker.getTime().seconds() < actParams.getOpeningTime().seconds()) {
							log.log(lvl, "activity of type={} ends at time={}, this is before time={} when the activity type opens",
								activity.getType(), timeTracker.getTime().seconds()/3600., actParams.getOpeningTime().seconds()/3600. );
						violationCnt++;
					}
				}
			}
		}
		log.warn("violationCnt={}", violationCnt);
	}
}
