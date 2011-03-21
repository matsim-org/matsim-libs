/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
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

package playground.yu.newPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewSmallPlan extends NewPopulation {
	private double outputSample = 1d;

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewSmallPlan(final Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	public void setOutputSample(double outputSample) {
		this.outputSample = outputSample;
	}

	@Override
	public void run(Person person) {
		if (MatsimRandom.getRandom().nextDouble() < outputSample) {
			pw.writePerson(person);
		}
	}

	public static void main(final String[] args) {
		String netFilename, inputPopFilename, outputPopFilename;
		double outputSample;
		if (args.length != 4) {
			netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
			inputPopFilename = "../integration-parameterCalibration/test/watch/zrh/500.plansMivZrh30km4plansScore0unselected.xml.gz";
			outputPopFilename = "../integration-parameterCalibration/test/watch/zrh/MivZrh30km4plansScore0unselected0.01Mini.xml.gz";
			outputSample = 1d;
		} else {
			netFilename = args[0];
			inputPopFilename = args[1];
			outputPopFilename = args[2];
			outputSample = Double.parseDouble(args[3]);
		}
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) s.getNetwork();
		new MatsimNetworkReader(s).readFile(netFilename);

		Population population = s.getPopulation();
		new MatsimPopulationReader(s).readFile(inputPopFilename);

		NewSmallPlan nsp = new NewSmallPlan(network, population,
				outputPopFilename);
		nsp.setOutputSample(outputSample);
		nsp.run(population);
		nsp.writeEndPlans();
	}
}
