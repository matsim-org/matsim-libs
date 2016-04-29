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
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.incidents.data.TrafficItem;
import playground.ikaddoura.incidents.io.HereMapsTrafficItemXMLReader;
import playground.ikaddoura.incidents.io.Incident2CSVWriter;
import playground.ikaddoura.incidents.io.Incident2NetworkChangeEventsWriter;
import playground.ikaddoura.incidents.io.Incident2SHPWriter;

/**
 * @author ikaddoura
 * 
 * This class analyzes incidents and writes them into a csv and a shapefile.
 *
 */
public class IncidentDataAnalysis {
	private final Logger log = Logger.getLogger(IncidentDataAnalysis.class);

	private String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";
	private String crs = TransformationFactory.DHDN_GK4;
//	private String crs = TransformationFactory.WGS84_UTM33N;
	
	private String inputDirectory = "../../../shared-svn/studies/ihab/incidents/server/output-berlin/";
	private String outputDirectory = "../../../shared-svn/studies/ihab/incidents/analysis/output-berlin_2016-04-27_a/";
	
	private boolean writeCSVFileForEachXMLFile = false;
	
	private boolean writeAllTrafficItems2ShapeFile = false;
	
	private boolean writeDaySpecificTrafficItems2ShapeFile = true;
	private String shpFileStartDateTime = "2016-02-11";
	private String shpFileEndDateTime = "2016-03-26";
	
	private boolean writeNetworkChangeEventFiles = true;
	private String networkChangeEventStartDateTime = "2016-02-11";
	private String networkChangeEventEndDateTime = "2016-03-26";
		
// ##################################################################
	
	private final Map<String, TrafficItem> trafficItems = new HashMap<>();
	private final TMCAlerts tmc = new TMCAlerts();

	public static void main(String[] args) throws XMLStreamException, IOException, ParseException {
		
		IncidentDataAnalysis incidentAnalysis = new IncidentDataAnalysis();
		incidentAnalysis.run();	
	}
	
	public IncidentDataAnalysis() {
		log.warn("Using the default constructor...");
	}
	
	public IncidentDataAnalysis(
			String networkFile,
			String crs,
			String inputDirectory,
			String outputDirectory,
			boolean writeCSVFileForEachXMLFile,
			boolean writeAllTrafficItems2ShapeFile,
			boolean writeDaySpecificTrafficItems2ShapeFile,
			String shpStartDateTime,
			String shpEndDateTime,
			boolean writeNetworkChangeEventFiles,
			String nceStartDateTime,
			String nceEndDateTime) {
		
		this.networkFile = networkFile;
		this.crs = crs;
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		this.writeCSVFileForEachXMLFile = writeCSVFileForEachXMLFile;
		this.writeAllTrafficItems2ShapeFile = writeAllTrafficItems2ShapeFile;
		this.writeDaySpecificTrafficItems2ShapeFile = writeDaySpecificTrafficItems2ShapeFile;
		this.shpFileStartDateTime = shpStartDateTime;
		this.shpFileEndDateTime = shpEndDateTime;
		this.writeNetworkChangeEventFiles = writeNetworkChangeEventFiles;
		this.networkChangeEventStartDateTime = nceStartDateTime;
		this.networkChangeEventEndDateTime = nceEndDateTime;
	}

