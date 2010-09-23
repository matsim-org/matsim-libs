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
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

public class FirstDepartureTimeFromHomeDistribution extends
		AbstractPersonAlgorithm implements PlanAlgorithm {
	private int bin_s = 300;
	private Map<Integer, Integer> entTimeCounts = new HashMap<Integer, Integer>();

	public FirstDepartureTimeFromHomeDistribution() {
		super();
	}

	public FirstDepartureTimeFromHomeDistribution(int bin_s) {
		super();
		this.bin_s = bin_s;
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {
		Activity act = (Activity) plan.getPlanElements().get(0);
		if (act.getType().startsWith("h")) {
			int endTime = ((int) act.getEndTime()) / this.bin_s;
			Integer count = this.entTimeCounts.get(endTime);
			if (count != null) {
				this.entTimeCounts.put(endTime, count + 1);
			} else {
				this.entTimeCounts.put(endTime, 1);
			}
		}
	}

	public void write(String outputFilename) {
		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("time\tno. of departing agents from home at this time");

		// Integer[] xs = (Integer[]) this.entTimeCounts.keySet().toArray();
		// Integer[] ys = (Integer[]) this.entTimeCounts.values().toArray();
		//
		// XYScatterChart chart = new XYScatterChart(
		// "\"home\" end time distribution", "\"home\" endTime",
		// "no. of agents with departure Time from home");
		// chart.addSeries("serieTitle", xs, ys);//???????????????????????

		for (Entry<Integer, Integer> endTimeCount : entTimeCounts.entrySet()) {
			writer.writeln(Time.writeTime(endTimeCount.getKey() * this.bin_s)
					+ "+\t" + endTimeCount.getValue());
			writer.flush();
		}
		writer.close();
	}

	public static void main(String args[]) {
		// String netFilename = "../matsim/examples/equil/network.xml";
		// String popFilename = "../matsim/examples/equil/plans2000.xml.gz";
		// String outputFilename =
		// "../matsim/examples/equil/plans2000.1stHomeEndTimeDistribution.log";
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String popFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.plans.xml.gz";
		String outputFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.plans.1stHomeEndTimeDistribution.log";
		ScenarioImpl scenario = new ScenarioImpl();

		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(popFilename);

		FirstDepartureTimeFromHomeDistribution fhaed = new FirstDepartureTimeFromHomeDistribution(
				60);
		fhaed.run(pop);
		fhaed.write(outputFilename);
	}
}
