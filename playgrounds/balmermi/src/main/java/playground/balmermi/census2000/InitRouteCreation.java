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

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;

public class InitRouteCreation {

	public static void createInitRoutes(Config config) {

		System.out.println("MATSim-IIDM: create initial routes.");

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading network xml file...");
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(null);//config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new XY2Links(network, null));
		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		plans.addAlgorithm(
				new PlanRouter(
				new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).get(
				) ) );
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {
		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);

		createInitRoutes(config);

		Gbl.printElapsedTime();
	}
}
