/* *********************************************************************** *
 * project: org.matsim.*
 * InitRouteCreation.java
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

package playground.balmermi.census2000;

import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.NetworkWriter;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.XY2Links;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

public class InitRouteCreation {

	public static void createInitRoutes() {

		System.out.println("MATSim-IIDM: create initial routes.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading network xml file...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		Plans plans = new Plans(Plans.USE_STREAMING);
		PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new XY2Links(network));
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		plans.addAlgorithm(new PlansCalcRoute(network, timeCostCalc, timeCostCalc));
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		plansWriter.write();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing network xml file... ");
		NetworkWriter net_writer = new NetworkWriter(network);
		net_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();

		Gbl.createConfig(args);

		createInitRoutes();

		Gbl.printElapsedTime();
	}
}
