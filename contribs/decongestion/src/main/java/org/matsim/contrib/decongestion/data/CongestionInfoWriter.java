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
package org.matsim.contrib.decongestion.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * @author ikaddoura
 *
 */
public class CongestionInfoWriter {
	private static final Logger log = Logger.getLogger(CongestionInfoWriter.class);
	
	public static void writeDelays(DecongestionInfo congestionInfo, int iteration, String outputPath) {
				
		String outputPathCongestionInfo = outputPath;
		File dir = new File(outputPathCongestionInfo);
		dir.mkdirs();
		
		log.info("Writing csv file...");
		
		String fileName = outputPathCongestionInfo + "delays_perLinkAndTimeBin.csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			int totalNumberOfTimeBins = (int) (congestionInfo.getScenario().getConfig().travelTimeCalculator().getMaxTime() / congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
			
			bw.write("Link Id");
			for (int i = 0; i < totalNumberOfTimeBins; i++) {
				double timeInterval = (i + 1) * congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
				bw.write(";" + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			}
			bw.newLine();
			
			for (Id<Link> linkId : congestionInfo.getlinkInfos().keySet()) {
				
				bw.write(linkId.toString());
				
				for (int i = 0; i < totalNumberOfTimeBins; i++) {
										
					double timeBinValue1 = 0.;
					if (congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().containsKey(i)) {
						timeBinValue1 = congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(i);
					}
					
					bw.write(";" + timeBinValue1);
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		if (congestionInfo.getDecongestionConfigGroup().isWRITE_LINK_INFO_CHARTS()) {
			log.info("Writing png file...");

			XYLineChart chart = new XYLineChart("Iteration " + iteration, "Time of day [hours]", "Average delay [seconds]");
			
			int totalNumberOfTimeBins = (int) (congestionInfo.getScenario().getConfig().travelTimeCalculator().getMaxTime() / congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
							
			double[] timeBins = new double[totalNumberOfTimeBins];
			for (int i = 0; i < totalNumberOfTimeBins; i++) {
				double timeInterval = (i + 1) * congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
				timeBins[i] = timeInterval / 3600.;
			}
			
			for (Id<Link> linkId : congestionInfo.getlinkInfos().keySet()) {
				
				double[] values = new double[totalNumberOfTimeBins];	
				boolean isEmpty = true;
				
				for (Integer i : congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().keySet()) {
					values[i] = congestionInfo.getlinkInfos().get(linkId).getTime2avgDelay().get(i);
					if (values[i] > 0.) {
						isEmpty = false;
					}
				}
				if (!isEmpty) chart.addSeries("Link " + linkId, timeBins, values);
			}
			chart.saveAsPng(outputPathCongestionInfo + "delays_perLinkAndTimeBin.png", 800, 600);
			log.info("Output written to " + outputPathCongestionInfo + "delays_perLinkAndTimeBin.png");

		}
		
	}
	
	public static void writeTolls(DecongestionInfo congestionInfo, int iteration, String outputPath) {
		
		String outputPathCongestionInfo = outputPath;
		File dir = new File(outputPathCongestionInfo);
		dir.mkdirs();
		
		log.info("Writing csv file...");
		
		String fileName2 = outputPathCongestionInfo + "toll_perLinkAndTimeBin.csv";
		File file2 = new File(fileName2);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			
			int totalNumberOfTimeBins = (int) (congestionInfo.getScenario().getConfig().travelTimeCalculator().getMaxTime() / congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
			
			bw.write("Link Id");
			for (int i = 0; i < totalNumberOfTimeBins; i++) {
				double timeInterval = (i + 1) * congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
				bw.write(";" + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			}
			bw.newLine();
			
			for (Id<Link> linkId : congestionInfo.getlinkInfos().keySet()) {
				
				bw.write(linkId.toString());
				
				for (int i = 0; i < totalNumberOfTimeBins; i++) {
										
					double toll = 0.;
					if (congestionInfo.getlinkInfos().get(linkId).getTime2toll().containsKey(i)) {
						toll = congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(i);
					}
					
					bw.write(";" + toll);
				}
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName2);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (congestionInfo.getDecongestionConfigGroup().isWRITE_LINK_INFO_CHARTS()) {
			log.info("Writing png file...");
			
			XYLineChart chart = new XYLineChart("Iteration " + iteration, "Time of day [hours]", "Toll [monetary units]");
			
			int totalNumberOfTimeBins = (int) (congestionInfo.getScenario().getConfig().travelTimeCalculator().getMaxTime() / congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
							
			double[] timeBins = new double[totalNumberOfTimeBins];
			for (int i = 0; i < totalNumberOfTimeBins; i++) {
				double timeInterval = (i + 1) * congestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
				timeBins[i] = timeInterval / 3600.;
			}
			
			for (Id<Link> linkId : congestionInfo.getlinkInfos().keySet()) {
				
				double[] values = new double[totalNumberOfTimeBins];	
				boolean isEmpty = true;
				
				for (Integer i : congestionInfo.getlinkInfos().get(linkId).getTime2toll().keySet()) {
					values[i] = congestionInfo.getlinkInfos().get(linkId).getTime2toll().get(i);
					if (values[i] > 0.) {
						isEmpty = false;
					}
				}
				if (!isEmpty) chart.addSeries("Link " + linkId, timeBins, values);
			}
			chart.saveAsPng(outputPathCongestionInfo + "toll_perLinkAndTimeBin.png", 800, 600);
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
			
			bw.write("Iteration ; Total delay [hours] ; Total congestion toll payments [monetary units] ; Total travel time [hours]; Total user benefits [monetary units]; System welfare (only considering congestion toll revenues, other revenues are neglected [monetary units]");
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
