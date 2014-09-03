///* *********************************************************************** *
// * project: org.matsim.*
// * PreparePopulation.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2013 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.christoph.dissertation;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.population.PopulationImpl;
//import org.matsim.core.population.PopulationReader;
//import org.matsim.core.population.PopulationWriter;
//import org.matsim.core.population.PopulationWriterHandlerImplV4;
//import org.matsim.core.scenario.ScenarioUtils;
//
//import playground.christoph.evacuation.population.RemoveUnselectedPlans;
//import playground.christoph.population.RemoveRoutes;
//
//public class PrepareSwitzerlandPopulation {
//
//	final private static Logger log = Logger.getLogger(PrepareSwitzerlandPopulation.class);
//
//	/**
//	 * Input arguments:
//	 * <ul>
//	 *	<li>path to population file</li>
//	 *  <li>path to network file</li>
//	 *  <li>path to facilities file</li>
//	 *  <li>path to population output file</li>
//	 * </ul>
//	 */
//	public static void main(String[] args) {
//		if (args.length != 4) return;
//
//		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(args[1]);
//		config.facilities().setInputFile(args[2]);
//
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//
//		String populationInputFile = args[0];
//		String populationOutputFile = args[3];
//
//		new PrepareSwitzerlandPopulation(scenario, populationInputFile, populationOutputFile);
//	}
//
//	public PrepareSwitzerlandPopulation(Scenario scenario, String populationInFile, String populationOutFile) {
//
//		log.info("Reading, processing, writing plans...");
//		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
//
//		population.setIsStreaming(true);
//
//		// still use V4 writer which supports desires and knowledges
//		PopulationWriter populationWriter = new PopulationWriter(population, scenario.getNetwork());
//        Knowledges result;
//        throw new RuntimeException("Knowledges are no more.");
//
//        populationWriter.setWriterHandler(new PopulationWriterHandlerImplV4(scenario.getNetwork(), result));
//		populationWriter.startStreaming(populationOutFile);
//
//		population.addAlgorithm(new RemoveUnselectedPlans());
//		population.addAlgorithm(new RemoveRoutes());
//		population.addAlgorithm(populationWriter);
//
//		PopulationReader plansReader = new MatsimPopulationReader(scenario);
//		plansReader.readFile(populationInFile);
//
//		population.printPlansCount();
//		populationWriter.closeStreaming();
//		log.info("done.");
//	}
//
//}
