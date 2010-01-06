/* *********************************************************************** *
 * project: org.matsim.*
 * License.java
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
package playground.yu.analysis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * checks, how many people have license for driving car and can choose from car
 * or pt
 * 
 * @author ychen
 * 
 */
public class License extends AbstractPersonAlgorithm {
	private int hasLicenseCount;

	/**
	 *
	 */
	public License() {
		this.hasLicenseCount = 0;
	}

	@Override
	public void run(final Person person) {
		if (person != null)
			if (((PersonImpl) person).getLicense().equals("yes"))
				this.hasLicenseCount++;
	}

	/**
	 * @return the hasLicenseCount
	 */
	public int getHasLicenseCount() {
		return this.hasLicenseCount;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/input/_10pctZrhCarPtPlans_opt.xml.gz";

		Gbl.startMeasurement();

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		License l = new License();
		l.run(population);

		System.out.println("--> Done!\n-->There is " + l.getHasLicenseCount()
				+ " legal drivers!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
