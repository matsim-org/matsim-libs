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
package playground.ikaddoura.decongestion.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * @author ikaddoura
 *
 */
public class CongestionInfoWriter {
	private static final Logger log = Logger.getLogger(CongestionInfoWriter.class);
	
	public static void writeCongestionInfoTimeInterval(DecongestionInfo congestionInfo, String outputPath) {
				
		String outputPathCongestionInfo = outputPath;
		File dir = new File(outputPathCongestionInfo);
		dir.mkdirs();
		
		String fileName = outputPathCongestionInfo + "infoPerLinkAndTimeBin.csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			int totalNumberOfTimeBins = (int) ((3600. * 30) / congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
			
			bw.write("Link Id");
			for (int i = 0; i < totalNumberOfTimeBins; i++) {
				double timeInterval = (i + 1) * congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
				bw.write(";Average delay " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) +
						";Toll " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			}
			bw.newLine();
			
			for (Id<Link> linkId : congestionInfo.getlinkInfos().keySet()) {
				
				bw.write(linkId.toString());
				
				for (int i = 0; i < totalNumberOfTimeBins; i++) {
										
					double timeBinValue1 = 0.;
					if (congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().containsKey(i)) {
						timeBinValue1 = congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(i);
					}
					
					double timeBinValue2 = 0.;
					if (congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(i)) {
						timeBinValue2 = congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(i);
					}
					
					bw.write(";" + timeBinValue1 + ";" + timeBinValue2);
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
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
