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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.Route2GeoDistFunction;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.mid.run.ConcurrentProxyTaskRunner;
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

		XMLHandler parser = new XMLHandler(new PlainFactory());
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
		parser.readFile(config.findParam(MODULE_NAME, "popInputFile"));
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));

		logger.info("Replacing activity types...");
		TaskRunner.run(new ReplaceActTypes(), persons);

		logger.info("Calculating geo distances from route distances...");
		double A = Double.parseDouble(config.getParam(MODULE_NAME, "A"));
		double alpha = Double.parseDouble(config.getParam(MODULE_NAME, "alpha"));
		double min = Double.parseDouble(config.getParam(MODULE_NAME, "min"));
		TaskRunner.run(new Route2GeoDistance(new Route2GeoDistFunction(A, alpha, min)), persons);

		logger.info("Truncating distances...");
		TaskRunner.run(new TruncateDistances(1000000), persons);
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		persons = PersonCloner.weightedClones(persons, Integer.parseInt(config.getParam(MODULE_NAME, "targetSize")), random);
		logger.info(String.format("Generated %s persons.", persons.size()));

		logger.info("Registering data loaders...");
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), null, random), FacilityDataLoader.KEY);
		/*
		 * Assign random facilities to acts
		 */
		logger.info("Initializing activities...");
		int numThreads = Integer.parseInt(config.findParam(MODULE_NAME, "numThreads"));
		ConcurrentProxyTaskRunner.run(new InitHomeBasedActLocsFactory(dataPool, random), persons, numThreads);

		logger.info("Setting up sampler...");
		/*
		 * Build a hamiltonian to evaluate the target distance
		 */
		boolean weighted = Boolean.parseBoolean(config.getParam(MODULE_NAME, "weighted"));
		HamiltonianComposite H = new HamiltonianComposite();
		TargetDistanceHamiltonian distance = new TargetDistanceHamiltonian(weighted);
		H.addComponent(distance, 10000);
		// TargetDistanceAbsolute distance = new TargetDistanceAbsolute();
		// H.addComponent(distance, 1);
		/*
		 * Build the move set and sampler
		 */
		ActivityLocationMutatorFactory factory = new ActivityLocationMutatorFactory(dataPool, ActivityTypes.HOME, random);
		Sampler sampler = new Sampler(persons, H, factory, random);
		/*
		 * Build the listener
		 */
		SamplerListenerComposite listener = new SamplerListenerComposite();
		String outputDir = config.getParam(MODULE_NAME, "outputDir");

		long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
		long logInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));

		if (dumpInterval % logInterval != 0) {
			throw new RuntimeException("The dump interval needs to be a multiple of the log interval.");
		}
		/*
		 * add loggers
		 */
		listener.addComponent(new HamiltonianLogger(distance, logInterval, outputDir));
		listener.addComponent(new HamiltonianLogger(new TargetDistanceAbsolute(), logInterval, outputDir));
		/*
		 * put the population writer and analyzer into a separate composite to
		 * ensure the CopyFacilityUserData is always called before dumping and
		 * analysis.
		 */
		// SamplerListenerComposite dumpListener = new
		// SamplerListenerComposite();
		listener.addComponent(new CopyFacilityUserData(dumpInterval));
		listener.addComponent(new PopulationWriter(outputDir, dumpInterval));
		listener.addComponent(new AnalyzerListener(dataPool, outputDir, dumpInterval));
		listener.addComponent(new ErrorTargetDistanceLogger(distance, logInterval, outputDir));
		/*
		 * need to copy activity location user key to activity attributes
		 */
		// lComposite.addComponent(new BlockingSamplerListener(dumpListener,
		// dumpInterval, numThreads));
		// lComposite.addComponent(new BlockingSamplerListener(new
		// AnalyzerListener(dataPool, outputDir), dumpInterval, numThreads));

		SamplerLogger slogger = new SamplerLogger();
		listener.addComponent(slogger);

		sampler.setSamplerListener(new BlockingSamplerListener(listener, logInterval, numThreads));

		String val = config.findParam(MODULE_NAME, "distanceStratification");
		boolean dStrat = false;
		if (val != null) {
			dStrat = Boolean.parseBoolean(val);
		}
		if (dStrat) {
			double threshold = Double.parseDouble(config.getParam(MODULE_NAME, "threshold"));
			sampler.setSegmenter(new DistancePopSegmenter(threshold));
//			sampler.setSegmenter(new LongDistSegmenter(threshold));
		}

		logger.info("Running sampler...");
		long iters = (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));

		sampler.run(iters, numThreads);
		slogger.stop();
		logger.info("Done.");

	}
}
