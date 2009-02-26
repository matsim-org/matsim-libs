/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.plansToPlans;

import java.io.PrintWriter;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;

/**
 * @author kn after mrieser
 */
public class MyPlansToPlans {

	private Config config;
	private final String configfile = null;
	private final String dtdfile = null;

	public void run(final String[] args) {
		this.config = Gbl.createConfig(new String[]{"../padang/dlr-network/pconfig.xml"});
		ConfigWriter configwriter = new ConfigWriter(this.config, new PrintWriter(System.out));
		configwriter.write();

//		final World world = Gbl.getWorld();
//
//		if (this.config.world().getInputFile() != null) {
//			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
//			worldReader.readFile(this.config.world().getInputFile());
//		}

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		final Population plans = new PopulationImpl(PopulationImpl.USE_STREAMING);
		final PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		final PopulationWriter plansWriter = new PopulationWriter(plans);
//		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));
		plans.addAlgorithm(plansWriter); // planswriter must be the last algorithm added
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		MyPlansToPlans app = new MyPlansToPlans();
		app.run(args);
	}

}
