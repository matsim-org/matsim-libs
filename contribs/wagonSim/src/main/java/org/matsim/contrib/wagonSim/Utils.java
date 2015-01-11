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
package org.matsim.contrib.wagonSim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author balmermi
 *
 */
public class Utils {
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public static final <T> void writeObjectIds(final Set<Id<T>> objectIds, final String outFile, final String headerName) throws IOException {
		BufferedWriter writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(outFile);
		if (headerName != null) { writer.write(headerName+"\n"); }
		TreeSet<Id<T>> sortedIds = new TreeSet<>(objectIds);
		for (Id<T> oId : sortedIds) { writer.write(oId.toString()+"\n"); }
		writer.flush();
		writer.close();
	}

	//////////////////////////////////////////////////////////////////////

	public static final boolean prepareFolder(String folder) {
		File dir = new File(folder);
		if (dir.exists()) {
			if (dir.isFile()) { return false; }
			else if (dir.list().length > 0) { return false; }
			else { return true; }
		}
		else if (dir.mkdirs()) { return true; }
		else { return false; }
	}

	//////////////////////////////////////////////////////////////////////
	
	public static final String removeSurroundingQuotes(String string) {
		if (string.startsWith("\"") && string.endsWith("\"")) { return string.substring(1,string.length()-1); }
		return string;
	}

	//////////////////////////////////////////////////////////////////////
	
	public static final Map<Id<Node>, Id<Node>> parseNodeMapFile(String nodeMapFile) throws IOException {
		Map<Id<Node>, Id<Node>> nodeMap = new HashMap<Id<Node>, Id<Node>>();
		BufferedReader br = IOUtils.getBufferedReader(nodeMapFile);
		int currRow = 0;
		String curr_line;
		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");
			Id<Node> origNodeId = Id.create(Utils.removeSurroundingQuotes(row[0].trim()), Node.class);
			Id<Node> mapNodeId = Id.create(Utils.removeSurroundingQuotes(row[1].trim()), Node.class);
			if (nodeMap.put(origNodeId,mapNodeId) != null) { throw new RuntimeException("row "+currRow+": node id="+origNodeId+" already mapped to a node. Bailing out."); }
		}
		return nodeMap;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public static final ObjectAttributes parseTrainTypesFile(String trainTypesFile) throws IOException {
		ObjectAttributes trainTypes = new ObjectAttributes();

		BufferedReader br = IOUtils.getBufferedReader(trainTypesFile);
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine();
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(Utils.removeSurroundingQuotes(header[i].trim()),i); }

