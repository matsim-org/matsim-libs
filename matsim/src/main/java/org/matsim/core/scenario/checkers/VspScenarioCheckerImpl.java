/* *********************************************************************** *
 * project: org.matsim.*
 * VspScenarioCheckerImpl
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
package org.matsim.core.scenario.checkers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

public final class VspScenarioCheckerImpl implements ScenarioChecker {
	private static final Logger log = LogManager.getLogger(VspScenarioCheckerImpl.class);

	@Override
	public void checkConsistencyBeforeRun(Scenario scenario) {

		Level lvl;

		switch (scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel()) {
			case ignore -> {
				log.info("NOT running vsp scenario consistency check because vsp defaults checking level is set to IGNORE. This can be changed in the vspExperimental configGroup.");
				return;
			}
			case info -> lvl = Level.INFO;
			case warn -> lvl = Level.WARN;
			case abort -> lvl = Level.WARN;
			default -> throw new RuntimeException("not implemented");
		}
		log.info("running checkConsistency of scenario before run ...");

		boolean problem = false; // ini

		problem = checkSubpopulations(scenario, lvl, problem);
		problem = checkActivitiesOpeningTime(scenario, lvl, problem);

		if (problem && scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel() == VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort) {
			String str = "found a situation that leads to vsp-abort.  aborting ...";
			System.out.flush();
			log.fatal(str);
			throw new RuntimeException(str);
		}
	}

	/**
	 * For a first draft this is empty. But it can be extended in the future
	 *
	 * @param scenario
	 */
	@Override
	public void checkConsistencyAfterRun(Scenario scenario) {
		Level lvl;

		switch (scenario.getConfig().vspExperimental().getVspDefaultsCheckingLevel()) {
			case ignore -> {
				log.info("NOT running scenario config consistency check because vsp defaults checking level is set to IGNORE. This can be changed in the vspExperimental configGroup.");
				return;
			}
			case info -> lvl = Level.INFO;
			case warn -> lvl = Level.WARN;
			case abort -> lvl = Level.WARN;
			default -> throw new RuntimeException("not implemented");
		}
	}


	private boolean checkSubpopulations(Scenario scenario, Level lvl, boolean problem) {

		if (scenario.getPopulation().getPersons().values().stream().anyMatch(p -> PopulationUtils.getSubpopulation(p) == null)) {
			log.log(lvl, "found person(s) without subpopulation.  The vsp default is, to set subpopulations for all agents.  Please check your population file and add subpopulation information to all persons.");
			problem = true;
		}

		Set<String> subpopulations = scenario.getPopulation().getPersons().values().stream()
			.map(PopulationUtils::getSubpopulation)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		// check if there are corresponding scoring params for all subpopulations.
		for (String subpopulation : subpopulations) {
			if (!scenario.getConfig().scoring().getScoringParametersPerSubpopulation().containsKey(subpopulation)) {
				log.log(lvl,
					"Found subpopulation '{}' but no corresponding scoring parameters. Please add scoring parameters for this subpopulation.",
					subpopulation);
				problem = true;
			}
		}
		return problem;
	}

	private boolean checkActivitiesOpeningTime(Scenario scenario, Level lvl, boolean problem) {
		log.info("start checking if activities are roughly within opening times ...");
		Counter counter = new Counter("# person ");
		final TimeTracker timeTracker = new TimeTracker(TimeInterpretation.create(scenario.getConfig()));
		double violationCnt = 0.;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String subpopulation = PopulationUtils.getSubpopulation(person);
			counter.incCounter();
			timeTracker.setTime(0.);
			for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(), ExcludeStageActivities)) {
				ScoringConfigGroup.ActivityParams actParams = scenario.getConfig().scoring().getScoringParameters(subpopulation).getActivityParams(
					activity.getType());

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
		if (violationCnt > 0) {
			log.log(lvl, "For {} activities of a selected plan the are outside the opening time", violationCnt);
			problem = true;
		}
		return problem;
	}
}
