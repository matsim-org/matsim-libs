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

package playground.johannes.gsv.synPop.mid.run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.HPersonMunicipality;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.hamiltonian.PersonState;
import playground.johannes.gsv.synPop.mid.hamiltonian.PopulationDensity;
import playground.johannes.gsv.synPop.sim.CompositeHamiltonian;
import playground.johannes.gsv.synPop.sim.HFacilityCapacity;
import playground.johannes.gsv.synPop.sim.HamiltonianLogger;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim.MutateHomeActLocation;
import playground.johannes.gsv.synPop.sim.PopulationWriter;
import playground.johannes.gsv.synPop.sim.Sampler;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

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
		
		XMLParser parser = new XMLParser();
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
	
		ZoneLayer<Double> municipalities = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popGemd"), "EWZ");
		ZoneLayer<Double> popZone = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popNuts3"), "value");
		
		double sum = 0;
		for(Zone<Double> zone : popZone.getZones()) {
			sum += zone.getAttribute();
		}
		for(Zone<Double> zone : popZone.getZones()) {
			zone.setAttribute(zone.getAttribute()/sum);
		}
		
		ZoneLayer<Map<String, Object>> nuts1 = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "nuts1"));
		/*
		 * load facilities
		 */
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(config.getParam(MODULE_NAME, "facilities"));
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		
		MutateHomeActLocation mutator = new MutateHomeActLocation(facilities, random);
		
		CompositeHamiltonian H = new CompositeHamiltonian();
		HPersonMunicipality municp = new HPersonMunicipality(municipalities);
		H.addComponent(municp, 5);
		
		PopulationDensity popDen = new PopulationDensity(popZone, persons.size(), random);
		H.addComponent(popDen);
		
		HFacilityCapacity cap = new HFacilityCapacity("home", facilities);
		H.addComponent(cap, 0.0001);
		
		PersonState state = new PersonState(nuts1);
		H.addComponent(state, 10);
		
		Sampler sampler = new Sampler(random);
		
		String outputDir = config.getParam(MODULE_NAME, "outputDir");
		PopulationWriter popWriter = new PopulationWriter(outputDir, sampler);
		popWriter.setDumpInterval(Integer.parseInt(config.getParam(MODULE_NAME, "dumpInterval")));
		
		
		
		sampler.addMutator(mutator);		
		
		sampler.addListener(popDen);
		sampler.addListener(popWriter);
		/*
		 * add loggers
		 */
		int logInterval = 1000000;
		sampler.addListener(new HamiltonianLogger(popDen, logInterval, outputDir + "/popDen.log"));
		sampler.addListener(new HamiltonianLogger(municp, logInterval, outputDir + "/municip.log"));
		sampler.addListener(new HamiltonianLogger(cap, logInterval, outputDir + "/capacity.log"));
		sampler.addListener(new HamiltonianLogger(state, logInterval, outputDir + "/state.log"));
		
		sampler.setHamiltonian(H);
		
		/*
		 * initialize persons
		 */
		logger.info("Initializing persons...");
		List<Initializer> initializers = new ArrayList<Initializer>();
		initializers.add(mutator);
		initializers.add(popDen);
		
		for(Initializer initializer : initializers) {
			for(ProxyPerson person : persons) {
				initializer.init(person);
			}
		}
		
		popDen.writeZoneData(outputDir + "zones-start.shp");
		logger.info("Running sampler...");
		sampler.run(persons, (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations")));
		logger.info("Done.");
		popDen.writeZoneData(outputDir + "zones-end.shp");
	}
	
	

}
