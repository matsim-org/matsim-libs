/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsimPlans2Zones
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.scripts.saspatzig;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;


/**
 * @author dgrether
 *
 */
public class DgMatsimPlans2Zones {
	
	private static final Logger log = Logger.getLogger(DgMatsimPlans2Zones.class);
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//### bvg project demand 
		String baseDir = DgPaths.REPOS + "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/scenario/";
		String zonesFile = DgPaths.REPOS + "shared-svn/studies/countries/eu/nuts_10m_2006/data/NUTS_RG_10M_2006.shp";
		String popFile = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/scenario/plans.times.xml.gz";
		CoordinateReferenceSystem popCrs = MGC.getCRS(TransformationFactory.DHDN_GK4);
//		String popFile = DgPaths.REPOS +  "shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-1.0sample/scenario/plans.times.xml.gz";
		String lkwIdPart = "lkw";
		String wvIdPart = "wv";
		boolean acceptAll = false;
//		String outFile = baseDir +  "plans.times.0.1sample.lkw.od.nuts.rg.10m.2006.txt";
		String outFile = baseDir +  "plans.times.0.1sample.wv.od.nuts.rg.10m.2006.txt";
//		String outFile = baseDir +  "plans.times.0.1sample.lkw.wv.od.nuts.rg.10m.2006.txt";
		//### end bvg project demand 
			
		//### satellic project demand 
		popFile = DgPaths.REPOS +  "shared-svn/studies/countries/de/berlin_prognose_2025/bb_gv_10pct.xml.gz";
		popCrs = MGC.getCRS(TransformationFactory.WGS84);
		acceptAll = true;
		outFile = DgPaths.REPOS +  "shared-svn/studies/countries/de/berlin_prognose_2025/bb.gv.10pct.od.nuts.rg.10m.2006.txt";
	//### end satellic project demand 

		
		
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(networkFile);
		config.plans().setInputFile(popFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		

		ShapeFileReader shpReader = new ShapeFileReader();
		Set<Feature> zones = shpReader.readFileAndInitialize(zonesFile);
		CoordinateReferenceSystem zonesCrs = shpReader.getCoordinateSystem();
		
		MathTransform transformation = CRS.findMathTransform(popCrs, zonesCrs, true);

		Map<String, Map<String, Integer>> fromToMap = new HashMap<String, Map<String, Integer>>();
		int numberOfTrips = 0;
		Population pop = scenario.getPopulation();
		for (Person person : pop.getPersons().values()){
			if (acceptAll || 
					//person.getId().toString().contains(lkwIdPart) ) {
//|| 
					person.getId().toString().contains(wvIdPart)){
				Activity act1 = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				Activity act2 = (Activity) person.getSelectedPlan().getPlanElements().get(2);
				Coordinate a1c = MGC.coord2Coordinate(act1.getCoord());
				Coordinate a2c = MGC.coord2Coordinate(act2.getCoord());
				a1c = JTS.transform(a1c, a1c, transformation);
				a2c = JTS.transform(a2c, a2c, transformation);
				Feature featureFrom = findFeature(zones, a1c);
				Feature featureTo = findFeature(zones, a2c);
				if (featureFrom == null || featureTo == null){
					log.warn("At least one of the features is not in nuts area!");
					continue;
				}
				//NUTS_ID
				String  fromId = (String) featureFrom.getAttribute("NUTS_ID");
				String toId = (String) featureTo.getAttribute("NUTS_ID");
				log.debug("found od pair: " + fromId + " to " + toId);
				if (! fromToMap.containsKey(fromId)){
					fromToMap.put(fromId, new HashMap<String, Integer>());
				}
				Map<String, Integer> fromMap = fromToMap.get(fromId);
				if (! fromMap.containsKey(toId)){
					fromMap.put(toId, 1);
				}
				else {
					Integer count = fromMap.get(toId);
					fromMap.put(toId, count + 1);
				}
				numberOfTrips++;
			}
		}
		
		log.info("found " + numberOfTrips + " trips");
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		writer.write("from_region \t to_region \t number_trips");
		writer.newLine();
		for (Entry<String, Map<String, Integer>> fromEntry : fromToMap.entrySet()){
			for (Entry<String, Integer> toEntry : fromEntry.getValue().entrySet()){
				writer.write(fromEntry.getKey() + "\t" + toEntry.getKey() + "\t" + toEntry.getValue());
				writer.newLine();
			}
		}
		writer.close();
	}

	private static Feature findFeature(Set<Feature> zones, Coordinate coordinate) {
		Point point = MGC.geoFac.createPoint(coordinate);
		for (Feature f : zones){
			//STAT_LEVL_
			Integer level =  (Integer) f.getAttribute("STAT_LEVL_");
			if (level.equals(3) &&  f.getDefaultGeometry().contains(point)) {
				return f;
			}
		}
		//if we haven't found something for nuts level 3 we look for nuts level 2
		for (Feature f : zones){
			Integer level =  (Integer) f.getAttribute("STAT_LEVL_");
			if (level.equals(2) &&  f.getDefaultGeometry().contains(point)) {
				return f;
			}
		}
		for (Feature f : zones){
			Integer level =  (Integer) f.getAttribute("STAT_LEVL_");
			if (level.equals(1) &&  f.getDefaultGeometry().contains(point)) {
				return f;
			}
		}
		log.warn("Cannot find feature for coordinate: " + coordinate);
		return null;
	}

}
