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

package playground.johannes.gsv.synPop.sim2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.DoubleSerializer;
import playground.johannes.gsv.synPop.io.IntegerSerializer;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.sim.ActivityLocationInitializer;
import playground.johannes.gsv.synPop.sim2.HamiltonianLogger;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
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
		parser.addSerializer(MIDKeys.PERSON_MUNICIPALITY_CLASS, IntegerSerializer.instance());
		parser.addSerializer(CommonKeys.PERSON_WEIGHT, DoubleSerializer.instance());
		parser.setValidating(false);
	
		logger.info("Loading persons...");
		parser.parse(config.findParam(MODULE_NAME, "popInputFile"));
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
		persons = PersonCloner.weightedClones(persons, Integer.parseInt(config.getParam(MODULE_NAME, "targetSize")), random);
		logger.info(String.format("Generated %s persons.", persons.size()));
		
		logger.info("Loading GIS data...");
		/*
		 * load facilities
		 */
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(config.getParam(MODULE_NAME, "facilities"));
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		int numThreads = Integer.parseInt(config.findParam(MODULE_NAME, "numThreads"));
		int logInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
		int dumpInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
		String outputDir = config.getParam(MODULE_NAME, "outputDir");
		long iters = (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));
		
		ActivityLocationMutatorFactory factory = new ActivityLocationMutatorFactory(facilities, "home", random);
		
		HamiltonianComposite h = new HamiltonianComposite();
		h.addComponent(new ActivityLocationHamiltonian(facilities), 100);
//		h.addComponent(new HFacilityCapacity(null, facilities), 0.00001);
		
		Sampler sampler = new Sampler(persons, h, factory, random);
		
		PopulationWriter popWriter = new PopulationWriter(outputDir);
		popWriter.setDumpInterval(1);
		
		SamplerListenerComposite listeners = new SamplerListenerComposite();
		listeners.addComponent(new BlockingSamplerListener(popWriter, dumpInterval));
		
		for(Hamiltonian hamil : h.getComponents()) {
			listeners.addComponent(new HamiltonianLogger(hamil, logInterval, outputDir));
		}
		listeners.addComponent(new SamplerLogger());
//		listeners.addComponent(new BlockingSamplerListener(new AnalyzerListener(facilities, outputDir), logInterval));
		sampler.setSamplerListener(listeners);
		/*
		 * initialize persons
		 */
		logger.info("Initializing persons...");
		List<Initializer> initializers = new ArrayList<Initializer>();
		
		ZoneLayer<Double> gemeinden = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popNuts3"), "EWZ");
		initializers.add(new ActivityLocationInitializer(facilities, gemeinden, "home", random));
		
		for(Initializer initializer : initializers) {
			for(ProxyPerson person : persons) {
				initializer.init(person);
			}
		}
		
		logger.info("Running sampler...");
		
		sampler.run(iters, numThreads);
		logger.info("Done.");
		
	}
	
	

}