	public void run() throws XMLStreamException, IOException, ParseException {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(this.outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		collectTrafficItems(); // traffic items that have the same traffic item IDs are updated by the more recent information or by the update traffic item
		Incident2CSVWriter.writeTrafficItems(trafficItems.values(), outputDirectory + "incidentData_beforeConsideringUpdateMessages.csv");
	
		updateTrafficItems(); // update all traffic items that are updated or canceled by another traffic item
		
		// write CSV file which contains all information (start point, end point, type, ...) 
		Incident2CSVWriter.writeTrafficItems(trafficItems.values(), outputDirectory + "incidentData.csv");
				
		// map incidents on network
		final Incident2Network networkMapper = new Incident2Network(loadScenario(), this.trafficItems, this.crs);
		networkMapper.computeIncidentPaths();
		final Map<String, Path> trafficItemId2path = networkMapper.getTrafficItemId2path();

		final Incident2SHPWriter shpWriter = new Incident2SHPWriter(this.tmc, this.trafficItems, trafficItemId2path);

		if (writeAllTrafficItems2ShapeFile) {
			log.info("Writing all traffic items to shape file...");
			
			shpWriter.writeTrafficItemLinksToShapeFile(outputDirectory + "trafficItems_all.shp", this.trafficItems.keySet(), this.crs);
			
			final Set<String> trafficItemsToCheck = networkMapper.getTrafficItemsToCheck();
			shpWriter.writeTrafficItemLinksToShapeFile(outputDirectory + "trafficItems_WARNING.shp", trafficItemsToCheck, this.crs);
		}
		
		if (writeDaySpecificTrafficItems2ShapeFile) {
			log.info("Writing filtered traffic items to shape file(s)...");

			final Set<String> filteredTrafficItems = new HashSet<>();
			for (TrafficItem item : this.trafficItems.values()) {
				if (DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < DateTime.parseDateTimeToDateTimeSeconds(shpFileStartDateTime)
						|| DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > DateTime.parseDateTimeToDateTimeSeconds(shpFileEndDateTime) + (24 * 3600.)) {
				} else {
					filteredTrafficItems.add(item.getId());
				}
			}
			shpWriter.writeTrafficItemLinksToShapeFile(outputDirectory + "trafficItems_" + shpFileStartDateTime + "_" + shpFileEndDateTime + ".shp", filteredTrafficItems, crs);
			shpWriter.writeCongestionInfo2ShapeFile(outputDirectory + "delays_" + shpFileStartDateTime + "_" + shpFileEndDateTime + ".shp", filteredTrafficItems, crs);
		}
		
		if (writeNetworkChangeEventFiles) {
			// write network change events file and network incident shape file for each day
			final Incident2NetworkChangeEventsWriter nceWriter = new Incident2NetworkChangeEventsWriter(this.tmc, this.trafficItems, trafficItemId2path, this.crs);
			nceWriter.writeIncidentLinksToNetworkChangeEventFile(this.networkChangeEventStartDateTime, this.networkChangeEventEndDateTime, this.outputDirectory);
		}
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private void collectTrafficItems() throws XMLStreamException, IOException {
		
		log.info("Collecting traffic items from all xml files in directory " + this.inputDirectory + "...");
	
		File[] fileList = new File(inputDirectory).listFiles();
		
		if (fileList.length == 0) {
			throw new RuntimeException("No file in " + this.inputDirectory + ". Aborting...");
		}
		
		boolean foundXMLFile = false;
		
		for (File f : fileList) {
 
			if (f.getName().endsWith(".xml") || f.getName().endsWith(".xml.gz")) {
				
				foundXMLFile = true;
				String inputXmlFile = f.toString();
								
				HereMapsTrafficItemXMLReader trafficItemReader = new HereMapsTrafficItemXMLReader();
				trafficItemReader.readStream(inputXmlFile);
				
				// write out
				if (writeCSVFileForEachXMLFile) {
					String outputCSVFile = inputXmlFile.substring(0, inputXmlFile.length() - 4) + ".csv";
					log.info("Writing xml file to csv file: " + outputCSVFile);
					Incident2CSVWriter.writeTrafficItems(trafficItemReader.getTrafficItems(), outputCSVFile);
				}
				
				int counterNew = 0;
				int counterIgnoredAlreadyInMap = 0;
				int counterIgnoredNullInfoItem = 0;
				int counterUpdated = 0;
				
				log.info(trafficItemReader.getTrafficItems().size() + " new traffic items.");
				for (TrafficItem item : trafficItemReader.getTrafficItems()) {
					
					if (item.getTMCAlert().getPhraseCode() == null ||
							item.getTo().getLatitude() == null ||
							item.getTo().getLongitude() == null ||
							item.getOrigin().getLatitude() == null ||
							item.getOrigin().getLongitude() == null) {

						log.warn("Null info. Ignoring traffic item: " + item.toString());
						counterIgnoredNullInfoItem++;
						
					} else {
						// no null info						
						
						if (trafficItems.containsKey(item.getId())) {
							// Item with same ID is already in the map.
							
							if (item.toString().equals(trafficItems.get(item.getId()).toString())) {
								// Everything is fine. No need for adding the item to the map.
								counterIgnoredAlreadyInMap++;
								
							} else {
								// The traffic item information is different.
								// Check if this is a normal update (e.g. of the endtime) or a minor correction of the coordinates. Otherwise throw a runtime exception.
								update(item);
								counterUpdated++;
							}
							
						} else {
							// Traffic item ID not yet in map.
							counterNew++;
							trafficItems.put(item.getId(), item);
						}
					}
				}
				
				if (counterNew > 0) log.info(" +++ " + counterNew + " new traffic items added to map.");
				if (counterIgnoredAlreadyInMap > 0) log.info(" +++ " + counterIgnoredAlreadyInMap + " traffic items ignored (already in the map).");
				if (counterIgnoredNullInfoItem > 0) log.info(" +++ " + counterIgnoredNullInfoItem + " traffic items ignored (null info).");				
				if (counterUpdated > 0) log.info(" +++ " + counterUpdated + " traffic items may be updated (if they are more recently downloaded or an update of the previous message).");
			}
		}
		
		log.info("Collecting traffic items from all xml files in directory " + this.inputDirectory + "... Done.");
		
		if (!foundXMLFile) {
			throw new RuntimeException("No *.xml or *.xml.gz file found in directory " + this.inputDirectory + ". Aborting...");
		}		
	}
		
	private void update(TrafficItem item) {
		// see what has to be updated...
		
		if ( item.getOrigin().toString().equals(trafficItems.get(item.getId()).getOrigin().toString()) &&
				item.getTo().toString().equals(trafficItems.get(item.getId()).getTo().toString()) &&
				item.getTMCAlert().toString().equals(trafficItems.get(item.getId()).getTMCAlert().toString()) &&
				(!item.getEndDateTime().equals(trafficItems.get(item.getId()).getEndDateTime())) ) {
			
			// same locations, same messages, different end times...
			
			log.info("Only the start/end times differ...");
			log.info("New item: " + item.toStringWithDownloadTime());
			log.info("Existing item: " + trafficItems.get(item.getId()).toStringWithDownloadTime());
			
			updateLatestInfoButKeepPreviousStartTime(item);
			
		} else if (item.getOrigin().toString().equals(trafficItems.get(item.getId()).getOrigin().toString()) &&
				item.getTo().toString().equals(trafficItems.get(item.getId()).getTo().toString()) &&
				(!item.getTMCAlert().toString().equals(trafficItems.get(item.getId()).getTMCAlert().toString())) ) {

			// same locations, different messages
			
			log.warn("Same location but different messages...");
			log.warn("New item: " + item.toStringWithDownloadTime());
			log.warn("Existing item: " + trafficItems.get(item.getId()).toStringWithDownloadTime());
			
			if (TMCAlerts.trafficItemIsAnUpdate(item) && (!TMCAlerts.trafficItemIsAnUpdate(trafficItems.get(item.getId()))) ) {
				// the new item is an update message but the existing one is not
				
				log.warn("The new item is a traffic update item, the existing item is normal traffic item. Setting the end time of the existing (normal) item to the start time of the new (update) item.");
				
				trafficItems.get(item.getId()).setEndDateTime(item.getStartDateTime());
	
			} else if ( (!TMCAlerts.trafficItemIsAnUpdate(item)) && TMCAlerts.trafficItemIsAnUpdate(trafficItems.get(item.getId())) ) {
				// the existing item is an update message but the new one is not

				log.warn("The existing item is a traffic update item, the new item is a normal traffic item. Setting the end time of the new (normal) item to the start time of the existing (update) item.");
				
				item.setEndDateTime(trafficItems.get(item.getId()).getStartDateTime());
				
				log.warn("Replacing the existing (update) item by the new (normal) item."); 
				
				trafficItems.put(item.getId(), item);
			
			} else {
				
				// just use the download time to find out what is the more recent information
				
				log.warn("Same traffic item IDs and location but different messages should only be possible if traffic item was updated by an update message.");
				log.warn("Check if one of the following messages is an update message and if yes, add the code to " + tmc.getClass());
				log.warn("New item: " + item.getTMCAlert().getPhraseCode().toString() + ": " + item.getTMCAlert().getDescription());
				log.warn("Existing item: " + trafficItems.get(item.getId()).getTMCAlert().getPhraseCode() + ": " + trafficItems.get(item.getId()).getTMCAlert().getDescription());
				
				updateLatestInfoButKeepPreviousStartTime(item);
				
				log.warn("Updated item: " + trafficItems.get(item.getId()).toStringWithDownloadTime());
			}
			
		} else {
			log.warn("Same traffic item ID should only be possible if the end time was updated or the traffic item was updated by an update message.");
			log.warn("Check the difference between the following traffic items:");
			log.warn("New item: " + item.toStringWithDownloadTime());
			log.warn("Existing item: " + trafficItems.get(item.getId()).toStringWithDownloadTime());
			
			updateLatestInfoButKeepPreviousStartTime(item);
			
			log.warn("Updated item: " + trafficItems.get(item.getId()).toStringWithDownloadTime());
		}		
	}
	
	private void updateTrafficItems() throws IOException {
		
		log.info("Updating all traffic items using the update message codes or the original Id information...");
		Set<String> updateItemsToBeDeleted = new HashSet<>();

		for (TrafficItem updateItem : this.trafficItems.values()) {
			
			if (TMCAlerts.trafficItemIsAnUpdate(updateItem)) {
								
				if (this.trafficItems.get(updateItem.getOriginalId()) == null) {
					// original traffic item not in map
					
				} else {
					TrafficItem originalItem = this.trafficItems.get(updateItem.getOriginalId());

					if (updateItem.getOrigin().toString().equals(originalItem.getOrigin().toString()) && updateItem.getTo().toString().equals(originalItem.getTo().toString())) {
						// the update and original traffic items' locations are the same
						
					} else {
						
						if (originalItem.getOrigin() == null && originalItem.getTo() == null) {
							// Ok, probably some minor location coordinate corrections. Proceed...
							
						} else {
							log.warn("An update message should only update the incident's endtime. The location should remain the same. Compare the following traffic items:");
							log.warn("Normal traffic item: " + originalItem);
							log.warn("Update traffic item: " + updateItem);
							
							log.warn("If the from and to locations' descriptions are not the same: Assuming that this is still the update for the previous traffic item. Proceed....");
						}						
					}
					originalItem.setEndDateTime(updateItem.getStartDateTime());
					updateItemsToBeDeleted.add(updateItem.getId());
				}
				
			} else {

				// some traffic items have no update code but provide update information for an existing traffic item
				
				if (updateItem.getId().equals(updateItem.getOriginalId())) {
					// The item's Id and original Id are the same. Considering this item not to be an update item.
					
				} else {
					// This item is considered as an update item providing information for an existing traffic item (original Id).
					
					if (this.trafficItems.get(updateItem.getOriginalId()) == null) {
						// original traffic item not in map
						
					} else {
						TrafficItem originalItem = this.trafficItems.get(updateItem.getOriginalId());

						if (updateItem.getOrigin().toString().equals(originalItem.getOrigin().toString())
								&& updateItem.getTo().toString().equals(originalItem.getTo().toString())
								&& updateItem.getTMCAlert().getPhraseCode().equals(originalItem.getTMCAlert().getPhraseCode())) {
							
							// the update and original traffic items' locations and alert codes are the same
							// only update the end time
							
							originalItem.setEndDateTime(updateItem.getEndDateTime());
							updateItemsToBeDeleted.add(updateItem.getId());
							
						} else {
							
							if (updateItem.getTMCAlert().getPhraseCode().equals(originalItem.getTMCAlert().getPhraseCode()) ||  ( updateItem.getTMCAlert().getPhraseCode().startsWith("Q1(") && originalItem.getTMCAlert().getPhraseCode().startsWith("Q1("))) {
								
//								log.warn("An update item should only update the incident's endtime. The location and alert code should remain the same. Compare the following traffic items:");
//								log.warn("Original traffic item: " + originalItem);
//								log.warn("Update traffic item: " + updateItem);
//								log.warn("The same alert code --> Updating the end time and the location information...");
								
								originalItem.setEndDateTime(updateItem.getEndDateTime());
								originalItem.setOrigin(updateItem.getOrigin());
								originalItem.setTo(updateItem.getTo());
								
								updateItemsToBeDeleted.add(updateItem.getId());
							
							} else {
								
								log.warn("An update item should only update the incident's endtime. The location and alert code should remain the same. Compare the following traffic items:");
								log.warn("Original traffic item: " + originalItem);
								log.warn("Update traffic item: " + updateItem);
								log.warn("Different alert codes --> Considering the update traffic item as a new traffic item...");								
							}
						}						
					}
				}				
			}
		}
		log.info("+++ " + updateItemsToBeDeleted.size() + " original traffic item(s) updated according to update message(s)");
		for (String updateItemId : updateItemsToBeDeleted) {
			this.trafficItems.remove(updateItemId);
		}
	}

	private void updateLatestInfoButKeepPreviousStartTime(TrafficItem item) {

		if ( item.getDownloadTime() > trafficItems.get(item.getId()).getDownloadTime() ) {
			log.info("Replacing the traffic item in the map by the more recent information (but keep the previous start time).");
			
			item.setStartDateTime(trafficItems.get(item.getId()).getStartDateTime());
			trafficItems.put(item.getId(), item);
		
		} else {
			
			log.info("Do not replacing the traffic item in the map because it is the more recent information.");
			
			if (item.getStartDateTime().equals(trafficItems.get(item.getId()).getStartDateTime())) {
				log.info("Same start time. No need to adjust the incident's start time.");
			} else {
				log.info("Updating the incident's start time to the previous traffic item's start time.");
				
				trafficItems.get(item.getId()).setStartDateTime(item.getStartDateTime());
			}
		}		
	}

	private Scenario loadScenario() {
		log.info("Loading scenario...");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		log.info("Loading scenario... Done.");
		return scenario;
	}

	public Map<String, TrafficItem> getTrafficItems() {
		return trafficItems;
	}
	
}
