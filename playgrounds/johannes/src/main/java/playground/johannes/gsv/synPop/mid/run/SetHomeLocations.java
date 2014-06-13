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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.IntegerSerializer;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.HPersonMunicipality;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.hamiltonian.PopulationDensity;
import playground.johannes.gsv.synPop.sim.CompositeHamiltonian;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim.MutateHomeLocation;
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
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		long seed = 4711;
		String popFile = "/home/johannes/gsv/mid2008/pop.xml";
		String deZoneFile = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/de.nuts0.shp";
		
		XMLParser parser = new XMLParser();
//		parser.addSerializer(MIDKeys.PERSON_MUNICIPALITY_LOWER, IntegerSerializer.instance());
//		parser.addSerializer(MIDKeys.PERSON_MUNICIPALITY_UPPER, IntegerSerializer.instance());
		parser.addSerializer(MIDKeys.PERSON_MUNICIPALITY_CLASS, IntegerSerializer.instance());
		
		parser.setValidating(false);
	
		logger.info("Loading persons...");
		parser.parse(popFile);
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		logger.info("Cloning persons...");
		int fact = 2;
		Set<ProxyPerson> clones = new HashSet<ProxyPerson>(fact * persons.size());
		for(ProxyPerson person : persons) {
			for(int i = 0; i < fact; i++) {
				clones.add(person.clone());
			}
		}
		
		persons.addAll(clones);
		logger.info(String.format("Generated %s persons.", persons.size()));
		
		logger.info("Loading GIS data...");
		/*
		 * load DE boundaries
		 */
		Set<SimpleFeature> features = FeatureSHP.readFeatures(deZoneFile);
		SimpleFeature feature = features.iterator().next();
		Geometry zoneDE = ((Geometry) feature.getDefaultGeometry()).getGeometryN(0);
		/*
		 * load municipality inhabitants
		 */
		ZoneLayer<Double> municipalities = ZoneLayerSHP.read("/home/johannes/gsv/mid2008/Gemeinden.gk3.shp", "EWZ");
		/*
		 * load marktzellen inhabitants
		 */
//		ZoneLayer<Double> markzellen = ZoneLayerSHP.read("/home/johannes/gsv/synpop/data/gis/marktzellen/plz8.gk3.shp", "A_GESAMT");
		ZoneLayer<Double> markzellen = ZoneLayerSHP.read("/home/johannes/gsv/mid2008/Gemeinden.gk3.shp", "EWZ");
		double sum = 0;
		for(Zone<Double> zone : markzellen.getZones()) {
			sum += zone.getAttribute();
		}
		for(Zone<Double> zone : markzellen.getZones()) {
			zone.setAttribute(zone.getAttribute()/sum);
		}
		logger.info("Done.");
		
		logger.info("Setting up sampler...");
		Random random = new XORShiftRandom(seed);
		
		MutateHomeLocation mutator = new MutateHomeLocation(zoneDE, random);
		
		CompositeHamiltonian H = new CompositeHamiltonian();
//		H.addComponent(new HPersonMunicipality(municipalities));
		PopulationDensity popDen = new PopulationDensity(markzellen, persons.size(), random);
		H.addComponent(popDen);
		
		Sampler sampler = new Sampler(random);
		sampler.addMutator(mutator);
		sampler.addListenter(popDen);
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
		sampler.run(persons, 10000000);
		logger.info("Done.");
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/mid2008/pop2.xml", sampler.getPopulation());
		logger.info("Done.");
	}

}
