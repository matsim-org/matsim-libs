/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateDemand.java
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

package playground.meisterk.org.matsim.run.portland;

import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.world.World;

public class GenerateDemand {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		Config config = Gbl.createConfig(args);
		GenerateDemand.generateDemand(config);

	}

	private static void generateDemand(Config config) {

		World world = Gbl.createWorld();

		System.out.println("Reading network...");
		NetworkLayer networkLayer = new NetworkLayer();
		new MatsimNetworkReader(networkLayer).readFile(config.network().getInputFile());
		world.setNetworkLayer(networkLayer);
		System.out.println("Reading network...done.");

		System.out.println("Reading facilities...");
		ActivityFacilitiesImpl facilityLayer = new ActivityFacilitiesImpl();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilityLayer);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(config.facilities().getInputFile());
		facilityLayer.printFacilitiesCount();
		world.setFacilityLayer(facilityLayer);
		world.complete();
		System.out.println("Reading facilities...done.");

		System.out.println("Setting up plans objects...");
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		plansWriter.startStreaming(config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(plans, networkLayer);
		System.out.println("Setting up plans objects...done.");

		System.out.println("Setting up person modules...");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		plans.addAlgorithm(new PlansCalcRoute(networkLayer, timeCostCalc, timeCostCalc));
		System.out.println("Setting up person modules...done.");

		System.out.println("Reading, processing and writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		System.out.println("Reading, processing and writing plans...done.");

	}

}
