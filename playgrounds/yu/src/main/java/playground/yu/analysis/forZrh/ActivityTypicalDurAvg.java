/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypicalDurAvg.java
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

/**
 * 
 */
package playground.yu.analysis.forZrh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.math.SimpleStatistics;

/**
 * calculate the average typical duration of aggregated activity type e.g. h, w
 * ... among all the agents
 * 
 * @author yu
 * 
 */
public class ActivityTypicalDurAvg extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private Map<String/* actType aggregated */, List<Double>/* durations */> actTypicalDurations = new HashMap<String, List<Double>>();

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {
		List<PlanElement> pes = plan.getPlanElements();
		for (int i = 2; i < pes.size(); i += 2) {
			Activity act = (Activity) pes.get(i);

			String subType = act.getType();
			String superType = subType.substring(0, 1);
			if (!superType.equals("t")) {
				double typicalDur /* [h] */= Double.parseDouble(subType
						.substring(1));

				List<Double> typicalDurations = actTypicalDurations
						.get(superType);
				if (typicalDurations == null) {
					typicalDurations = new ArrayList<Double>();
					actTypicalDurations.put(superType, typicalDurations);
				}
				typicalDurations.add(typicalDur);
			}
		}
	}

	public String output() {
		String toReturn = "actSuperType\tmin\tmax\tavg\n";
		for (Entry<String, List<Double>> actTypeTypicalDurationEntry : actTypicalDurations
				.entrySet()) {
			String actSuperType = actTypeTypicalDurationEntry.getKey();
			List<Double> typicalDurations = actTypeTypicalDurationEntry
					.getValue();
			toReturn += actSuperType + "\t"
					+ SimpleStatistics.min(typicalDurations) + "\t"
					+ SimpleStatistics.max(typicalDurations) + "\t"
					+ SimpleStatistics.average(typicalDurations) + "\n";

		}
		return toReturn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml", //
		populationFilename = "../runs-svn/run1300/ITERS/it.1000/1000.plans.xml.gz";

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// Network network=scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		ActivityTypicalDurAvg atda = new ActivityTypicalDurAvg();
		atda.run(population);
		System.out.println(atda.output());
	}
}
