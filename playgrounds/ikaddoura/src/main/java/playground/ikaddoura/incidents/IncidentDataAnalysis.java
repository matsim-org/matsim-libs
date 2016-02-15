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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
 * @author ikaddoura
 * 
 * This class analyzes incidents and writes them into a csv and a shapefile.
 *
 */
public class IncidentDataAnalysis {
	private static final Logger log = Logger.getLogger(IncidentDataAnalysis.class);
 
	private final String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";
//	private final String outputDirectory = "../../../shared-svn/studies/ihab/incidents/output-berlin/";
	private final String outputDirectory = "/Users/ihab/Desktop/repomgr-ik/output-berlin/";
	
//	private final String networkFile = "../../../shared-svn/studies/ihab/incidents/network/germany-network-mainroads.xml";
//	private final String outputDirectory = "../../../shared-svn/studies/ihab/incidents/germany-test/";
	
	private final boolean writeCSVFileForEachXMLFile = false;
		
// ##################################################################

	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
	private final Map<String, TrafficItem> trafficItems = new HashMap<>();
	private final Map<String, Path> trafficItemId2path = new HashMap<>();
	private final TMCAlerts tmc = new TMCAlerts();
	private final Set<String> trafficItemsToBeChecked = new HashSet<>();

	private Scenario scenario = null;
	private Network carNetwork = null;
	
	public static void main(String[] args) throws XMLStreamException, IOException {
		IncidentDataAnalysis incidentAnalysis = new IncidentDataAnalysis();
		incidentAnalysis.run();	
	}

	public void run() throws XMLStreamException, IOException {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(this.outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		collectTrafficItems(); // traffic items that have the same traffic item IDs are updated by the more recent information
		updateTrafficItems(); // update all traffic items that are updated or canceled by another traffic item
		loadScenario();
		computeCarNetwork();
		computeIncidentPaths();
		writeIncidentLinksToShapeFile();
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private void updateTrafficItems() {
		Set<String> updateItemsToBeDeleted = new HashSet<>();

		for (TrafficItem updateItem : this.trafficItems.values()) {
			
			if (tmc.trafficItemIsAnUpdate(updateItem)) {
								
				if (this.trafficItems.get(updateItem.getOriginalId()) == null) {
					// original traffic item not in map
					
				} else {
					TrafficItem originalItem = this.trafficItems.get(updateItem.getOriginalId());

					if (updateItem.getOrigin().toString().equals(originalItem.getOrigin().toString()) && updateItem.getTo().toString().equals(originalItem.getTo().toString())) {
						// everything seems ok, the updated traffic item's locations are the same
						
					} else {
						throw new RuntimeException("An update message should only update the incident's endtime. The location should remain the same. Aborting...");
					}
					originalItem.setEndTime(updateItem.getStartTime());
					updateItemsToBeDeleted.add(updateItem.getId());
				}
				
			} else {
				// nothing to update
			}
		}
		log.info("+++ " + updateItemsToBeDeleted.size() + " original traffic item(s) updated according to update message(s)");
		for (String updateItemId : updateItemsToBeDeleted) {
			this.trafficItems.remove(updateItemId);
		}

	}

	private void computeCarNetwork() {
		log.info("Creating car network... ");

		carNetwork = NetworkUtils.createNetwork();
		NetworkFactory factory = new NetworkFactoryImpl(carNetwork);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				
				if (!carNetwork.getNodes().containsKey(link.getFromNode().getId())) {
					carNetwork.addNode(factory.createNode(link.getFromNode().getId(), link.getFromNode().getCoord()));
				}
				if (!carNetwork.getNodes().containsKey(link.getToNode().getId())) {
					carNetwork.addNode(factory.createNode(link.getToNode().getId(), link.getToNode().getCoord()));
				}
				
				carNetwork.addLink(factory.createLink(link.getId(), link.getFromNode(), link.getToNode()));
			}
		}	
		
		log.info("Creating car network... Done.");
	}

	private void loadScenario() {
		log.info("Loading scenario...");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		this.scenario = ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
	}

