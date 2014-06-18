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
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.DoubleSerializer;
import playground.johannes.gsv.synPop.io.IntegerSerializer;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.hamiltonian.PopulationDensity;
import playground.johannes.gsv.synPop.sim.CompositeHamiltonian;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim.MutateHomeLocation;
import playground.johannes.gsv.synPop.sim.PopulationWriter;
import playground.johannes.gsv.synPop.sim.Sampler;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import com.vividsolutions.jts.geom.Geometry;

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
		 * load DE boundaries
		 */
		Set<SimpleFeature> features = FeatureSHP.readFeatures(config.getParam(MODULE_NAME, "deBoundary"));
		SimpleFeature feature = features.iterator().next();
		Geometry zoneDE = ((Geometry) feature.getDefaultGeometry()).getGeometryN(0);
		/*
		 * load municipality inhabitants
		 */
//		ZoneLayer<Double> municipalities = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "gemeinden"), "EWZ");
		/*
		 * load marktzellen inhabitants
		 */
//		ZoneLayer<Double> markzellen = ZoneLayerSHP.read("/home/johannes/gsv/synpop/data/gis/marktzellen/plz8.gk3.shp", "A_GESAMT");
//		ZoneLayer<Double> markzellen = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "marktzellen"), "A_GESAMT");
		ZoneLayer<Double> markzellen = ZoneLayerSHP.read(config.findParam(MODULE_NAME, "marktzellen"), "EWZ");
		double sum = 0;
		for(Zone<Double> zone : markzellen.getZones()) {
			sum += zone.getAttribute();
		}
		for(Zone<Double> zone : markzellen.getZones()) {
			zone.setAttribute(zone.getAttribute()/sum);
		}
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		
		
		MutateHomeLocation mutator = new MutateHomeLocation(zoneDE, random);
		
		CompositeHamiltonian H = new CompositeHamiltonian();
//		H.addComponent(new HPersonMunicipality(municipalities));
		PopulationDensity popDen = new PopulationDensity(markzellen, persons.size(), random);
		H.addComponent(popDen);
		
		Sampler sampler = new Sampler(random);
		
		PopulationWriter popWriter = new PopulationWriter(config.getParam(MODULE_NAME, "outputDir"), sampler);
		popWriter.setDumpInterval(Integer.parseInt(config.getParam(MODULE_NAME, "dumpInterval")));
		
		
		sampler.addMutator(mutator);
		sampler.addListenter(popDen);
		sampler.addListenter(popWriter);
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
		
		logger.info("Running sampler...");
		sampler.run(persons, (long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations")));
		logger.info("Done.");
		
//		popDen.writeZoneData("/home/johannes/gsv/mid2008/popDen.shp");
	}

}
