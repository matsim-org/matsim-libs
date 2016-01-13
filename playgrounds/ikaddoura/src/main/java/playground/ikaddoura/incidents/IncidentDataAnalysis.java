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
package playground.ikaddoura.incidents;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
 * @author ikaddoura this class requests incident data from HERE Maps.
 *
 */
public class IncidentDataAnalysis {
	private static final Logger log = Logger.getLogger(IncidentDataAnalysis.class);
 
	private final String outputDirectory = "../../../shared-svn/studies/ihab/incidents/";
	private Map<String, TrafficItem> trafficItems = new HashMap<>();
	private String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";

	public static void main(String[] args) throws XMLStreamException, IOException {
		
		IncidentDataAnalysis incidentAnalysis = new IncidentDataAnalysis();
		incidentAnalysis.run();	
	}

	public void run() throws XMLStreamException, IOException {
		collectTrafficItems();
		processTrafficItems();
	}

	private void collectTrafficItems() throws XMLStreamException, IOException {
		
		log.info("Analyzing all xml files in directory " + this.outputDirectory);
		File[] fileList = new File(outputDirectory).listFiles();

		for(File f : fileList) {
			if (f.getName().endsWith(".xml")) {
				
				String inputXmlFile = f.toString();
								
				HereMapsTrafficItemXMLReader trafficItemReader = new HereMapsTrafficItemXMLReader();
				trafficItemReader.readStream(inputXmlFile);
				
//				String outputCSVFile = inputXmlFile.substring(0, inputXmlFile.length() - 4) + ".csv";
//				log.info("Output CSV File: " + outputCSVFile);
//				TrafficItemWriter writer = new TrafficItemWriter();
//				writer.writeCSVFile(trafficItemReader.getTrafficItems(), outputCSVFile);
//				
				int counter = 0;
				for (TrafficItem item : trafficItemReader.getTrafficItems()) {
					if (trafficItems.containsKey(item.getId())) {
						// item with same ID already in map
						
						// see if they are really the same...
						if (item.toString().equals(trafficItems.get(item.getId()).toString())) {
							// ok, everything is fine
						} else {
							log.warn("new traffic item: " + item.toString());
							log.warn("old traffic item: " + trafficItems.get(item.getId()).toString());
							throw new RuntimeException("Two traffic items have the same ID but contain different information."
									+ "If missing information is just added everything is fine."
									+ "If information is changed, e.g. if the end time is corrected, everything becomes more complicated."
									+ "Aborting...");
						}
						
					} else {
						counter++;
						trafficItems.put(item.getId(), item);
					}
				}
				
				log.info(" +++ " + counter + " new traffic items added to map.");
			}
		}
		
		TrafficItemWriter writer = new TrafficItemWriter();
		writer.writeCSVFile(trafficItems.values(), outputDirectory + "incidentData.csv");
		
	}
	
	private void processTrafficItems() {
		log.info("Loading network...");
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (String id : this.trafficItems.keySet()) {
			
			if (this.trafficItems.get(id).getTMCAlert().getPhraseCode().contains("D15")) {
				// D15 means the road capacity is reduced to one lane
				DijkstraFactory f = new DijkstraFactory();
				final TravelDisutility travelCosts = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
				
				final Coord coordOriginWGS84 = new Coord(Double.valueOf(this.trafficItems.get(id).getOrigin().getLongitude()), Double.valueOf(this.trafficItems.get(id).getOrigin().getLatitude()));
				final Coord coordToWGS84 = new Coord(Double.valueOf(this.trafficItems.get(id).getTo().getLongitude()), Double.valueOf(this.trafficItems.get(id).getTo().getLatitude()));

				CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
				
				final Coord coordOriginGK4 = ct.transform(coordOriginWGS84);
				final Coord coordToGK4 = ct.transform(coordToWGS84);
				
				Link linkOrigin = NetworkUtils.getNearestLink(scenario.getNetwork(), coordOriginGK4);
				Link linkTo = NetworkUtils.getNearestLink(scenario.getNetwork(), coordToGK4);

				Path path = f.createPathCalculator(scenario.getNetwork(), travelCosts, new FreeSpeedTravelTime()).calcLeastCostPath(linkOrigin.getFromNode(), linkTo.getToNode(), 0., null, null);
				
				System.out.println("Traffic Item Coordinates (WGS84): " + coordOriginWGS84.toString() + " --> " + coordToWGS84.toString());
				System.out.println("Traffic Item Coordinates (DHDN_GK4): " + coordOriginGK4.toString() + " --> " + coordToGK4.toString());
				System.out.println("Nearest Origin Link: " + linkOrigin.getId() + " -- Nearest To Link: " + linkTo.getId());
				
				System.out.print("Affected Links: ");
				for (Link link : path.links) {
					System.out.print(link.getId().toString() + " ");
				}
				System.out.println();
				
			}				
		}
	}

}
