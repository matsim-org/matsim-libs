/* *********************************************************************** *
 * project: org.matsim.*
 * PopLinksMatrixCreater.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.linkUtilOffset.dayVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;
import Jama.Matrix;

/**
 * @author yu
 * 
 */
public class PopLinksMatrixCreator extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private Network network;
	private static double TIME_BIN = 86400d;// a whole day
	/**
	 * matrix[mxn] m-number of pop, n-number of links
	 */
	private Matrix matrix;
	private Map<Id, Integer> linkSequence = new HashMap<Id, Integer>();
	private int personCnt = 0;

	/**
	 * 
	 */
	public PopLinksMatrixCreator(Network network, Population population) {
		this.network = network;
		Map<Id, Link> linkMap = (Map<Id, Link>) this.network.getLinks();
		int i = 0;
		for (Link link : linkMap.values())
			this.linkSequence.put(link.getId(), i++);// 1.transfers value, 2.++

		System.out.println(this.linkSequence);// also the squence for X_n [one
		// day version]
		matrix = new Matrix(population.getPersons().size(), linkMap.size()/*
																		 * [timeBin
																		 * ==one
																		 * day
																		 * version
																		 * ]
																		 */, 0d);
	}

	/**
	 * Man should use this contructor only when the used timeBin != one day
	 * 
	 * @param network
	 * @param timeBin
	 */
	public PopLinksMatrixCreator(Network network, Population population,
			double timeBin) {
		this(network, population);
		TIME_BIN = timeBin;
	}

	public static double getTIME_BIN() {
		return TIME_BIN;
	}

	@Override
	public void run(Person person) {// timeBin==one day version
		run(person.getSelectedPlan());
		this.personCnt++;
	}

	public void run(Plan plan) {// timeBin==a day version
		List<PlanElement> pes = plan.getPlanElements();
		for (int i = 1; i < pes.size(); i += 2) {// Leg
			Leg leg = (Leg) pes.get(i);
			if (leg.getMode().equals(TransportMode.car)) {
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				int endLinkIdx = this.linkSequence.get(route.getEndLinkId());// routeEndLinkIdx
				// in
				// array
				this.matrix.set(this.personCnt, endLinkIdx, this.matrix.get(
						this.personCnt, endLinkIdx) + 1d);
				for (Id linkId : route.getLinkIds()) {
					int linkIdx = this.linkSequence.get(linkId);// routeLinkIdx
					// in array
					this.matrix.set(this.personCnt, linkIdx, this.matrix.get(
							this.personCnt, linkIdx) + 1d);
				}
			}
		}
	}

	public void writeMatrix(String matrixOutputFilename) {
		SimpleWriter writer = new SimpleWriter(matrixOutputFilename);
		writer.write("\tcol_no.");
		for (int n = 0; n < this.matrix.getColumnDimension(); n++)
			writer.write("\t" + n);
		writer.writeln("\nrow_no.");
		for (int m = 0; m < this.matrix.getRowDimension(); m++) {
			writer.write(m + "\t");
			for (int n = 0; n < this.matrix.getColumnDimension(); n++) {
				writer.write("\t" + this.matrix.get(m, n));
			}
			writer.writeln();
		}
		writer.close();
	}

	public Matrix getMatrix() {
		return matrix;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml", // 
		populationFilename = "../integration-demandCalibration1.0.1/test/output/prepare/ITERS/it.100/100.plans.xml.gz", //
		matrixOutputFilename = "../integration-demandCalibration1.0.1/test/output/prepare/popLinksMatrix.log";

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		PopLinksMatrixCreator plmc = new PopLinksMatrixCreator(network,
				population);
		plmc.run(population);
		plmc.writeMatrix(matrixOutputFilename);

	}

}