	private void writeIncidentLinksToShapeFile() {
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
		.setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4))
		.setName("Link")
		.addAttribute("LinkId", String.class)
		.addAttribute("IncidentId", String.class)
		.addAttribute("Street", String.class)
		.addAttribute("Alert", String.class)
		.addAttribute("Message", String.class)
		.addAttribute("Length", Double.class)
		.addAttribute("Modes", String.class)
		.addAttribute("Capacity", Double.class)
		.addAttribute("Lanes", Double.class)
		.addAttribute("Freespeed", Double.class)
		.addAttribute("IncModes", String.class)
		.addAttribute("IncCap", Double.class)
		.addAttribute("IncLanes", Double.class)
		.addAttribute("IncSpeed", Double.class)
		.addAttribute("IncStart", String.class)
		.addAttribute("IncEnd", String.class)
		.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
						
		for (String id : this.trafficItemId2path.keySet()) {
			
			if (this.trafficItemId2path.get(id) == null) {
				// no path identified
				log.warn("Skipping traffic item " + id + " because there is no path.");
				
			} else {
				for (Link link : this.trafficItemId2path.get(id).links) {
					if (tmc.getIncidentObject(link, this.trafficItems.get(id)) != null) {
						SimpleFeature feature = factory.createPolyline(
								new Coordinate[] {
										new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
										new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord())) }
								, tmc.getIncidentObject(link, this.trafficItems.get(id))
								, null);
						features.add(feature);
					}
				}
			}
		}
		
		if (features.isEmpty()) {
			log.warn("No traffic incidents. Nothing to write into a shape file.");
		} else {
			log.info("Writing out incident shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputDirectory + "incidentLinks.shp");
			log.info("Writing out incident shapefile... Done.");
		}
		
		if (!this.trafficItemsToBeChecked.isEmpty()) {
			Collection<SimpleFeature> criticalFeatures = new ArrayList<SimpleFeature>();
			
			for (String id : this.trafficItemsToBeChecked) {
				
				if (this.trafficItemId2path.get(id) == null) {
					// no path identified
					log.warn("Skipping traffic item " + id + " because there is no path.");
					
				} else {
					for (Link link : this.trafficItemId2path.get(id).links) {
						if (tmc.getIncidentObject(link, this.trafficItems.get(id)) != null) {
							SimpleFeature feature = factory.createPolyline(
									new Coordinate[] {
											new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
											new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord())) }
									, tmc.getIncidentObject(link, this.trafficItems.get(id))
									, null);
							criticalFeatures.add(feature);
						}
					}
				}
			}
			
			if (criticalFeatures.isEmpty()) {
				log.warn("No traffic incidents. Nothing to write into a shape file.");
			} else {
				log.info("Writing out incident shapefile... ");
				ShapeFileWriter.writeGeometries(criticalFeatures, outputDirectory + "incidentLinksToBeChecked.shp");
				log.info("Writing out incident shapefile... Done.");
			}
		}
	}

	private void collectTrafficItems() throws XMLStreamException, IOException {
		
		log.info("Collecting traffic items from all xml files in directory " + this.outputDirectory + "...");
	
		File[] fileList = new File(outputDirectory).listFiles();

		for(File f : fileList) {
			
			if (f.getName().endsWith(".xml") || f.getName().endsWith(".xml.gz")) {
				
				String inputXmlFile = f.toString();
								
				HereMapsTrafficItemXMLReader trafficItemReader = new HereMapsTrafficItemXMLReader();
				trafficItemReader.readStream(inputXmlFile);
				
				// write out
				if (writeCSVFileForEachXMLFile) {
					String outputCSVFile = inputXmlFile.substring(0, inputXmlFile.length() - 4) + ".csv";
					log.info("Writing xml file to csv file: " + outputCSVFile);
					TrafficItemWriter writer = new TrafficItemWriter();
					writer.writeCSVFile(trafficItemReader.getTrafficItems(), outputCSVFile);
				}
				
				int counterNew = 0;
				int counterUpdatedEndTimes = 0;
				int counterUpdatedEndTimesUpdateMessage = 0;
				int counterIgnoredAlreadyInMap = 0;
				int counterIgnoredNullInfoItem = 0;
				for (TrafficItem item : trafficItemReader.getTrafficItems()) {
					
					if (item.getTMCAlert().getPhraseCode() == null ||
							item.getTo().getLatitude() == null ||
							item.getTo().getLongitude() == null ||
							item.getOrigin().getLatitude() == null ||
							item.getOrigin().getLongitude() == null) {

						log.warn("Null info. Ignoring traffic item: " + item.toString());
						counterIgnoredNullInfoItem++;
						
					} else {
						if (trafficItems.containsKey(item.getId())) {
							// Item with same ID is already in the map.
							
							if (item.toString().equals(trafficItems.get(item.getId()).toString())) {
								// Everything is fine. No need for adding the item to the map.
								counterIgnoredAlreadyInMap++;
								
							} else {
								// The traffic item information is different.
								
								// same locations, same messages, same start times, different end times
								if ( item.getOrigin().toString().equals(trafficItems.get(item.getId()).getOrigin().toString()) &&
										item.getTo().toString().equals(trafficItems.get(item.getId()).getTo().toString()) &&
										item.getTMCAlert().toString().equals(trafficItems.get(item.getId()).getTMCAlert().toString()) &&
										item.getStartTime().equals(trafficItems.get(item.getId()).getStartTime()) &&
										(!item.getEndTime().equals(trafficItems.get(item.getId()).getEndTime())) ) {
									
									log.info("Different end times...");
									log.info("New item: " + item.toString());
									log.info("Old item: " + trafficItems.get(item.getId()));
									
									if ( item.getDownloadTime() > trafficItems.get(item.getId()).getDownloadTime() ) {
										trafficItems.put(item.getId(), item);
										counterUpdatedEndTimes++;
									}
									
									log.info("Upd item: " + trafficItems.get(item.getId()));

									
								// same locations, different messages
								} else if (item.getOrigin().toString().equals(trafficItems.get(item.getId()).getOrigin().toString()) &&
										item.getTo().toString().equals(trafficItems.get(item.getId()).getTo().toString()) &&
										(!item.getTMCAlert().toString().equals(trafficItems.get(item.getId()).getTMCAlert().toString())) ) {
									
									if (tmc.trafficItemIsAnUpdate(item) && (!tmc.trafficItemIsAnUpdate(trafficItems.get(item.getId()))) ) {
										
										if (trafficItems.get(item.getId()).getEndTime().equals(item.getStartTime())) {
											// already updated before
										} else {
											log.info("Different messages...");
											log.info("New item: " + item.toString());
											log.info("Old item: " + trafficItems.get(item.getId()));
											
											trafficItems.get(item.getId()).setEndTime(item.getStartTime());
											counterUpdatedEndTimesUpdateMessage++;
											
											log.info("Upd item: " + trafficItems.get(item.getId()));	
										}

									} else if ( (!tmc.trafficItemIsAnUpdate(item)) && tmc.trafficItemIsAnUpdate(trafficItems.get(item.getId())) ) {
										if (item.getEndTime().equals(trafficItems.get(item.getId()).getStartTime())) {
											// already updated before
										} else {
											log.info("Different messages...");
											log.info("New item: " + item.toString());
											log.info("Old item: " + trafficItems.get(item.getId()));
											
											item.setEndTime(trafficItems.get(item.getId()).getStartTime());
											trafficItems.put(item.getId(), item);
											counterUpdatedEndTimesUpdateMessage++;
											
											log.info("Upd item: " + trafficItems.get(item.getId()));
										}
									
									} else {
										
//										if (item.getDownloadTime() > trafficItems.get(item.getId()).getDownloadTime() ) {
//											trafficItems.put(item.getId(), item);
//										}	
										throw new RuntimeException("Aborting...");
									}
								} else {
									
//									if (item.getDownloadTime() > trafficItems.get(item.getId()).getDownloadTime() ) {
//										trafficItems.put(item.getId(), item);
//									}
									throw new RuntimeException("Aborting...");
								}	
							}
							
						} else {
							counterNew++;
							trafficItems.put(item.getId(), item);
						}
					}
					
				}
				
				if (counterNew > 0) log.info(" +++ " + counterNew + " new traffic items added to map.");
				if (counterIgnoredAlreadyInMap > 0) log.info(" +++ " + counterIgnoredAlreadyInMap + " traffic items ignored (already in the map).");
				
				if (counterUpdatedEndTimes > 0) log.info(" +++ " + counterUpdatedEndTimes + " traffic items updated (more recent information).");
				if (counterUpdatedEndTimesUpdateMessage > 0) log.info(" +++ " + counterUpdatedEndTimesUpdateMessage + " traffic items updated (canceled message).");
				if (counterIgnoredNullInfoItem > 0) log.info(" +++ " + counterIgnoredNullInfoItem + " traffic items ignored (null info).");
			}
			log.info("Collecting traffic items from all xml files in directory " + this.outputDirectory + "... Done.");
		}
		
		TrafficItemWriter writer = new TrafficItemWriter();
		writer.writeCSVFile(trafficItems.values(), outputDirectory + "incidentData.csv");
		
	}
	
	private void computeIncidentPaths() {
		
		log.info("Processing traffic items...");
		
		for (String id : this.trafficItems.keySet()) {
						
			final Coord coordFromWGS84 = new Coord(Double.valueOf(this.trafficItems.get(id).getOrigin().getLongitude()), Double.valueOf(this.trafficItems.get(id).getOrigin().getLatitude()));
			final Coord coordToWGS84 = new Coord(Double.valueOf(this.trafficItems.get(id).getTo().getLongitude()), Double.valueOf(this.trafficItems.get(id).getTo().getLatitude()));
			
			final Coord coordFromGK4 = ct.transform(coordFromWGS84);
			final Coord coordToGK4 = ct.transform(coordToWGS84);			
			double beelineDistance = NetworkUtils.getEuclidianDistance(coordFromGK4, coordToGK4);
			
			Path incidentPath = null;
			
			// first just use the nearest link functionality
			Link nearestLinkFrom = NetworkUtils.getNearestLink(carNetwork, coordFromGK4);
			Link nearestLinkTo = NetworkUtils.getNearestLink(carNetwork, coordToGK4);
			
			incidentPath = computePath(nearestLinkFrom.getToNode(), nearestLinkTo.getFromNode());
			double pathDistance = computePathDistance(incidentPath);
			
			// then see if the path is plausible
			boolean tryToFindABetterPath = false;
			if (pathDistance > 2. * beelineDistance) {
				log.warn("No good path identified for incident " + id + ". The path distance is at least twice as long as the beeline distance. Trying to identify a more plausible path...");
				tryToFindABetterPath = true;
			}
				
			if (tryToFindABetterPath) {
				double pathDistanceBeelineDifference = Double.MAX_VALUE;

				Collection<Node> nearestNodesAroundFromCoord = ((NetworkImpl) carNetwork).getNearestNodes(coordFromGK4, 250.);
				Collection<Node> nearestNodesAroundToCoord = ((NetworkImpl) carNetwork).getNearestNodes(coordFromGK4, 250.);
				
				for (Node nodeArroundFromCoord : nearestNodesAroundFromCoord) {
					for (Node nodeAroundToCoord : nearestNodesAroundToCoord) {
						
						Path path = computePath(nodeArroundFromCoord, nodeAroundToCoord);
						double pathDifference = Math.abs(computePathDistance(path) - beelineDistance);
						
						if (pathDifference < pathDistanceBeelineDifference) {
							pathDistanceBeelineDifference = pathDifference;
							incidentPath = path;
						}
					}
				}
			}
			
			if (pathDistance > 2. * beelineDistance) {
				log.warn("No good path identified for incident " + id + ". The path distance is at least twice as long as the beeline distance."
						+ "The inplausible paths will be written into 'incidentsLinksToBeChecked.shp'. Maybe try a better network resolution.");
				this.trafficItemsToBeChecked.add(id);
			}
			
			if (incidentPath == null || incidentPath.links.size() == 0) {
				log.warn("No path identified for incident " + id + ".");
			}
			
			this.trafficItemId2path.put(id, incidentPath);
		}
		
		log.info("Processing traffic items... Done.");		
	}

	private double computePathDistance(Path path) {
		
		double pathDistance = 0.;
		
		for (Link link : path.links) {
			pathDistance = pathDistance + link.getLength();
		}	
		
		return pathDistance;
	}

	private Path computePath(Node fromNode, Node toNode) {
		
		DijkstraFactory f = new DijkstraFactory();
		final TravelDisutility travelCosts = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());

		Path path = f.createPathCalculator(scenario.getNetwork(), travelCosts, new FreeSpeedTravelTime()).calcLeastCostPath(scenario.getNetwork().getNodes().get(fromNode.getId()), scenario.getNetwork().getNodes().get(toNode.getId()), 0., null, null);
		
		return path;
	}

}
