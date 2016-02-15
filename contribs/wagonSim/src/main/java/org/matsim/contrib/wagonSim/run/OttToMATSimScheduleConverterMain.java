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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.schedule.OTTDataContainer;
import org.matsim.contrib.wagonSim.schedule.OTTDataToMATSimScheduleConverter;
import org.matsim.contrib.wagonSim.schedule.OTTScheduleParser;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author balmermi
 *
 */
public class OttToMATSimScheduleConverterMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(OttToMATSimScheduleConverterMain.class);
	
	private final Scenario scenario;
	private final ObjectAttributes vehicleAttributes = new ObjectAttributes();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public OttToMATSimScheduleConverterMain() {
		Config config = Utils.getDefaultWagonSimConfig();
		scenario = ScenarioUtils.createScenario(config);
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void convertFromFiles(String ottFile, Network infraNetwork, String nodeMapFile, String trainTypesFile, boolean isPerformance) throws IOException {
		Map<Id<Node>, Id<Node>> nodeMap = Utils.parseNodeMapFile(nodeMapFile);
		log.info("node map file contains "+nodeMap.size()+" mappings.");
		ObjectAttributes trainTypes = Utils.parseTrainTypesFile(trainTypesFile);
		OTTDataContainer dataContainer = new OTTDataContainer();
		new OTTScheduleParser(dataContainer).parse(ottFile, nodeMap);
		
		OTTDataToMATSimScheduleConverter converter = new OTTDataToMATSimScheduleConverter(scenario,vehicleAttributes);
		converter.convert(dataContainer,infraNetwork,trainTypes,isPerformance);
	}
	
	//////////////////////////////////////////////////////////////////////

	public final Scenario getScenario() {
		return this.scenario;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final ObjectAttributes getVehicleAttributes() {
		return this.vehicleAttributes;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

//		args = new String[] {
//				"S:/raw/europe/ch/ch/sbb/0002/20130715_Fahrplandaten/OTT_20120308.csv",
//				"S:/raw/europe/ch/ch/sbb/0002/20130715_Fahrplandaten/Traintypes.csv",
//				"S:/raw/europe/ch/ch/sbb/0002/20130524_Daten_Infrastruktur/._infra.xml",
//				"S:/raw/europe/ch/ch/sbb/0002/20130828_nodeMergeList/NodeMap.csv",
//				"true", // true := performance schedule; false := target schedule
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance",
//		};
		
		if (args.length != 6) {
			log.error(OttToMATSimScheduleConverterMain.class.getCanonicalName()+" ottFile trainTypesFile nemoInfraXmlFile nodeMapFile isPerformance outputBase");
			System.exit(-1);
		}
		
		String ottFile = args[0];
		String trainTypesFile = args[1];
		String nemoInfraXmlFile = args[2];
		String nodeMapFile = args[3];
		boolean isPerformance = Boolean.parseBoolean(args[4]);
		String outputBase = args[5];
		
		log.info("Main: "+OttToMATSimScheduleConverterMain.class.getCanonicalName());
		log.info("ottFile: "+ottFile);
		log.info("trainTypesFile: "+trainTypesFile);
		log.info("nemoInfraXmlFile: "+nemoInfraXmlFile);
		log.info("nodeMapFile: "+nodeMapFile);
		log.info("isPerformance: "+isPerformance);
		log.info("outputBase: "+outputBase);

		NEMOInfraToMATSimNetworkConverterMain networkConverter = new NEMOInfraToMATSimNetworkConverterMain();
		networkConverter.convertFromFile(nemoInfraXmlFile);
		
		OttToMATSimScheduleConverterMain converter = new OttToMATSimScheduleConverterMain();
		converter.convertFromFiles(ottFile,networkConverter.getScenario().getNetwork(),nodeMapFile,trainTypesFile,isPerformance);
		
		MATSimInfraOttNetworkMergerMain merger = new MATSimInfraOttNetworkMergerMain();
		merger.mergeNetworks(networkConverter.getScenario().getNetwork(),converter.getScenario().getNetwork());
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		if (!Utils.prepareFolder(outputBase+"/merged")) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		new NetworkWriter(merger.getMergedNetwork()).write(outputBase+"/merged/network.merged.xml.gz");
		
		if (isPerformance) {
			new NetworkWriter(converter.getScenario().getNetwork()).write(outputBase+"/network.ott.performance.xml.gz");
			new TransitScheduleWriter(converter.getScenario().getTransitSchedule()).writeFile(outputBase+"/transitSchedule.ott.performance.xml.gz");
			Utils.writeShuntingTimes(converter.getScenario(),null,outputBase+"/shuntingTimes.ott.performance.txt");
			new VehicleWriterV1(((MutableScenario)converter.getScenario()).getTransitVehicles()).writeFile(outputBase+"/transitVehicles.ott.performance.xml.gz");
			Utils.writeShuntingTable(converter.getScenario().getTransitSchedule(),outputBase+"/shuntingTable.ott.performance.txt");
			new ObjectAttributesXmlWriter(converter.getVehicleAttributes()).writeFile(outputBase+"/transitVehicleAttributes.ott.performance.xml.gz");
		}
		else {
			new NetworkWriter(converter.getScenario().getNetwork()).write(outputBase+"/network.ott.target.xml.gz");
			new TransitScheduleWriter(converter.getScenario().getTransitSchedule()).writeFile(outputBase+"/transitSchedule.ott.target.xml.gz");
			Utils.writeShuntingTimes(converter.getScenario(),null,outputBase+"/shuntingTimes.ott.target.txt");
			new VehicleWriterV1(((MutableScenario)converter.getScenario()).getTransitVehicles()).writeFile(outputBase+"/transitVehicles.ott.target.xml.gz");
			Utils.writeShuntingTable(converter.getScenario().getTransitSchedule(),outputBase+"/shuntingTable.ott.target.txt");
			new ObjectAttributesXmlWriter(converter.getVehicleAttributes()).writeFile(outputBase+"/transitVehicleAttributes.ott.target.xml.gz");
		}
	}
}
