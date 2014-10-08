/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim3;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class SetActivityLocations {

	public static final Logger logger = Logger.getLogger(SetActivityLocations.class);
	
	private static final String MODULE_NAME = "popGenerator";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		ConfigUtils.loadConfig(config, args[0]);
		
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
	
		parser.addToBlacklist("workLoc");
		parser.addToBlacklist("homeLoc");
		parser.addToBlacklist("homeCoord");
		parser.addToBlacklist("location");
		parser.addToBlacklist("coord");
		parser.addToBlacklist("state");
		parser.addToBlacklist("inhabClass");
		parser.addToBlacklist("index");
		parser.addToBlacklist("roundTrip");
		parser.addToBlacklist("origin");
		parser.addToBlacklist("purpose");
		parser.addToBlacklist("delete");
		
		logger.info("Loading persons...");
		parser.parse(config.findParam(MODULE_NAME, "popInputFile"));
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		persons = PersonCloner.weightedClones(persons, Integer.parseInt(config.getParam(MODULE_NAME, "targetSize")), random);
		logger.info(String.format("Generated %s persons.", persons.size()));
		
		logger.info("Loading data...");
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
//		dataPool.register(new LandUseDataLoader(config.getModule(MODULE_NAME)), LandUseDataLoader.KEY);
		logger.info("Done.");
		
//		logger.info("Validation data...");
//		FacilityZoneValidator.validate(dataPool, ActivityType.HOME, 3);
//		FacilityZoneValidator.validate(dataPool, ActivityType.HOME, 1);
//		logger.info("Done.");
		/*
		 * Assign random facilities to acts
		 */
		logger.info("Initializing activities...");
		ProxyTaskRunner.run(new InitActivitLocations(dataPool), persons);
		
		logger.info("Setting up sampler...");
		/*
		 * Build a hamiltonian to evaluate the target distance
		 */
		HamiltonianComposite H = new HamiltonianComposite();
		TargetDistanceHamiltonian distance = new TargetDistanceHamiltonian();
		H.addComponent(distance, 10000);
//		TargetDistanceAbsolute distance = new TargetDistanceAbsolute();
//		H.addComponent(distance, 1);
		/*
		 * Build the move set and sampler
		 */
		ActivityLocationMutatorFactory factory = new ActivityLocationMutatorFactory(dataPool, ActivityType.HOME, random);
		Sampler sampler = new Sampler(persons, H, factory, random);
		/*
		 * Build the listener
		 */
		SamplerListenerComposite lComposite = new SamplerListenerComposite();
		/*
		 * need to copy activity location user key to activity attributes
		 */
		int dumpInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
		int logInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
		lComposite.addComponent(new BlockingSamplerListener(new CopyFacilityUserData(), dumpInterval));
//		lComposite.addComponent(new BlockingSamplerListener(new CopyFacilityUserData(), logInterval));
		/*
		 * add a population writer
		 */
		String outputDir = config.getParam(MODULE_NAME, "outputDir");
		PopulationWriter popWriter = new PopulationWriter(outputDir);
		popWriter.setDumpInterval(1);
		lComposite.addComponent(new BlockingSamplerListener(popWriter, dumpInterval));
		/*
		 * add loggers
		 */
		
		lComposite.addComponent(new HamiltonianLogger(distance, logInterval, outputDir));
		lComposite.addComponent(new HamiltonianLogger(new TargetDistanceAbsolute(), logInterval, outputDir));
		lComposite.addComponent(new BlockingSamplerListener(new AnalyzerListener(dataPool, outputDir), dumpInterval));
		
		SamplerLogger slogger = new SamplerLogger();
		lComposite.addComponent(slogger);
		
		sampler.setSamplerListener(lComposite);

		logger.info("Running sampler...");
		long iters = (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));
		int numThreads = Integer.parseInt(config.findParam(MODULE_NAME, "numThreads"));
		sampler.run(iters, numThreads);
		slogger.stop();
		logger.info("Done.");
		
	}
}
