/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.mielec;

import java.util.List;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.core.config.*;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class MielecTaxiPassengerPopulationCreator {
	public static void process(String suffix) {
		String dir = "../../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/plans_taxi/";
		String demandFile = dir + "taxi_demand_" + suffix + ".txt";
		String planFile = dir + "plans_only_taxi_mini_benchmark_" + suffix + ".xml.gz";

		// we have two travel demand waves:
		// 6:00:00-12:59:59
		// 13:00:00-19:59:59
		final int maxDepartureTime = 13 * 3600;
		final int departureTimeShift = 6 * 3600;

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();

		List<String[]> lines = CSVReaders.readTSV(demandFile);
		for (int i = 1; i < lines.size(); i++) {
			String[] line = lines.get(i);
			Id<Person> personId = Id.createPersonId(line[0]);

			double departureTime = Double.parseDouble(line[1]);
			if (departureTime >= maxDepartureTime) {
				continue;
			}
			departureTime -= departureTimeShift;

			Id<Link> fromLinkId = Id.createLinkId(line[2]);
			Id<Link> toLinkId = Id.createLinkId(line[3]);

			Plan plan = pf.createPlan();
			Activity act = pf.createActivityFromLinkId("dummy", fromLinkId);
			act.setEndTime(departureTime);
			plan.addActivity(act);
			Leg leg = pf.createLeg(TaxiModule.TAXI_MODE);
			leg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));
			plan.addLeg(leg);
			plan.addActivity(pf.createActivityFromLinkId("dummy", toLinkId));

			Person person = pf.createPerson(personId);
			person.addPlan(plan);
			population.addPerson(person);
		}

		new PopulationWriter(population).write(planFile);
	}

	public static void main(String[] args) {
		String[] suffixes = { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0" };
		for (String suffix : suffixes) {
			process(suffix);
		}
	}
}
