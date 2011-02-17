/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeFromPopulationSummary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.choiceSetGeneration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.math.SimpleStatistics;

/**
 * @author yu
 * 
 */
public class PathSizeFromPopulationSummary extends AbstractPersonAlgorithm {

	private Network network;
	private List<Double> minPSs, maxPSs, avgPSs, allPSs;

	public PathSizeFromPopulationSummary(Network network) {
		this.network = network;
		minPSs = new ArrayList<Double>();
		maxPSs = new ArrayList<Double>();
		avgPSs = new ArrayList<Double>();
		allPSs = new ArrayList<Double>();
	}

	@Override
	public void run(Person person) {
		if (hasCarLeg(person)) {
			PathSizeFromPlanChoiceSet psfpcs = new PathSizeFromPlanChoiceSet(
					network, person.getPlans());
			minPSs.add(psfpcs.getMinPathSize());
			maxPSs.add(psfpcs.getMaxPathSize());
			avgPSs.add(psfpcs.getAvgPathSize());
			allPSs.addAll(psfpcs.getAllPathSizes());
		}
	}

	/**
	 * judge, whether there is car Leg in Plans of person
	 * 
	 * @param person
	 * @return
	 */
	private boolean hasCarLeg(Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals(TransportMode.car)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void output(String outputFilenameBase) {
		DistributionCreator minDc = new DistributionCreator(minPSs, 0.01);
		minDc.createChartPercent(outputFilenameBase + "minPS.png",
				"Distribution of minimal path-sizes in choice set",
				"path-size (0, 1.0]", "frequency of path-size");
		minDc.write(outputFilenameBase + "minPS.log");

		DistributionCreator maxDc = new DistributionCreator(maxPSs, 0.01);
		maxDc.createChartPercent(outputFilenameBase + "maxPS.png",
				"Distribution of maximal path-sizes in choice set",
				"path-size (0, 1.0]", "frequency of path-size");
		maxDc.write(outputFilenameBase + "maxPS.log");

		DistributionCreator avgDc = new DistributionCreator(avgPSs, 0.01);
		avgDc.createChartPercent(outputFilenameBase + "avgPS.png",
				"Distribution of average path-sizes in choice set",
				"path-size (0, 1.0]", "frequency of path-size");
		avgDc.write(outputFilenameBase + "avgPS.log");

		DistributionCreator allDc = new DistributionCreator(allPSs, 0.01);
		allDc.createChartPercent(outputFilenameBase + "allPS.png",
				"Distribution of all path-sizes in choice set",
				"path-size (0, 1.0]", "frequency of path-size");
		allDc.write(outputFilenameBase + "allPS.log");

		System.out.println("avg. of minPS\t" + SimpleStatistics.average(minPSs)
				+ "\navg. of maxPS\t" + SimpleStatistics.average(maxPSs)
				+ "\navg. of avgPS\t" + SimpleStatistics.average(avgPSs)
				+ "\navg. of allPS\t" + SimpleStatistics.average(allPSs));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = args[0], populationFilename = args[1], outputFilenameBase = args[2];

		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		PathSizeFromPopulationSummary psfps = new PathSizeFromPopulationSummary(
				scenario.getNetwork());
		psfps.run(scenario.getPopulation());
		psfps.output(outputFilenameBase);
	}

}
