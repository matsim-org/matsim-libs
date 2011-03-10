/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeSummaryFrom2Populations.java
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
package playground.yu.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.choiceSetGeneration.PathSizeFrom2Routes;
import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.math.SimpleStatistics;

/**
 * @author yu
 * 
 */
public class PathSizeSummaryFrom2Populations extends AbstractPersonAlgorithm
		implements PlanAlgorithm {
	private Population referencePopulation;
	private Plan referenceSelectedPlan;
	private Id personId;
	private List<Double> pathSizes;

	public PathSizeSummaryFrom2Populations(Population referencePopulation) {
		this.referencePopulation = referencePopulation;
		pathSizes = new ArrayList<Double>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.population.algorithms.AbstractPersonAlgorithm#run(org.matsim
	 * .api.core.v01.population.Person)
	 */
	@Override
	public void run(Person currentPerson) {
		personId = currentPerson.getId();
		Person referencePerson = referencePopulation.getPersons().get(personId);
		referenceSelectedPlan = referencePerson.getSelectedPlan();
		run(currentPerson.getSelectedPlan());
	}

	@Override
	public void run(Plan currentPlan) {
		List<PlanElement> referencePlanElements = referenceSelectedPlan
				.getPlanElements(), currentPlanElements = currentPlan
				.getPlanElements();
		for (int i = 0; i < referencePlanElements.size(); i++) {
			PlanElement referencePlanElement = referencePlanElements.get(i);
			if (referencePlanElement instanceof Leg) {
				Leg referenceLeg = (Leg) referencePlanElement;

				if (referenceLeg.getMode().equals(TransportMode.car)) {
					if (currentPlanElements.get(i) instanceof Leg) {
						Leg currentLeg = (Leg) currentPlanElements.get(i);
						if (currentLeg.getMode().equals(TransportMode.car)) {
							NetworkRoute referenceRoute = (NetworkRoute) referenceLeg
									.getRoute(), currentRoute = (NetworkRoute) currentLeg
									.getRoute();
							double pathSize = new PathSizeFrom2Routes(
									currentRoute, referenceRoute).getPathSize();
							pathSizes.add(pathSize);
						} else {// !newLeg.getMode().equals(TransportMode.car)
							throw new RuntimeException(
									"currentLeg with PlanElement index " + i
											+ " should be a \"car\" Leg [["
											+ currentLeg + "]]");
						}// !newLeg.getMode().equals(TransportMode.car)

					} else {// !currentPlanElements.get(i) instanceof Leg
						throw new RuntimeException("CurrentPlanElement " + i
								+ " of Person " + personId
								+ " should be a Leg [["
								+ currentPlanElements.get(i) + "]]");

					}// !currentPlanElements.get(i) instanceof Leg

				}// referenceLeg.getMode().equals(TransportMode.car)

			}// referencePlanElement instanceof Leg
		}
	}

	public void output(String outputFilenameBase) {
		DistributionCreator distributionCreator = new DistributionCreator(
				pathSizes, 0.005);
		distributionCreator.createChartPercent(outputFilenameBase + "png",
				"Distribution of path-sizes", "path-size [0.5, 1.0]",
				"frequency of path-size");
		distributionCreator.writePercent(outputFilenameBase + "log");

		System.out.println("avg. pathSizes\t"
				+ SimpleStatistics.average(pathSizes));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = args[0], referencePopulationFilename = args[1], currentPopulationFilename = args[2], outputFilenameBase = args[3];

		Scenario referenceScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(referenceScenario).readFile(networkFilename);
		new MatsimPopulationReader(referenceScenario)
				.readFile(referencePopulationFilename);
		Population referencePopulation = referenceScenario.getPopulation();

		PathSizeSummaryFrom2Populations pssf2p = new PathSizeSummaryFrom2Populations(
				referencePopulation);

		Network network = referenceScenario.getNetwork();
		PathSizeFrom2Routes.setNetwork(network);

		Scenario currentScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((ScenarioImpl) currentScenario).setNetwork((NetworkImpl) network);
		// new MatsimNetworkReader(currentScenario).readFile(networkFilename);
		new MatsimPopulationReader(currentScenario)
				.readFile(currentPopulationFilename);
		Population currentPopulation = currentScenario.getPopulation();

		pssf2p.run(currentPopulation);
		pssf2p.output(outputFilenameBase);
	}

}
