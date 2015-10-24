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

/**
 * 
 */
package playground.johannes.gsv.demand;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class PopulationGenerator {
	
	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);
	
	public static void main(String args[]) throws IOException {
		/*
		 * Read shape file with NUTS3 zones.
		 */
		Set<SimpleFeature> features = EsriShapeIO.readFeatures("/home/johannes/gsv/netz2030/data/raw/Zonierung_Kreise_WGS84_Stand2008Attr_WGS84_region.shp");
		
		Map<String, String> nuts2gsvMapping = readColumn("/home/johannes/gsv/netz2030/data/raw/inhabitants.csv", 1, 0);
		/*
		 * Read csv file with number of inhabitants per zone.
		 */
		Map<String, String> values = readColumn("/home/johannes/gsv/netz2030/data/raw/inhabitants.csv", 16);
		Map<String, Integer> inhabitants = new HashMap<String, Integer>();
		for(Map.Entry<String, String> entry : values.entrySet()) {
			inhabitants.put(entry.getKey(), Integer.parseInt(entry.getValue()));
		}
		
		/*
		 * Assign inhabitants to zones, multiply with sample size and calc sum.
		 */
		logger.info("Reading inhabitants file...");
		ZoneLayer<Integer> zoneLayer = readAttribut(features, inhabitants);
		multiplyAttribute(zoneLayer, 0.0001);
		long N = calculateSum(zoneLayer);
		
		/*
		 * Create N persons and add one empty plan each.
		 */
		logger.info("Creating persons...");
		Population pop = createPersons(N);
		addEmptyPlan(pop);
		
		/*
		 * Create home activities equally distribute in the zone.
		 */
		logger.info("Distributing persons...");
		PersonEqualDistribution task = new PersonEqualDistribution(zoneLayer);
		task.apply(pop);
		
		/*
		 * Creating primary activities...
		 */
		logger.info("Distribution primary activities...");
		Map<String, Integer> attracts = readAttractivities("/home/johannes/gsv/netz2030/data/raw/StrukturAttraktivitaet.csv", nuts2gsvMapping);
		zoneLayer = readAttribut(features, attracts);
//		zoneLayer.overwriteCRS(DefaultGeographicCRS.WGS84);
		zoneLayer.overwriteCRS(CRSUtils.getCRS(4326));
//		PlanPrimaryActivity task2 = new PlanPrimaryActivity(zoneLayer);
//		task2.apply(pop);
		
		/*
		 * Write population.
		 */
		logger.info("Writing persons...");
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write("/home/johannes/gsv/netz2030/data/population.xy.xml");
	}
	
	private static Map<String, String> readColumn(String file, int colIdx, int codeIdx) throws IOException {
		Map<String, String> values = new HashMap<String, String>();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";");
			values.put(tokens[codeIdx], tokens[colIdx]);
		}
		
		reader.close();
		
		return values;
	}
	
	private static Map<String, String> readColumn(String file, int colIdx) throws IOException {
		return readColumn(file, colIdx, 1);
	}
	
	private static void multiplyAttribute(ZoneLayer<Integer> zoneLayer, double factor) {
		for(Zone<Integer> zone : zoneLayer.getZones()) {
			zone.setAttribute((int) (zone.getAttribute() * factor));
		}
	}
	
	private static long calculateSum(ZoneLayer<Integer> zoneLayer) {
		long N = 0;
		for(Zone<Integer> zone : zoneLayer.getZones()) {
			N += zone.getAttribute();
		}
		return N;
	}
	
	public static ZoneLayer<Integer> readAttribut(Set<SimpleFeature> features, Map<String, Integer> values) {
		Set<Zone<Integer>> zones = new HashSet<Zone<Integer>>();
		
		for(SimpleFeature fearure : features) {
			String code = (String)fearure.getAttribute("NUTS3_CODE");
			Integer value = values.get(code);
			
			if(value != null) {
				Zone<Integer> zone = new Zone<Integer>(((Geometry) fearure.getDefaultGeometry()).getGeometryN(0));
				zone.setAttribute(value);
				zones.add(zone);
			}
			
		}
		
		return new ZoneLayer<Integer>(zones);
	}
	
	public static Population createPersons(long n) {
		
		Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for(long i = 0; i < n; i++) {
			Person p = pop.getFactory().createPerson(Id.create(i, Person.class));
			pop.addPerson(p);
		}
		
		return pop;
	}

	private static void addEmptyPlan(Population pop) {
		for(Person p : pop.getPersons().values()) {
			Plan plan = pop.getFactory().createPlan();
			p.addPlan(plan);
		}
	}
	
	private static Map<String, Integer> readAttractivities(String file, Map<String, String> codeMapping) throws IOException {
		Map<String, Integer> values = new HashMap<String, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			int sum = 0;
			for(int i = 2; i < 15; i++)
				sum += Integer.parseInt(tokens[i]);
			
			values.put(codeMapping.get(tokens[0]), sum);
		}
		
		reader.close();
		return values;
	}
}
