/* *********************************************************************** *
 * project: org.matsim.*
 * BlurPlanTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.timeblur;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;


public class BlurPlanTimes {

	public static void run(final String inputPlansFile, final String inputNetworkFile, final String outputPlansFile, int mutationRange) {
		Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputNetworkFile);
		PopulationImpl population = new PopulationImpl();
		population.setIsStreaming(true);

		PersonBlurTimes pbt = new PersonBlurTimes(mutationRange);
		population.addAlgorithm(pbt);

		PopulationWriter pw = new PopulationWriter(population,outputPlansFile);
		population.addAlgorithm(pw);

		new MatsimPopulationReader(population, network).readFile(inputPlansFile);
		population.printPlansCount();
		pw.writeEndPlans();
	}

	/**
	 * @param args input-population-file, network-file, output-population-file, mutationRange
	 */
	public static void main(final String[] args) {
		if (args.length == 4) {
			BlurPlanTimes.run(args[0], args[1], args[2], Integer.parseInt(args[3]));
		} else {
			System.err.println("This program expected 4 arguments:");
			System.err.println(" - input-population-file");
			System.err.println(" - input-network-file");
			System.err.println(" - output-population-file");
			System.err.println(" - mutationRange = [1...] (in seconds, i.e. 1800 means mutation of +/- 30 min)");
		}
	}
}
