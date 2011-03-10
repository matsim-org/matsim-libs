/* *********************************************************************** *
 * project: org.matsim.*
 * FirstDepartureTimeFromHomeDistribution.java
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

package playground.yu.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

public class ActivityEndTimeDistribution extends AbstractPersonAlgorithm
		implements PlanAlgorithm {
	private int bin_s = 300;
	private Map<String/* actType */, Map<Integer/* time */, Integer/* count */>> entTimeCounts = new HashMap<String, Map<Integer, Integer>>();

	public ActivityEndTimeDistribution() {
		super();
	}

	public ActivityEndTimeDistribution(int bin_s) {
		super();
		this.bin_s = bin_s;
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {
		// Activity act = (Activity) plan.getPlanElements().get(0);
		// if (act.getType().startsWith("h")) {
		// int entTime = ((int) act.getEndTime()) / this.bin_s;
		// Integer count = this.entTimeCounts.get(entTime);
		// if (count != null) {
		// this.entTimeCounts.put(entTime, count + 1);
		// } else {
		// this.entTimeCounts.put(entTime, 1);
		// }
		// }

		List<PlanElement> planElements = plan.getPlanElements();
		for (int i = 0; i < planElements.size() - 1; i += 2) {
			Activity act = (Activity) planElements.get(i);

			String type = act.getType().substring(0, 1);
			Map<Integer, Integer> timeCount = this.entTimeCounts.get(type);
			if (timeCount == null) {
				timeCount = new HashMap<Integer, Integer>();
				this.entTimeCounts.put(type, timeCount);
			}

			int endTime = ((int) act.getEndTime()) / this.bin_s;
			Integer count = timeCount.get(endTime);
			if (count != null) {
				timeCount.put(endTime, count + 1);
			} else {
				timeCount.put(endTime, 1);
			}
		}

	}

	public void write(String outputFilename) {
		for (String type : this.entTimeCounts.keySet()) {
			SimpleWriter writer = new SimpleWriter(outputFilename + type
					+ ".log");
			writer.writeln("time\tno. of agents departing after " + type);

			for (Entry<Integer, Integer> endTimeCount : entTimeCounts.get(type)
					.entrySet()) {
				writer.writeln(Time.writeTime(endTimeCount.getKey()
						* this.bin_s)
						+ "+\t" + endTimeCount.getValue());
				writer.flush();
			}
			writer.close();
		}
	}

	public static void main(String args[]) {
		// String netFilename = "../matsim/examples/equil/network.xml";
		// String popFilename = "../matsim/examples/equil/plans2000.xml.gz";
		// String outputFilename =
		// "../matsim/examples/equil/plans2000.1stHomeEndTimeDistribution.log";
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String popFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.plans.xml.gz";
		String outputFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.plans.departureTimeDistributionafter.";
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(popFilename);

		ActivityEndTimeDistribution fhaed = new ActivityEndTimeDistribution(60);
		fhaed.run(pop);
		fhaed.write(outputFilename);
	}
}
