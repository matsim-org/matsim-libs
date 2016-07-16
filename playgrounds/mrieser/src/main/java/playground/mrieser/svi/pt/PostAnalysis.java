/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.pt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.mrieser.svi.converters.DynusTNetworkReader;
import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.analysis.CalculateLinkTravelTimesFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;

/**
 * @author mrieser
 */
public class PostAnalysis {

	private final static Logger log = Logger.getLogger(PostAnalysis.class);
	
	public static void main(String[] args) {
		
		String modelDirectory = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen2001_final/DynusT/ohne36";
		String zoneIdToIndexMappingFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen2001_final/l41_ZoneNo_TAZ_mapping.csv";
		String ptLinesFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen2001_final/ptLines.txt";
		String vehTrajectoryFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/output_ohne36_10/ITERS/it.70/70.DynusT/VehTrajectory.dat";
		String ptStatsFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/output_ohne36_10/ITERS/it.70/70.ptStats_FIXED.txt";
		
		log.info("Reading DynusT-network..." + modelDirectory);
		Network dynusTNetwork = NetworkUtils.createNetwork();
		new DynusTNetworkReader(dynusTNetwork).readFiles(modelDirectory + "/xy.dat", modelDirectory + "/network.dat");
		
		ZoneIdToIndexMapping zoneIdToIndexMapping = new ZoneIdToIndexMapping();
		new ZoneIdToIndexMappingReader(zoneIdToIndexMapping).readFile(zoneIdToIndexMappingFilename);
		
		TravelTimeCalculator ttc = new TravelTimeCalculator(dynusTNetwork, ConfigUtils.createConfig().travelTimeCalculator());
		
		CalculateLinkTravelTimesFromVehTrajectories lttCalc = new CalculateLinkTravelTimesFromVehTrajectories(ttc, dynusTNetwork);
		new VehicleTrajectoriesReader(lttCalc, zoneIdToIndexMapping).readFile(vehTrajectoryFilename);
		
		
		PtLines ptLines = new PtLines();
		log.info("reading pt lines from " + ptLinesFilename);
		new PtLinesReader(ptLines, dynusTNetwork).readFile(ptLinesFilename);
		
		new PtLinesStatistics(ptLines).writeStatsToFile(
				ptStatsFilename,
				ttc);
		
	}
}
