/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ikaddoura.intervalBasedCongestionPricing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo;

/**
 * 
 * @author ikaddoura
 *
 */
public class CongestionInfoWriter {
	private static final Logger log = Logger.getLogger(CongestionInfoWriter.class);
	private static final boolean writeCSVTFiles = false;
	
	public static void writeCongestionInfoTimeInterval(CongestionInfo congestionInfo, String outputPath) {
		double timeInterval = congestionInfo.getCurrentTimeBinEndTime();
		
		String outputPathCongestionInfo = outputPath + "congestionInfo/";
		File dir = new File(outputPathCongestionInfo);
		dir.mkdirs();
		
		String fileName = outputPathCongestionInfo + "congestionInfo_" + timeInterval + ".csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id"
					+ " ; Freespeed travel time [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Number of agents leaving the link " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Total travel time [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Average travel time [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Travel time last agent [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Total delay [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Delay last agent [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Maximum travel time [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					+ " ; Maximum delay [sec] " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS)
					);
			bw.newLine();
			
			for (Id<Link> linkId : congestionInfo.getCongestionLinkInfos().keySet()){
				
				int numberOfAgents = congestionInfo.getCongestionLinkInfos().get(linkId).getLeavingVehicles().size();
				
				if (numberOfAgents > 0) {
					double travelTimeSum_sec = congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeSum_sec();
					double averageTravelTime_sec = travelTimeSum_sec / numberOfAgents;
					double travelTimeLastLeavingAgent_sec = congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeLastLeavingAgent_sec();
					double freespeedTravelTime_sec = Math.round(congestionInfo.getScenario().getNetwork().getLinks().get(linkId).getLength() / congestionInfo.getScenario().getNetwork().getLinks().get(linkId).getFreespeed());
					double totalDelay_sec = travelTimeSum_sec - (numberOfAgents * freespeedTravelTime_sec);
					double delayLastAgent_sec = travelTimeLastLeavingAgent_sec - freespeedTravelTime_sec;
					double maximumTravelTime_sec = congestionInfo.getCongestionLinkInfos().get(linkId).getTravelTimeMaximum();
					double maximumDelay_sec = maximumTravelTime_sec - freespeedTravelTime_sec;
					
					bw.write(linkId.toString()
							+ ";" + freespeedTravelTime_sec
							+ ";" + numberOfAgents
							+ ";"+ travelTimeSum_sec
							+ ";" + averageTravelTime_sec
							+ ";" + travelTimeLastLeavingAgent_sec
							+ ";" + totalDelay_sec
							+ ";" + delayLastAgent_sec
							+ ";" + maximumTravelTime_sec
							+ ";" + maximumDelay_sec
							);
					bw.newLine();
				}
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (writeCSVTFiles) {
			File file2 = new File(fileName + "t");
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
				bw.write("\"String\",\"Real\",\"Real\"");
							
				bw.newLine();
				
				bw.close();
				log.info("Output written to " + fileName + "t");
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
	
	public static void writeIterationStats(
			SortedMap<Integer, Double> iteration2totalDelay,
			SortedMap<Integer, Double> iteration2totalTollPayments,
			SortedMap<Integer, Double> iteration2totalTravelTime,
			SortedMap<Integer, Double> iteration2userBenefits,
			String outputDirectory) {
		
		String fileName = outputDirectory + "congestionInfo.csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Iteration ; Total delay [hours] ; Total toll payments [monetary units] ; Total travel time [hours]; Total user benefits [monetary units]; System welfare [monetary units]");
			bw.newLine();
			
			for (Integer iteration : iteration2totalDelay.keySet()) {
				bw.write(iteration + " ; "
						+ iteration2totalDelay.get(iteration) / 3600. + " ; "
						+ iteration2totalTollPayments.get(iteration) + " ; "
						+ iteration2totalTravelTime.get(iteration) / 3600. + " ; "
						+ iteration2userBenefits.get(iteration) + ";"
						+ (iteration2totalTollPayments.get(iteration) + iteration2userBenefits.get(iteration))
						);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
