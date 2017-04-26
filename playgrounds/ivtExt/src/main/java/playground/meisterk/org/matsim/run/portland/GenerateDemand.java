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

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class GenerateDemand {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {

		Config config = ConfigUtils.loadConfig(args[0]);
		GenerateDemand.generateDemand(config);

	}

	private static void generateDemand(Config config) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		System.out.println("Reading network...");
		Network networkLayer = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		System.out.println("Reading network...done.");

		System.out.println("Reading facilities...");
		ActivityFacilities facilityLayer = scenario.getActivityFacilities();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(config.facilities().getInputFile());
		System.out.println("Reading facilities...done.");

		System.out.println("Setting up plans objects...");
//		Population reader = (Population) scenario.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		StreamingDeprecated.setIsStreaming(reader, true);
		StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		plansWriter.startStreaming(null);//config.plans().getOutputFile());
//		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("Setting up plans objects...done.");

		System.out.println("Setting up person modules...");
		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		reader.addAlgorithm(new PlanRouter(
		new TripRouterFactoryBuilderWithDefaults().build(
				scenario ).get(
		) ));
		System.out.println("Setting up person modules...done.");

		System.out.println("Reading, processing and writing plans...");
		final PersonAlgorithm algo = plansWriter;
		reader.addAlgorithm(algo);
//		plansReader.readFile(config.plans().getInputFile());
		reader.readFile(config.plans().getInputFile());
		PopulationUtils.printPlansCount(reader) ;
		plansWriter.closeStreaming();
		System.out.println("Reading, processing and writing plans...done.");

	}

}
