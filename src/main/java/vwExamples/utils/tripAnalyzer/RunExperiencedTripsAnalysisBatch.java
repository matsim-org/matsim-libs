/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

//Filter Trips for users that work in BS but live outside BS. There is no range filter. 
//Which means even people in WOB may be analyzed as commuters

package vwExamples.utils.tripAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;

public class RunExperiencedTripsAnalysisBatch {

	static Map<String, Geometry> zoneMap = new HashMap<>();
	static Set<String> zones = new HashSet<>();
	static String shapeFile = "D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\shp\\Hannover_Stadtteile.shp";
	static String shapeFeature = "NO";
	private static List<Geometry> districtGeometryList = new ArrayList<Geometry>();
	private static GeometryFactory geomfactory = JTSFactoryFinder.getGeometryFactory(null);
	private static GeometryCollection geometryCollection = geomfactory.createGeometryCollection(null);

	private static Geometry boundary; // Research boundary in which e.g. mileage or traffic performance is measured

	public static void main(String[] args) {

		run("D:\\Matsim\\Axer\\Hannover\\ZIM\\output\\");
		run("D:\\Matsim\\Axer\\Hannover\\Base\\");
		run("D:\\Matsim\\Axer\\Hannover\\WV\\");
	}

	public static void run(String runDir) {

		readShape(shapeFile, shapeFeature);
		getResearchAreaBoundary();

		File[] directories = new File(runDir).listFiles(File::isDirectory);
		for (File scenarioDir : directories) {

			String[] StringList = scenarioDir.toString().split("\\\\");
			String scenarioName = StringList[StringList.length - 1];

			Set<String> scenarioToBeAnalyzed = new HashSet<String>();
			
//			scenarioToBeAnalyzed.add("vw280_100pct");
//			scenarioToBeAnalyzed.add("2019-11-05_21-27-49__cityDRT_100pct");
//			scenarioToBeAnalyzed.add("VW280_CityCommuterDRT_carOnly_100pct_additiv");
//			scenarioToBeAnalyzed.add("VW280_HomeOffice_100pct");
//			scenarioToBeAnalyzed.add("VW280_LocalLinkFlow_1.15_100pct_Inno");
//			scenarioToBeAnalyzed.add("UAM_100pct_0.2_3");
			scenarioToBeAnalyzed.add("vw280_0.1");
			
						

			if (scenarioToBeAnalyzed.contains(scenarioName)) {

				System.out.println(runDir + "\\" + scenarioName);
				analyzeTrips(runDir + "\\" + scenarioName, scenarioName);
			}

		}

	}

	public static void analyzeTrips(String rundir, String runid) {

		String runDirectory = rundir;
		String runId = runid + ".";
		String runPrefix = runDirectory + "\\" + runId;

		boolean useTransitSchedule = true;

		Set<String> monitoredModes = new HashSet<>();
		monitoredModes.add("pt");
		monitoredModes.add("transit_walk");
		monitoredModes.add("drt");
		monitoredModes.add("drt_walk");
		monitoredModes.add("access_walk");
		monitoredModes.add("egress_walk");
		monitoredModes.add("non_network_walk");
		monitoredModes.add("car");
		monitoredModes.add("walk");
		monitoredModes.add("bike");
		monitoredModes.add("ride");
		monitoredModes.add("stayHome");
		monitoredModes.add("uam");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix + "output_network.xml.gz");
		// new PopulationReader(scenario).readFile(runPrefix + "output_plans.xml.gz");

		if (useTransitSchedule) {
			new TransitScheduleReader(scenario).readFile(runPrefix + "output_transitSchedule.xml.gz");
		}

		// Analysis
		EventsManager events = EventsUtils.createEventsManager();

		Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();

		// Match link2Zone
		Map<Id<Link>, String> links2ZoneMap = link2Zone(scenario.getNetwork());

		DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(),
				scenario.getTransitSchedule(), monitoredModes, monitoredStartAndEndLinks, links2ZoneMap, zoneMap,
				boundary);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
		System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
		ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix + "experiencedTrips.csv",
				eventHandler.getPerson2ExperiencedTrips(), eventHandler.getZone2BinActiveVehicleMap(),
				eventHandler.getModeMileageMap(), monitoredModes, scenario.getNetwork(), zoneMap, boundary);
		tripsWriter.writeExperiencedTrips();
		// tripsWriter.writeExperiencedLegs();
		// System.out.print("Counted Stuck Events "+ eventHandler.getStuckEvents());
		// ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix +
		// "experiencedLegs.csv",
		// eventHandler.getPerson2ExperiencedTrips(), monitoredModes,
		// scenario.getNetwork(), relevantAgents,
		// zoneMap);
		// legsWriter.writeExperiencedLegs();

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

	public static void getResearchAreaBoundary() {
		// This class infers the geometric boundary of all network link

		Logger.getLogger(ExperiencedTripsWriter.class).warn("MERGED GEOMETRIES TO ONE LARGE ZONE BOUNDARY");
		for (Geometry zoneGeom : zoneMap.values()) {
			districtGeometryList.add(zoneGeom);
		}

		geometryCollection = (GeometryCollection) geomfactory.buildGeometry(districtGeometryList);
		boundary = geometryCollection.union();

	}

	public static Map<Id<Link>, String> link2Zone(Network network) {
		System.out.println("Derive Zones for all Links");
		Map<Id<Link>, String> link2Zone = new HashMap<>();
		// Iterate over each link
		for (Link l : network.getLinks().values()) {

			Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
			Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY());
			Geometry lineString = new LineSegment(start, end).toGeometry(new GeometryFactory());

			// Check if link intersects with zone
			for (String z : zoneMap.keySet()) {
				// System.out.println("Working on Zone: "+z);
				Geometry zone = zoneMap.get(z);
				if (lineString.intersects(zone)) {

					link2Zone.put(l.getId(), z);
					break;

				}
			}
			// No zone found for link
			// link2Zone.put(l.getId(), "1");

			// if (!link2Zone.containsKey(l.getId()))
			// {
			//// System.out.println( l.getId() + " not in area, set null " );
			// link2Zone.put(l.getId(), null);
			//
			// }
			// System.out.println(linkCounter + " out of " +linkNumber );
		}

		return link2Zone;
	}

}
