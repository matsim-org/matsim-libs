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

package playground.johannes.gsv.synPop.invermo.sim;

import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.data.FacilityZoneValidator;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.sim3.*;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.processing.TaskRunner;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class SetHomeLocations {

	public static final Logger logger = Logger.getLogger(SetHomeLocations.class);
	
	private static final String MODULE_NAME = "popGenerator";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		ConfigUtils.loadConfig(config, args[0]);
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
	
		logger.info("Loading persons...");
		parser.parse(config.findParam(MODULE_NAME, "popInputFile"));
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		persons = PersonCloner.weightedClones(persons, Integer.parseInt(config.getParam(MODULE_NAME, "targetSize")), random);
		logger.info(String.format("Generated %s persons.", persons.size()));
		
//		logger.info("Loading data...");
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
		dataPool.register(new LandUseDataLoader(config.getModule(MODULE_NAME)), LandUseDataLoader.KEY);
//		logger.info("Done.");
		
		logger.info("Validation data...");
		FacilityZoneValidator.validate(dataPool, ActivityTypes.HOME, 3);
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		/*
		 * Assign facilities to the geocoded home locations.
		 */
		TaskRunner.run(new AssignHomeFacilities(dataPool), persons);
		/*
		 * Calculate the target population density.
		 */
		TaskRunner.run(new InitializeTargetDensity(dataPool), persons);
		/*
		 * Distribute population according to zone values.
		 */
		new InitHomeLocations(dataPool, random).apply(persons);
		/*
		 * Build a hamiltonian to evaluate the target population density
		 */
		HamiltonianComposite H = new HamiltonianComposite();
		PersonPopulationDenstiy persDen = new PersonPopulationDenstiy(dataPool);
		H.addComponent(persDen, 20);
		/*
		 * Build the move set and sampler
		 */
		SwitchHomeLocationFactory factory = new SwitchHomeLocationFactory(random);
		Sampler sampler = new Sampler(persons, H, factory, random);
		/*
		 * Build the listener
		 */
		/*
		 * need to copy home location user key to activity attributes
		 */
		long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
		int logInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
		int numThreads = Integer.parseInt(config.findParam(MODULE_NAME, "numThreads"));
		String outputDir = config.getParam(MODULE_NAME, "outputDir");
		
		SamplerListenerComposite dumpListener = new SamplerListenerComposite();
		dumpListener.addComponent(new CopyHomeLocations(dumpInterval));
		dumpListener.addComponent(new PopulationWriter(outputDir, dumpInterval));
		dumpListener.addComponent(new HamiltonianLogger(persDen, logInterval, outputDir));
		/*
		 * add loggers
		 */
		SamplerLogger slogger = new SamplerLogger();
		dumpListener.addComponent(slogger);
		
		sampler.setSamplerListener(new BlockingSamplerListener(dumpListener, dumpInterval, numThreads));
				
		logger.info("Running sampler...");
		long iters = (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));
		
		sampler.run(iters, numThreads);
		slogger.stop();
		logger.info("Done.");
	}
	
	

}
