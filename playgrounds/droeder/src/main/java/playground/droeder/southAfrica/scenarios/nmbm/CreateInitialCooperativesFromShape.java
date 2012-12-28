/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.scenarios.nmbm;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class CreateInitialCooperativesFromShape {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(CreateInitialCooperativesFromShape.class);
	private static String SHP;
	private static String NETWORK;
	private static String OUTSCHEDULE;
	private static String INSCHEDULE;
	

	private CreateInitialCooperativesFromShape() {
	}
	
	public static void main(String[] args) {
		Map<Id, Geometry> geometries = redShapeFile(SHP);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(NETWORK);
		sc.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(sc).readFile(INSCHEDULE);
		
		TransitSchedule sched = createSchedule(geometries, sc.getNetwork(), sc.getTransitSchedule());
		new TransitScheduleWriter(sched).writeFile(OUTSCHEDULE);
	}
	
	private static TransitSchedule createSchedule(Map<Id, Geometry> geometries,	Network network2, TransitSchedule transitSchedule) {
		TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		
		for(Entry<Id, Geometry> g: geometries.entrySet()){
//			schedule.ad
		}
		
		return schedule;
	}

	static Map<Integer, Set<String>> values = new HashMap<Integer, Set<String>>();
	
	private static Map<Id, Geometry> redShapeFile(String shapeFile){
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(shapeFile);
		
		Map<Id, Geometry> geometries = new HashMap<Id, Geometry>();
		for(SimpleFeature f: reader.getFeatureSet()){
			Geometry g = (Geometry) f.getAttribute(0); // should be the Geometry
			Id id = new IdImpl( (Long) f.getAttribute(1));
			geometries.put(id, g);
		}
		return geometries;
	}
}

