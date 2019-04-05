/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package vwExamples.utils.delays;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import analysis.drtOccupancy.DynModeTripsAnalyser;

public class RunTravelDelayAnalysisBatch {

	static Set<Id<Person>> relevantAgents = new HashSet<>();
	static Map<String, Geometry> zoneMap = new HashMap<>();
	static Geometry boundary;
	static Set<String> zones = new HashSet<>();
	static String shapeFile = "D:\\\\Matsim\\\\Axer\\\\Hannover\\\\ZIM\\\\input\\\\shp\\\\Real_Region_Hannover.shp";
	static String shapeFeature = "NO";
	static List<Geometry> districtGeometryList = new ArrayList<Geometry>();
	static GeometryFactory geomfactory = JTSFactoryFinder.getGeometryFactory(null);
	static GeometryCollection geometryCollection = geomfactory.createGeometryCollection(null);

	public static void main(String[] args) {

		String runDir = "D:\\Matsim\\Axer\\Hannover\\ZIM\\output\\";
//		String runId = "vw219_netnet150_veh_idx0.";

		readShape(shapeFile, shapeFeature);
		getResearchAreaBoundary();

		File[] directories = new File(runDir).listFiles(File::isDirectory);
		for (File scenarioDir : directories) {
			relevantAgents.clear();

			String[] StringList = scenarioDir.toString().split("\\\\");
			String scenarioName = StringList[StringList.length - 1];

			StreamingPopulationReader spr = new StreamingPopulationReader(
					ScenarioUtils.createScenario(ConfigUtils.createConfig()));
			spr.addAlgorithm(new PersonAlgorithm() {
				@Override
				public void run(Person person) {
					// relevantAgents.add(person.getId());

					//01: Case for Commuter
//					if (livesOutside(person.getSelectedPlan(), zoneMap)
//							&& worksInside(person.getSelectedPlan(), zoneMap)) {
//						relevantAgents.add(person.getId());
//					}
					
					//02: HousholdSurvery (Inhabitants)
//					if (livesInside(person.getSelectedPlan(), zoneMap)) {
//						relevantAgents.add(person.getId());
//					}

				}

			});
			spr.readFile(scenarioDir + "\\" + scenarioName + ".output_plans.xml.gz");

			Network network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(scenarioDir + "\\" + scenarioName + ".output_network.xml.gz");
			TravelDelayCalculator tdc = new TravelDelayCalculator(network,boundary);

			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(tdc);
			new MatsimEventsReader(events).readFile(scenarioDir + "\\" + scenarioName + ".output_events.xml.gz");
			DynModeTripsAnalyser.collection2Text(tdc.getTrips(), scenarioDir + "\\" + scenarioName + ".delay.csv",
					"PersonId;ArrivalTime;FreespeedTravelTime;ActualTravelTime;Delay;Beeline;Flag;Mileage_m");

		}

	}
	
	public static void getResearchAreaBoundary() {
		// This class infers the geometric boundary of all network link
		
		for (Geometry zoneGeom : zoneMap.values()) {
			districtGeometryList.add(zoneGeom);
		}

		geometryCollection = (GeometryCollection) geomfactory.buildGeometry(districtGeometryList);
		boundary = geometryCollection.union();

	}
	

	public static void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}

	public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		for (String zone : zoneMap.keySet()) {
			Geometry geometry = zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return true;
			}
		}

		return false;
	}

	public static boolean livesOutside(Plan plan, Map<String, Geometry> zoneMap) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().contains("home")) {

					Activity activity = ((Activity) pe);
					Coord coord = activity.getCoord();
					// If home is not inside zoneMap return true
					if (!isWithinZone(coord, zoneMap)) {
						return true;
					}

				}
			}
		}
		return false;
	}
	
	
	public static boolean livesInside(Plan plan, Map<String, Geometry> zoneMap) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().contains("home")) {

					Activity activity = ((Activity) pe);
					Coord coord = activity.getCoord();
					// If work is inside zoneMap return true
					if (isWithinZone(coord, zoneMap)) {
						return true;
					}

				}
			}
		}
		return false;

	}

	public static boolean worksInside(Plan plan, Map<String, Geometry> zoneMap) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().contains("work")) {

					Activity activity = ((Activity) pe);
					Coord coord = activity.getCoord();
					// If work is inside zoneMap return true
					if (isWithinZone(coord, zoneMap)) {
						return true;
					}

				}
			}
		}
		return false;

	}

}