		// parse rows and store nodes
		while ((curr_line = br.readLine()) != null) {
			String [] row = curr_line.split(";");
			
			String type = Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.TRAIN_TYPE)].trim());
			Double maxSpeed = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.TRAIN_MAX_SPEED)].trim()))/3.6;
			Double maxWeight = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.TRAIN_MAX_WEIGHT)].trim()))*1000.0;
			Double maxLength = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.TRAIN_MAX_LENGTH)].trim()));

			trainTypes.putAttribute(type, WagonSimConstants.TRAIN_MAX_SPEED, maxSpeed);
			trainTypes.putAttribute(type, WagonSimConstants.TRAIN_MAX_WEIGHT, maxWeight);
			trainTypes.putAttribute(type, WagonSimConstants.TRAIN_MAX_LENGTH, maxLength);
		}
		return trainTypes;
	}
	
	//////////////////////////////////////////////////////////////////////
	
 	public static final Map<String, Id<Node>> parseZoneToNodeMapFile(String zoneToNodeMapFile) throws IOException {
		Map<String, Id<Node>> zoneToNodeMap = new HashMap<>();

		BufferedReader br = IOUtils.getBufferedReader(zoneToNodeMapFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(Utils.removeSurroundingQuotes(header[i].trim()),i); }

		// parse rows and store nodes
		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");
			
			String zoneId = Utils.removeSurroundingQuotes(row[lookup.get("GVVerkehrszelle")].trim());
			Id<Node> nodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get("Abfertigungsstelle")].trim()), Node.class);
			
			if (zoneToNodeMap.put(zoneId,nodeId) != null) { throw new RuntimeException("row "+currRow+": zone id="+zoneId+" already mapped to a node. Bailing out."); }
		}
		return zoneToNodeMap;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public static final void writeShuntingTable(TransitSchedule schedule, String outputFile) throws IOException {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		bw.write(""+WagonSimConstants.SHUNTING_TABLE_LOCID+"\t"+WagonSimConstants.SHUNTING_TABLE_NODEID+"\t"+WagonSimConstants.SHUNTING_TABLE_SHUNTINGFLAG+"\n");
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			Id locId = transitLine.getId();
			if (transitLine.getRoutes().size() != 1) { throw new RuntimeException("lineId="+locId+" must define exactly one transit route. Bailing out."); }
			TransitRoute transitRoute = transitLine.getRoutes().values().iterator().next();
			for (TransitRouteStop stop : transitRoute.getStops()) {
				bw.write(locId.toString()+"\t"+stop.getStopFacility().getId().toString()+"\ttrue\n");
			}
		}
		bw.flush();
		bw.close();
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public static final Map<Id<TransitLine>, Map<Id<Node>, Boolean>> parseShuntingTable(String shuntingTableFile) throws IOException {
		Map<Id<TransitLine>, Map<Id<Node>, Boolean>> shuntingTable = new HashMap<>();
		BufferedReader br = IOUtils.getBufferedReader(shuntingTableFile);
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine();
		String [] header = curr_line.split("\t");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(Utils.removeSurroundingQuotes(header[i].trim()),i); }

		// parse rows
		while ((curr_line = br.readLine()) != null) {
			String [] row = curr_line.split("\t");
			Id<TransitLine> locId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.SHUNTING_TABLE_LOCID)].trim()), TransitLine.class);
			Id<Node> nodeId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.SHUNTING_TABLE_NODEID)].trim()), Node.class);
			Boolean shuntingAllowed = new Boolean(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.SHUNTING_TABLE_SHUNTINGFLAG)].trim()));
			Map<Id<Node>,Boolean> map = shuntingTable.get(locId);
			if (map == null) {
				map = new HashMap<>();
				shuntingTable.put(locId, map);
			}
			map.put(nodeId, shuntingAllowed);
		}
		return shuntingTable;
	}
	
	//////////////////////////////////////////////////////////////////////

	public static final Map<Id<TransitStopFacility>, Double> parseShuntingTimes(String shuntingTimesFile) throws IOException {
		Map<Id<TransitStopFacility>, Double> minShuntingTimes = new HashMap<>();
		BufferedReader br = IOUtils.getBufferedReader(shuntingTimesFile);
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine();
		String [] header = curr_line.split("\t");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(Utils.removeSurroundingQuotes(header[i].trim()),i); }

		// parse rows
		while ((curr_line = br.readLine()) != null) {
			String [] row = curr_line.split("\t");
			Id<TransitStopFacility> stationId = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.STOP_STATION_ID)].trim()), TransitStopFacility.class);
			Double minShuntingTime = new Double(Utils.removeSurroundingQuotes(row[lookup.get(WagonSimConstants.STOP_MIN_SHUNTING_TIME)].trim()));
			minShuntingTimes.put(stationId, minShuntingTime);
		}
		return minShuntingTimes;
	}

	//////////////////////////////////////////////////////////////////////

	public static final void writeShuntingTimes(Scenario scenario, Map<Id<TransitStopFacility>,Double> shuntingTimes, String outputFile) throws IOException {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		bw.write(WagonSimConstants.STOP_STATION_ID+"\t"+WagonSimConstants.STOP_MIN_SHUNTING_TIME+"\n");
		for (TransitStopFacility stopFacility : scenario.getTransitSchedule().getFacilities().values()) {
			if ((shuntingTimes != null) && shuntingTimes.containsKey(stopFacility.getId())) {
				bw.write(stopFacility.getId().toString()+"\t"+shuntingTimes.get(stopFacility.getId())+"\n");
			}
			else {
				bw.write(stopFacility.getId().toString()+"\t"+scenario.getConfig().transitRouter().getAdditionalTransferTime()+"\n");
			}
		}
		bw.flush();
		bw.close();
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public static final Config getDefaultWagonSimConfig() {
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(1);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.controler().setFirstIteration(0);
		// don't know which number is good here. From my point of view
		// this should be configurable from outside // dr, oct'13
		config.controler().setLastIteration(50);
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.linkStats().setAverageLinkStatsOverIterations(0);
		config.linkStats().setWriteLinkStatsInterval(1);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(0.0);
		config.transitRouter().setAdditionalTransferTime(15*60); // default minimum shunting time
		config.transitRouter().setSearchRadius(0);
		config.transitRouter().setExtensionRadius(0);
		// should be very slow, otherwise we will get a walk-connection
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, 0.000001);

		StrategySettings settings = new StrategySettings(Id.create("1", StrategySettings.class));
		// I think we should use SelectRandom here, as I'm not really sure what will
		// happen when we use ChangeExpBeta (own scoring). //dr, oct'13
		settings.setStrategyName(DefaultPlanStrategiesModule.Selector.SelectRandom.toString());
		settings.setWeight(0.8);
		config.strategy().addStrategySettings(settings);
		settings = new StrategySettings(Id.create("2", StrategySettings.class));
		settings.setStrategyName(DefaultPlanStrategiesModule.Names.ReRoute.toString());
		settings.setWeight(0.2);
		config.strategy().addStrategySettings(settings);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.strategy().setMaxAgentPlanMemorySize(5);
		
		ActivityParams params = new ActivityParams(WagonSimConstants.ORIGIN);
		params.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(params);
		params = new ActivityParams(WagonSimConstants.DESTINATION);
		params.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(params);

		config.qsim().setStartTime(0.0);
		config.qsim().setEndTime(4*24*3600.0);
		config.qsim().setStuckTime(3600.0);
		return config;
	}
}
