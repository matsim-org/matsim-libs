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
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.shunting.ShuntingTableToMATSimScheduleEnricher;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author balmermi
 *
 */
public class MATSimScheduleEnricherMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(MATSimScheduleEnricherMain.class);
	
	private final Scenario scenario;
	private final ObjectAttributes vehicleAttributes;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public MATSimScheduleEnricherMain(String networkFile, String transitScheduleFile, String transitVehiclesFile, String transitVehicleAttributesFile) {
		Config config = Utils.getDefaultWagonSimConfig();
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new TransitScheduleReader(scenario).readFile(transitScheduleFile);
		new VehicleReaderV1(((MutableScenario)scenario).getTransitVehicles()).readFile(transitVehiclesFile);
		vehicleAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(vehicleAttributes).readFile(transitVehicleAttributesFile);
	}

	//////////////////////////////////////////////////////////////////////
	
	public final void enrich(String shuntingTableFile, double minDwellTime) throws IOException {
		ShuntingTableToMATSimScheduleEnricher enricher = new ShuntingTableToMATSimScheduleEnricher(scenario,vehicleAttributes);

		Map<Id<TransitLine>, Map<Id<Node>, Boolean>> shuntingTable = Utils.parseShuntingTable(shuntingTableFile);
		enricher.enrich(shuntingTable,minDwellTime);
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
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/network.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitSchedule.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicles.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicleAttributes.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/shuntingTable.ott.performance.txt",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/shuntingTimes.ott.performance.txt",
//				"999999",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched",
//		};
		
		if (args.length != 8) {
			log.error(MATSimScheduleEnricherMain.class.getCanonicalName()+" networkFile transitScheduleFile transitVehiclesFile transitVehicleAttributesFile shuntingTableFile shuntingTimesFile minDwellTime outputBase");
			System.exit(-1);
		}
		
		String networkFile = args[0];
		String transitScheduleFile = args[1];
		String transitVehiclesFile = args[2];
		String transitVehicleAttributesFile = args[3];
		String shuntingTableFile = args[4];
		String shuntingTimesFile = args[5];
		Double minDwellTime = Double.parseDouble(args[6]);
		String outputBase = args[7];
		
		log.info("Main: "+MATSimScheduleEnricherMain.class.getCanonicalName());
		log.info("networkFile: "+networkFile);
		log.info("transitScheduleFile: "+transitScheduleFile);
		log.info("transitVehiclesFile: "+transitVehiclesFile);
		log.info("transitVehicleAttributesFile: "+transitVehicleAttributesFile);
		log.info("shuntingTableFile: "+shuntingTableFile);
		log.info("shuntingTimesFile: "+shuntingTimesFile);
		log.info("outputBase: "+outputBase);
		
		MATSimScheduleEnricherMain enricher = new MATSimScheduleEnricherMain(networkFile, transitScheduleFile, transitVehiclesFile, transitVehicleAttributesFile);
		enricher.enrich(shuntingTableFile, minDwellTime);
		
		Map<Id<TransitStopFacility>, Double> shuntingTimes = Utils.parseShuntingTimes(shuntingTimesFile);
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		new NetworkWriter(enricher.getScenario().getNetwork()).write(outputBase+"/network.enriched.xml.gz");
		new TransitScheduleWriter(enricher.getScenario().getTransitSchedule()).writeFile(outputBase+"/transitSchedule.enriched.xml.gz");
		Utils.writeShuntingTimes(enricher.getScenario(), shuntingTimes,outputBase+"/shuntingTimes.enriched.txt");
		new VehicleWriterV1(((MutableScenario)enricher.getScenario()).getTransitVehicles()).writeFile(outputBase+"/transitVehicles.enriched.xml.gz");
		Utils.writeShuntingTable(enricher.getScenario().getTransitSchedule(),outputBase+"/shuntingTable.enriched.txt");
		new ObjectAttributesXmlWriter(enricher.getVehicleAttributes()).writeFile(outputBase+"/transitVehicleAttributes.enriched.xml.gz");
	}
}
