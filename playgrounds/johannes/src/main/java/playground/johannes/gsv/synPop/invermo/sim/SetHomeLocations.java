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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim2.HamiltonianComposite;
import playground.johannes.gsv.synPop.sim2.HamiltonianLogger;
import playground.johannes.gsv.synPop.sim2.PopulationWriter;
import playground.johannes.gsv.synPop.sim2.SamplerListenerComposite;
import playground.johannes.gsv.synPop.sim2.SamplerLogger;
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
	
//		ZoneLayer<Double> municipalities = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popGemd"), "EWZ");
		ZoneLayer<Double> popZone = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popNuts3"), "value");
		ZoneLayer<Double> popDensity = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "popNuts3"), "value");
		
		for(Zone<Double> zone : popDensity.getZones()) {
			double inhabs = zone.getAttribute();
			double a = zone.getGeometry().getArea();
			zone.setAttribute(inhabs/a);
		}
		
		double sum = 0;
		for(Zone<Double> zone : popZone.getZones()) {
			sum += zone.getAttribute();
		}
		for(Zone<Double> zone : popZone.getZones()) {
			zone.setAttribute(zone.getAttribute()/sum);
		}
		
//		ZoneLayer<Map<String, Object>> nuts1 = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "nuts1"));
		/*
		 * load facilities
		 */
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(config.getParam(MODULE_NAME, "facilities"));
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		Set<ActivityFacility> remove = new HashSet<>();
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			Coord coord = facility.getCoord();
			Zone<?> zone = popDensity.getZone(MatsimCoordUtils.coordToPoint(coord));
			if(zone == null) {
				remove.add(facility);
			}
		}
		logger.warn(String.format("Removing %s of %s facilities because the can not be assigned to a zone.", remove.size(), facilities.getFacilities().size()));
		for(ActivityFacility facility : remove) {
			facilities.getFacilities().remove(facility.getId());
		}
		
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		
		FacilityData rFacilities = FacilityData.getInstance(facilities, random);
		
		HamiltonianComposite H = new HamiltonianComposite();
		PersonPopulationDenstiy persDen = null;//new PersonPopulationDenstiy(popDensity);
		H.addComponent(persDen, 10000);
		
//		PopulationDensity popDen = new PopulationDensity(persons, popZone, persons.size(), random); 
//		H.addComponent(popDen, 1000);
		
//		HFacilityCapacity cap = new HFacilityCapacity("home", facilities);
//		H.addComponent(cap, 0.0001);
		
//		MutatorFactory factory = new StartLocationMutatorFactory(rFacilities, random);
//		MutatorFactory factory = new SwitchHomeLocFactory();
		MutatorFactory factory = new MutateHomeLocFactory(rFacilities, random);
		Sampler sampler = new Sampler(persons, H, factory, random);
		
		SamplerListenerComposite lComposite = new SamplerListenerComposite();
		
		String outputDir = config.getParam(MODULE_NAME, "outputDir");
		PopulationWriter popWriter = new PopulationWriter(outputDir);
		int dumpInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
		popWriter.setDumpInterval(dumpInterval);
		
		lComposite.addComponent(new CopyHomeLocations(dumpInterval));
		lComposite.addComponent(popWriter);
		/*
		 * add loggers
		 */
		int logInterval = (int) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
//		lComposite.addComponent(popDen);
//		lComposite.addComponent(new HamiltonianLogger(popDen, logInterval, outputDir));
		lComposite.addComponent(new HamiltonianLogger(persDen, logInterval, outputDir));
//		lComposite.addComponent(new HamiltonianLogger(cap, logInterval, outputDir + "/capacity.log"));
		lComposite.addComponent(new SamplerLogger());
		
		sampler.setSamplerListener(lComposite);
		/*
		 * initialize persons
		 */
		logger.info("Initializing persons...");
		List<Initializer> initializers = new ArrayList<Initializer>();
		initializers.add(new SetMissingActTypes());
		initializers.add(new InitializeFacilities(rFacilities));
//		initializers.add(new InitializeStartLocation());
//		initializers.add(new InitializeTargetDensity(popDensity));
//		initializers.add(new InitHomeLocations(persons, popZone, rFacilities, random));
		
		
		for(Initializer initializer : initializers) {
			for(ProxyPerson person : persons) {
				initializer.init(person);
			}
		}
		
		new InitHomeLocations(persons, popZone, rFacilities, random);
		
//		popDen.initializeZones();
//		popDen.writeZoneData(outputDir + "zones-start.shp");
		logger.info("Running sampler...");
		long iters = (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations"));
		int numThreads = Integer.parseInt(config.findParam(MODULE_NAME, "numThreads"));
		sampler.run(iters, numThreads);
		logger.info("Done.");
//		popDen.writeZoneData(outputDir + "zones-end.shp");
	}
	
	

}
