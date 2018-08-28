/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.run;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.demand.WagonDataContainer;
import org.matsim.contrib.wagonSim.demand.WagonDataParser;
import org.matsim.contrib.wagonSim.demand.WagonToMatsimDemandConverter;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author balmermi
 *
 */
public class WagonToDemandConverterMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(WagonToDemandConverterMain.class);
	
	private final ObjectAttributes wagonAttributes = new ObjectAttributes();
	private final Date demandDateTime;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public WagonToDemandConverterMain(Date demandDateTime) throws ParseException {
		this.demandDateTime = demandDateTime;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void convertFromFiles(String wagonDataFile, String nodeMapFile, String zoneToNodeMapFile, Scenario scenario, ObjectAttributes transitVehicleAttributes) throws IOException {
		Map<Id<Node>, Id<Node>> nodeMap = Utils.parseNodeMapFile(nodeMapFile);
		log.info("node map file contains "+nodeMap.size()+" mappings.");
		Map<String, Id<Node>> zoneToNodeMap = Utils.parseZoneToNodeMapFile(zoneToNodeMapFile);
		log.info("zone to node map file contains "+zoneToNodeMap.size()+" mappings.");

		Map<String, Id<Node>> remappedZoneToNodeMap = new HashMap<>();
		for (Entry<String, Id<Node>> e : zoneToNodeMap.entrySet()) {
			Id<Node> mappedNodeId = nodeMap.get(e.getValue());
			if (mappedNodeId != null) {
				remappedZoneToNodeMap.put(e.getKey(), mappedNodeId);
				log.info("remapped zone id="+e.getKey()+" from node id="+e.getValue()+" to node id="+mappedNodeId+".");
			}
			else {
				remappedZoneToNodeMap.put(e.getKey(),e.getValue());
			}
		}
		
		WagonDataContainer dataContainer = new WagonDataContainer();
		new WagonDataParser(dataContainer,demandDateTime).parse(wagonDataFile);
		
		new WagonToMatsimDemandConverter(scenario, wagonAttributes, transitVehicleAttributes, remappedZoneToNodeMap).convert(dataContainer);
	}
	
	//////////////////////////////////////////////////////////////////////

	public final ObjectAttributes getWagonAttributes() {
		return this.wagonAttributes;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {

//		args = new String[] {
//				"D:/tmp/sbb/0002/20130604_Wagendaten/20130603_Wagendaten_2012/wagendaten_2012_CargoRail.csv.gz",
//				"D:/tmp/sbb/0002/20130604_Wagendaten/20130603_Wagendaten_2012/zuordnung_verkehrszellen_CargoRail_2012.csv",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/network.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitSchedule.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitVehicleAttributes.enriched.xml.gz",
//				"D:/tmp/sbb/0002/20130828_nodeMergeList/NodeMap.csv",
//				"2012-06-14",
//				"14.00.00",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnrichedDemand"
//		};
				
		if (args.length != 9) {
			log.error(WagonToDemandConverterMain.class.getCanonicalName()+" wagonDataFile zoneToNodeMapFile scheduleNetworkFile scheduleFile transitVehicleAttributesFile nodeMapFile demandDate[YYYY-MM-DD] demandStartTime[HH.MM.SS] outputBase");
			System.exit(-1);
		}
		
		String wagonDataFile = args[0];
		String zoneToNodeMapFile = args[1];
		String scheduleNetworkFile = args[2];
		String scheduleFile = args[3];
		String transitVehicleAttributesFile = args[4];
		String nodeMapFile = args[5];
		String date = args[6];
		String time = args[7];
		String outputBase = args[8];
		
		log.info("Main: "+WagonToDemandConverterMain.class.getCanonicalName());
		log.info("wagonDataFile: "+wagonDataFile);
		log.info("zoneToNodeMapFile: "+zoneToNodeMapFile);
		log.info("scheduleNetworkFile: "+scheduleNetworkFile);
		log.info("sscheduleFile: "+scheduleFile);
		log.info("transitVehicleAttributesFile: "+transitVehicleAttributesFile);
		log.info("nodeMapFile: "+nodeMapFile);
		log.info("demandDate[YYYY-MM-DD]: "+date);
		log.info("demandStartTime[HH.MM.SS]: "+time);
		log.info("outputBase: "+outputBase);

		Config config = Utils.getDefaultWagonSimConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(scheduleNetworkFile);
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		ObjectAttributes transitVehicleAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(transitVehicleAttributes).readFile(transitVehicleAttributesFile);
		
		Date demandDateTime = WagonSimConstants.DATE_FORMAT_YYYYMMDDHHMMSS.parse(date+"-"+time);
		WagonToDemandConverterMain converter = new WagonToDemandConverterMain(demandDateTime);
		converter.convertFromFiles(wagonDataFile,nodeMapFile,zoneToNodeMapFile,scenario,transitVehicleAttributes);
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		new PopulationWriter(scenario.getPopulation(),null).write(outputBase+"/demand.wagons.xml.gz");
		new ObjectAttributesXmlWriter(converter.getWagonAttributes()).writeFile(outputBase+"/wagonAttributes.xml.gz");
	}
}
