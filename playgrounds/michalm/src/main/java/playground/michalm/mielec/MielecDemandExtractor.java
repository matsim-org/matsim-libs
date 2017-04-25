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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.config.*;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class MielecDemandExtractor {
	public static void main(String[] args) {
		String[] suffixes = { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0" };
		for (String suffix : suffixes) {
			process(suffix);
		}
	}

	private static void process(String suffix) {
		String dir = "d:/eclipse/shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/plans_taxi/";
		String planFile = dir + "plans_taxi_" + suffix + ".xml.gz";
		String demandFile = dir + "taxi_demand_" + suffix + ".txt";

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(planFile);

		try (CompactCSVWriter csvWriter = new CompactCSVWriter(IOUtils.getBufferedWriter(demandFile))) {
			csvWriter.writeNext("personId", "departureTime", "fromLinkId", "toLinkId");

			for (Person p : scenario.getPopulation().getPersons().values()) {
				Leg leg = (Leg)p.getPlans().get(0).getPlanElements().get(1);

				if (leg.getMode() == TaxiModule.TAXI_MODE) {
					csvWriter.writeNext(p.getId() + "", (int)leg.getDepartureTime() + "", //
							leg.getRoute().getStartLinkId() + "", leg.getRoute().getEndLinkId() + "");
				}
			}
		}
	}
}
