/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.FractionalIterable;

/**
 *
 * @author Gunnar Flötteröd
 * 
 *         based on
 * 
 * @author nagel
 *
 *         because VspPlansCleaner is, as far as I understand, locked into the
 *         dependency injection framework.
 *
 */
public class RouteAndTimeCleaner {

	private static double randomTime_s() {
		return Math.random() * 3600 * 24;
	}

	public static void keepOnlySelected(final Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			person.getPlans().clear();
			person.addPlan(plan);
			person.setSelectedPlan(plan);
		}
	}

	public static void removeTimeAndRoute(final Scenario scenario, final boolean removeTime,
			final boolean removeRoute) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();

			final int actCnt = (plan.getPlanElements().size() + 1) / 2;
			double actLength_s = 3600 * 24 / actCnt;
			double nextActStartTime_s = 0;
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (removeTime) {
						act.setStartTime(nextActStartTime_s);
						act.setMaximumDuration(actLength_s);
						nextActStartTime_s += actLength_s;
						act.setEndTime(nextActStartTime_s);
					}
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (removeTime) {
						leg.setDepartureTime(nextActStartTime_s);
						leg.setTravelTime(0);
					}
					if (removeRoute) {
						leg.setRoute(null);
					}
				}
			}
		}
	}

	static void keepOnlyCarUsers(Population pop) {

		Set<Id<Person>> noCarUsersIds = new LinkedHashSet<>(pop.getPersons().keySet());
		for (Person person : pop.getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if ("car".equals(leg.getMode())) {
						noCarUsersIds.remove(person.getId());
					}
				}
			}
		}

		for (Id<Person> nonCarUserId : noCarUsersIds) {
			pop.getPersons().remove(nonCarUserId);
		}
	}

	static void keepOnlyFraction(Population pop, final double frac) {

		final Set<Id<Person>> idsToRemove = new LinkedHashSet<>();
		for (Id<Person> removePersonId : new FractionalIterable<>(pop.getPersons().keySet(), 1.0 - frac)) {
			idsToRemove.add(removePersonId);
		}

		for (Id<Person> removeId : idsToRemove) {
			pop.getPersons().remove(removeId);
		}
	}

	public static void main(String[] args) {

		System.out.println("exiting");
		System.exit(0);

		String path = "/Users/GunnarF/NoBackup/data-workspace/searchacceleration" + "/rerun-2015-11-23a_No_Toll_large/";
		Config config = ConfigUtils.loadConfig(path + "matsim-config.xml");
		config.plans().setInputFile(path + "400.plans.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		keepOnlySelected(scenario);
		keepOnlyFraction(scenario.getPopulation(), 0.2); // from 5 to 1 percent
		keepOnlyCarUsers(scenario.getPopulation()); // of the maintained fraction!
		removeTimeAndRoute(scenario, true, true);

		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(path + "initial-plans.1pct.xml");
	}

}
