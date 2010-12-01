/* *********************************************************************** *
 * project: org.matsim.*
 * MNLUtilsTest.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.tests;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PlanImpl.Type;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.CharyparNagelScoringFunctionFactory4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.Events2Score4TravPerfStuck_mnl;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.Events2Score4TravPerf_mnl;

import cadyts.utilities.math.MultinomialLogit;

public class MNLUtilsTest {
	public static void main(String[] args) {
		String eventsFilename = "test/mnlUtilsTest/400.events.txt.gz";
		String popFilename = "test/mnlUtilsTest/output_plans.xml.gz";
		String netFilename = "test/prepare3/network.xml";
		String configFilename = "test/prepare3/config4UtilTest.xml";

		double travelingPt = -3d;

		Scenario scenario = new ScenarioImpl();

		Config config = scenario.getConfig();
		new MatsimConfigReader(config).readFile(configFilename);

		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManager events = new EventsManagerImpl();

		// initialize MultinomialLogit
		MultinomialLogit mnl = new MultinomialLogit(1/* choiceSetSize */, 4/*
																		 * attributeCount[
																		 * travCar
																		 * ,
																		 * travPt
																		 * ,
																		 * Perf,
																		 * stuck
																		 * ]
																		 */);
		mnl.setUtilityScale(2d);
		for (int i = 0; i < 1/* choiceSetSize, only 1 element in choiceSet */; i++)
			mnl.setASC(i, 0);
		mnl.setCoefficient(0, -6d/* traveling */);
		mnl.setCoefficient(1, travelingPt);
		mnl.setCoefficient(2, 6d/* performing */);
		mnl.setCoefficient(3, -18d/* lateArrival */);

		// initialize scoringConfigGroup
		CharyparNagelScoringConfigGroup scoringConfigGroup = config
				.charyparNagelScoring();
		scoringConfigGroup.setTravelingPt_utils_hr(travelingPt);

		// pop?
		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(popFilename);
		// deletes all unselected plans
		for (Person person : pop.getPersons().values()) {
			List<Plan> plans = new ArrayList<Plan>();
			plans.addAll(person.getPlans());
			for (Plan plan : plans) {
				if (!plan.isSelected())
					person.getPlans().remove(plan);
			}
		}
		Map<Id, Double> selectedScores = new HashMap<Id, Double>();
		for (Person person : pop.getPersons().values()) {
			selectedScores.put(person.getId(), person.getSelectedPlan()
					.getScore());
		}
		Events2Score4TravPerf_mnl events2score = new
		// DummyEvents2Score_mnl
		Events2Score4TravPerfStuck_mnl//
		(mnl, new CharyparNagelScoringFunctionFactory4PC(scoringConfigGroup),
				pop, 4, scoringConfigGroup);
		events.addHandler(events2score);

		new MatsimEventsReader(events).readFile(eventsFilename);

		events2score.finish();
		int n = 0;
		for (Person person : pop.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (((PlanImpl) selectedPlan).getType().equals(Type.CAR)) {
				n++;
				double matsimScore = selectedScores.get(person.getId());

				events2score.setPersonScore(person);
				double yuScore = events2score.getAgentScore(person.getId());

				events2score.setPersonAttrs(person);
				double mnlScore = events2score.getMultinomialLogit().getUtils()
						.get(0);
				if (matsimScore != yuScore || matsimScore != mnlScore) {
					System.err.println("matsim:\t" + matsimScore + "\tyu:\t"
							+ yuScore + "\tmnl:\t" + mnlScore);
				}
			}
		}
		System.out.println("there is totally " + n + " CAR-Plans");
	}
}
