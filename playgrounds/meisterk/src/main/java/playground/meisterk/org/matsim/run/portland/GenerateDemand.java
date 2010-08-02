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

import org.matsim.api.core.v01.ScenarioImpl;
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

		ScenarioImpl scenario = new ScenarioImpl(config);
		World world = scenario.getWorld();

		System.out.println("Reading network...");
		NetworkLayer networkLayer = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		System.out.println("Reading network...done.");

		System.out.println("Reading facilities...");
		ActivityFacilitiesImpl facilityLayer = scenario.getActivityFacilities();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(config.facilities().getInputFile());
		facilityLayer.printFacilitiesCount();
		world.complete(null);
		System.out.println("Reading facilities...done.");

		System.out.println("Setting up plans objects...");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans, networkLayer);
		plansWriter.startStreaming(config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("Setting up plans objects...done.");

		System.out.println("Setting up person modules...");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		plans.addAlgorithm(new PlansCalcRoute(config.plansCalcRoute(), networkLayer, timeCostCalc, timeCostCalc));
		System.out.println("Setting up person modules...done.");

		System.out.println("Reading, processing and writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		System.out.println("Reading, processing and writing plans...done.");

	}

}
