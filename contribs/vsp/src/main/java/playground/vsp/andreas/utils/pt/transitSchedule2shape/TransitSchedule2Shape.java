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

package playground.vsp.andreas.utils.pt.transitSchedule2shape;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

/**
 * Converts a given transit schedule to shape. Extracts mode of transport. Allows to restrict the partransit lines to be converted by the iteration they are found.
 * 
 * @author aneumann
 *
 */
public class TransitSchedule2Shape {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String iteration = "380";
		final String runId = "run71";
		String targetCoordinateSystem = TransformationFactory.WGS84_UTM33N; // Berlin

		final String SCHEDULEFILE = "f:/p_runs/txl/" + runId + "/it." + iteration + "/" + runId + "." + iteration + ".transitSchedule.xml.gz";
		final String COOPLOGGERFILE = "f:/p_runs/txl/" + runId + "/" + runId + ".pCoopLogger.txt";
		final String ALLLINESSHAPEOUTFILE = "f:/p_runs/txl/" + runId + "/it." + iteration + "/" + runId + "." + iteration + ".transitSchedule.shp";
		final String PARAINBUSINESSSHAPEOUTFILE = "f:/p_runs/txl/" + runId + "/it." + iteration + "/" + runId + "." + iteration + ".transitSchedule_para_in_business.shp";
		final int removeAllParatransitLinesYoungerThanIteration = 2729;
		final String pIdentifier = "para_";
		
//		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/onlySpandauBusLines.xml.gz";
//		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_basecase.xml.gz";
//		final String SHAPEOUTFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/onlySpandauBusLines.shp";
		
		final String NETWORKFILE  = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
		
//		Set<Id> linesToRemove = new TreeSet<Id>();
//		linesToRemove.add(new IdImpl("130-B-130"));
//		linesToRemove.add(new IdImpl("131-B-131"));
//		linesToRemove.add(new IdImpl("134-B-134"));
//		linesToRemove.add(new IdImpl("135-B-135"));
//		linesToRemove.add(new IdImpl("136-B-136"));
//		linesToRemove.add(new IdImpl("234-B-234"));
//		linesToRemove.add(new IdImpl("236-B-236"));
//		linesToRemove.add(new IdImpl("237-B-237"));
//		linesToRemove.add(new IdImpl("334-B-334"));
//		linesToRemove.add(new IdImpl("337-B-337"));
//		linesToRemove.add(new IdImpl("M32-B-832"));
//		linesToRemove.add(new IdImpl("M37-B-837"));
		
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
//		new TransitScheduleReaderV1(transitSchedule, network).readFile(SCHEDULEFILE);
		new TransitScheduleReaderV1(transitSchedule, new RouteFactories()).readFile(SCHEDULEFILE);
	
		Map<Id, SortedMap<String, Object>> lineAttributesMap = TransitSchedule2Shape.getAttributesForLines(transitSchedule, pIdentifier);
		Collection<Id> linesToConvert = TransitSchedule2Shape.getIdsFromAllLinesButParatransitYoungerThanIterationGiven(transitSchedule, pIdentifier, removeAllParatransitLinesYoungerThanIteration);
		DaShapeWriter.writeTransitLines2Shape(ALLLINESSHAPEOUTFILE, transitSchedule, linesToConvert, lineAttributesMap, targetCoordinateSystem);
		
		linesToConvert = TransitSchedule2Shape.getIdsFromCoopLoggerInBusinessOnly(transitSchedule, COOPLOGGERFILE);
		DaShapeWriter.writeTransitLines2Shape(PARAINBUSINESSSHAPEOUTFILE, transitSchedule, linesToConvert, lineAttributesMap, targetCoordinateSystem);
	}
	
	public static Map<Id, SortedMap<String, Object>> getAttributesForLines(TransitSchedule transitSchedule, String pIdentifier){
		Map<Id, SortedMap<String, Object>> lineAttributesMap = new HashMap<Id, SortedMap<String, Object>>();
		final String ptTransportMode = "ptMode";
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			SortedMap<String, Object> lineAttributes = new TreeMap<String, Object>();
			
			if (transitLine.getId().toString().contains("-B-") ) {
				lineAttributes.put(ptTransportMode, new String("bus"));
			} else if (transitLine.getId().toString().contains("-T-")) {
				lineAttributes.put(ptTransportMode, new String("tram"));
			} else if (transitLine.getId().toString().contains("SB_")) {
				lineAttributes.put(ptTransportMode, new String("s-bahn"));
			} else if (transitLine.getId().toString().contains("-U-")) {
				lineAttributes.put(ptTransportMode, new String("u-bahn"));
			} else if (transitLine.getId().toString().contains(pIdentifier)) {
				lineAttributes.put(ptTransportMode, new String("para"));
			} else {
				lineAttributes.put(ptTransportMode, new String("other"));
			}
			
			lineAttributesMap.put(transitLine.getId(), lineAttributes);
		}
		
		return lineAttributesMap;
	}

	private static Collection<Id> getIdsFromAllLinesButParatransitYoungerThanIterationGiven(TransitSchedule transitSchedule, String pIdentifier, int removeAllParatransitLinesYoungerThanIteration) {

		Collection<Id> linesToConvert = new TreeSet<Id>();
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			if (transitLine.getId().toString().contains(pIdentifier)) {
				try {
					int iterationTheParatransitLineWasFound = Integer.valueOf(transitLine.getId().toString().split("_")[1]);
					if (iterationTheParatransitLineWasFound <= removeAllParatransitLinesYoungerThanIteration) {
						linesToConvert.add(transitLine.getId());
					}
				} catch (NumberFormatException e) {
					// its a paratransit line with initial schedule - keep it
					linesToConvert.add(transitLine.getId());
				}

			} else {
				linesToConvert.add(transitLine.getId());
			}
		}
		
		return linesToConvert;
	}
	
	private static Collection<Id> getIdsFromCoopLoggerInBusinessOnly(TransitSchedule transitSchedule, String coopLoggerFile) {
		Set<String> coopsToKeep = ReadCoopLoggerFileAndReturnCoopsInBusiness.readCoopLoggerFileAndReturnCoopsInBusiness(coopLoggerFile);
		Collection<Id> linesToConvert = new TreeSet<Id>();
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			if (coopsToKeep.contains(transitLine.getId().toString())) {
				linesToConvert.add(transitLine.getId());
			}
		}
		return linesToConvert;
	}
}
