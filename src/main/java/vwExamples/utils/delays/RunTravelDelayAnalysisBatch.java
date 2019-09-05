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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.geotools.filter.expression.ThisPropertyAccessorFactory;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import analysis.drtOccupancy.DynModeTripsAnalyser;
import vwExamples.utils.LinksToShape.Links2ESRIShape;

public class RunTravelDelayAnalysisBatch {

	static Set<Id<Person>> relevantAgents = new HashSet<>();
	static Map<String, Geometry> zoneMap = new HashMap<>();
	static Geometry boundary;
	static Set<String> zones = new HashSet<>();
	static String shapeFile = "D:\\\\Matsim\\\\Axer\\\\Hannover\\\\ZIM\\\\input\\\\shp\\\\Hannover_Stadtteile.shp";
	static String shapeFeature = "NO";
	static List<Geometry> districtGeometryList = new ArrayList<Geometry>();
	static GeometryFactory geomfactory = JTSFactoryFinder.getGeometryFactory(null);
	static GeometryCollection geometryCollection = geomfactory.createGeometryCollection(null);
	static List<String> LinkAttributesList = new ArrayList<String>();

	public static void main(String[] args) {

		String runDir = "D:\\Matsim\\Axer\\Hannover\\ZIM\\output\\";
		// String runId = "vw219_netnet150_veh_idx0.";

		readShape(shapeFile, shapeFeature);
		getResearchAreaBoundary();

		File[] directories = new File(runDir).listFiles(File::isDirectory);
		for (File scenarioDir : directories) {
			relevantAgents.clear();

			String[] StringList = scenarioDir.toString().split("\\\\");
			String scenarioName = StringList[StringList.length - 1];
			
			Set<String> scenarioToBeAnalyzed = new HashSet<String>();
//			scenarioToBeAnalyzed.add("VW243_HomeOfficeInOut1x_10pct");
//			scenarioToBeAnalyzed.add("VW243_HomeOfficeInOut2x_10pct");
//			scenarioToBeAnalyzed.add("VW243_LocalLinkFlow_1.28_10pct");
//			scenarioToBeAnalyzed.add("VW243_CityCommuterDRT_10pct300_veh_idx0");
//			scenarioToBeAnalyzed.add("VW243_LocalLinkFlow_1.15_10pct");
//			scenarioToBeAnalyzed.add("VW243_CityCommuterDRTAmpel2.0_10pct300_veh_idx0");
//			scenarioToBeAnalyzed.add("VW243_Drt_HomeOffice_LinkFlow1.15300_veh_idx0");
//			scenarioToBeAnalyzed.add("VW251_CityCommuterDRT_100pct2800_veh_idx0");
//			scenarioToBeAnalyzed.add("vw251.1.0");
//			scenarioToBeAnalyzed.add("vw243_CityDRT_10pct_0.1300_veh_idx0");
//			scenarioToBeAnalyzed.add("vw243_CityDRT_10pct_0.05300_veh_idx0");
			scenarioToBeAnalyzed.add("vw243_CityDRT_10pct_0.1350_veh_idx0");
			
			
//			
			

			if (scenarioToBeAnalyzed.contains(scenarioName)) {
				System.out.println("Start Delay Analysis: "+ scenarioName);

				StreamingPopulationReader spr = new StreamingPopulationReader(
						ScenarioUtils.createScenario(ConfigUtils.createConfig()));
				spr.addAlgorithm(new PersonAlgorithm() {
					@Override
					public void run(Person person) {
						// relevantAgents.add(person.getId());

						// 01: Case for Commuter
						// if (livesOutside(person.getSelectedPlan(), zoneMap)
						// && worksInside(person.getSelectedPlan(), zoneMap)) {
						// relevantAgents.add(person.getId());
						// }

						// 02: HousholdSurvery (Inhabitants)
						// if (livesInside(person.getSelectedPlan(), zoneMap)) {
						// relevantAgents.add(person.getId());
						// }

					}

				});
				spr.readFile(scenarioDir + "\\" + scenarioName + ".output_plans.xml.gz");

				Network network = NetworkUtils.createNetwork();
				new MatsimNetworkReader(network).readFile(scenarioDir + "\\" + scenarioName + ".output_network.xml.gz");
				TravelDelayCalculator tdc = new TravelDelayCalculator(network, boundary);

				EventsManager events = EventsUtils.createEventsManager();
				events.addHandler(tdc);
				new MatsimEventsReader(events).readFile(scenarioDir + "\\" + scenarioName + ".output_events.xml.gz");
				DynModeTripsAnalyser.collection2Text(tdc.getTrips(),
						scenarioDir + "\\" + scenarioName + ".delay_city_hannover.csv",
						"PersonId;ArrivalTime;FreespeedTravelTime;ActualTravelTime;Delay;Beeline;Flag;Mileage_m");

				Map<Id<Link>, MutableDouble> linkFlows = tdc.getLinkFlowMap();
				Map<Id<Link>, MutableDouble> linkDelays = tdc.getLinkDelayMap();

				for (Entry<Id<Link>, ? extends Link> linkEntry : network.getLinks().entrySet()) {
					Id<Link> linkId = linkEntry.getValue().getId();

					if (linkFlows.containsKey(linkId)) {

						Double flow = linkFlows.get(linkId).doubleValue();
						Double delay = linkDelays.get(linkId).doubleValue();
						Double delayPerVeh_min = delay / (flow * 60);
						Double linkCongestionIdx = tdc.getMeanCongestionIdxPerLink(linkId);

						// double accaptedDelay =
						// NetworkUtils.getFreespeedTravelTime(linkEntry.getValue())*0.2*flow;
						linkEntry.getValue().getAttributes().putAttribute("flow_veh", flow);
						// linkEntry.getValue().getAttributes().putAttribute("congestion_idx",
						// delay/accaptedDelay);
						linkEntry.getValue().getAttributes().putAttribute("delay_h", delay / 3600.0);
						linkEntry.getValue().getAttributes().putAttribute("delayv_min", delayPerVeh_min);
						linkEntry.getValue().getAttributes().putAttribute("cong_idx", linkCongestionIdx);

					} else {
						linkEntry.getValue().getAttributes().putAttribute("flow_veh", -99.0);
						// linkEntry.getValue().getAttributes().putAttribute("congestion_idx", -99.0);
						linkEntry.getValue().getAttributes().putAttribute("delay_h", -99.0);
						linkEntry.getValue().getAttributes().putAttribute("delayv_min", -99.0);
						linkEntry.getValue().getAttributes().putAttribute("cong_idx", -99.0);

					}
				}

				NetworkUtils.writeNetwork(network,
						scenarioDir + "\\" + scenarioName + ".output_network_flow_delay.xml.gz");

//				 String netfile = scenarioDir + "\\" + scenarioName +
//				 ".output_network_flow_delay.xml.gz";
//				 String outputFileLs = scenarioDir + "\\" + scenarioName +
//				 ".output_network_flow_delay_l.shp";
//				 String outputFileP = scenarioDir + "\\" + scenarioName +
//				 ".output_network_flow_delay_p.shp";
//				 String[] params = {netfile,outputFileLs,outputFileP};
//				 Links2ESRIShape.main(params);

				createLinkAttributesList(network);
				DynModeTripsAnalyser.collection2Text(LinkAttributesList,
						scenarioDir + "\\" + scenarioName + ".linkData_delay_flow_city_hannover.csv",
						"linkid;delay_h;flow_veh");

//				writeTimeDependentFlow(scenarioDir + "\\" + scenarioName + ".LinkFlows.csv",tdc.getTimeDependentFlow());
			}
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

	public static void createLinkAttributesList(Network network) {
		String sep = ";";
		for (Entry<Id<Link>, ? extends Link> linkEntry : network.getLinks().entrySet()) {

			String linkId = linkEntry.getValue().getId().toString();
			String delay_sec = linkEntry.getValue().getAttributes().getAttribute("delay_h").toString();
			String flow_veh = linkEntry.getValue().getAttributes().getAttribute("flow_veh").toString();

			String Entry = linkId + sep + delay_sec + sep + flow_veh;
			LinkAttributesList.add(Entry);
		}

	}
	
	public static void writeTimeDependentFlow(String fileName, Map<Double, HashMap<Id<Link>,MutableInt>> timeDependentFlow) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);

		
		SortedSet<Double> timeBins = new TreeSet<>(timeDependentFlow.keySet());
				
		
		
		try {

			bw.write("hour;linkId;flowPerHour");
			bw.newLine();
			for (Double t : timeBins) {
				
				for (Entry<Id<Link>, MutableInt> linkEntry : timeDependentFlow.get(t).entrySet())
				{
					String row = t + ";" + linkEntry.getKey() + ";" + linkEntry.getValue();

					bw.write(row);
					bw.newLine();
				}

				

			}

			bw.flush();
			bw.close();
			// datasetrequ.addSeries(parkCount);
			// // JFreeChart chart = chartProfile(splitParkings.size(), dataset, "Waiting
			// // times", "Wait time (s)");
			// JFreeChart chart2 = chartProfile(splitParkings.size(), datasetrequ, "Parked
			// Vehicles over Time",
			// "Total Parked Cars [-]");
			// // ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			// ChartSaveUtils.saveAsPNG(chart2, fileName + "_parkEvents", 1500, 1000);

		} catch (IOException e) {

			e.printStackTrace();
		}
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
