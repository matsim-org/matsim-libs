/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package ft.utils;

import java.io.IOException;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author fthiel
 *
 */
public class ActivityTimeChecker {
	public static void main(String[] args) throws IOException {

		int failCounter = 0;
		// Create a Scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// Fill this Scenario with a population.
		new PopulationReader(scenario).readFile(
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz");
		double actStartTime = 0;

		for (Person person : scenario.getPopulation().getPersons().values()) {

			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			boolean firstact = true;
			for (PlanElement planElement : planElements) {

				if (planElement instanceof Activity) {
					Activity act = (Activity) planElement;
					double actEndTime = act.getEndTime();
					double actDuration = 0;
					if (firstact == true) {
						firstact = false;
					} else if (!Time.isUndefinedTime(act.getEndTime())) {
						Leg prevLeg = PopulationUtils.getPreviousLeg(person.getSelectedPlan(), act);
						Activity prevAct = PopulationUtils.getPreviousActivity(person.getSelectedPlan(), prevLeg);
						actStartTime = prevLeg.getTravelTime() + prevAct.getEndTime();
						actDuration = actEndTime - actStartTime;
					}

					if (actDuration < 0) {
						Id<Person> personID = person.getId();
						System.out.println(personID);
						failCounter++;

					}
				}
			}
		}
		System.out.println(failCounter);
	}

}
