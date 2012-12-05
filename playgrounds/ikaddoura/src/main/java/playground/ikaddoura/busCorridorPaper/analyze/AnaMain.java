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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.analyze;

import java.io.File;

/**
 * @author ikaddoura
 *
 */
public class AnaMain {

//	private String outputFolder = "/Users/Ihab/Desktop/analysis_output/B_NTC/";
//	private String runFolder = "/Users/Ihab/ils4/kaddoura/welfareBusCorridor_opt3/output/B_NTC/";

//	private String outputFolder = "/Users/Ihab/Desktop/analysis_output/A_TC/";
//	private String runFolder = "/Users/Ihab/ils4/kaddoura/welfareBusCorridor_opt3/output/A_TC/";
	
	private String outputFolder = "/Users/Ihab/Desktop/TEST_analysis_output/";
	private String runFolder = "/Users/Ihab/Desktop/TEST/";
	
	public static void main(String[] args) {
		AnaMain ana = new AnaMain();
		ana.run();
	}

	private void run() {
		
		ExtItOutputAnalyzer dataAnalyzer = new ExtItOutputAnalyzer(runFolder);
		dataAnalyzer.loadData();
		dataAnalyzer.loadParameterData();
		
		File file = new File(outputFolder);
		file.mkdirs();
		
		dataAnalyzer.writeWelfare(outputFolder);
		dataAnalyzer.writePtTrips(outputFolder);
		dataAnalyzer.writeCarTrips(outputFolder);
		dataAnalyzer.writeMissedBusTrips(outputFolder);
		dataAnalyzer.writeAvgT0minusTActDivByT0perCarTrip(outputFolder);
		dataAnalyzer.writeOperatorProfit(outputFolder);
		dataAnalyzer.writeUsersLogSum(outputFolder);
		
		dataAnalyzer.writeAvgWelfareMatrix(outputFolder);
		dataAnalyzer.writeAvgPtTripsMatrix(outputFolder);
		dataAnalyzer.writeAvgCarTripsMatrix(outputFolder);
		dataAnalyzer.writeAvgMissedBusTripsMatrix(outputFolder);
		dataAnalyzer.writeAvgAvgT0minusTActDivByT0perCarTripMatrix(outputFolder);
		dataAnalyzer.writeAvgOperatorProfitMatrix(outputFolder);
		dataAnalyzer.writeAvgUsersLogSumMatrix(outputFolder);
		
		dataAnalyzer.writeGlobalMaxWelfareMatrix(outputFolder);
//		dataAnalyzer.writeGlobalMaxOperatorProfitMatrix(outputFolder); // TODO
		dataAnalyzer.writeNumberOfBuses2optimalFareFrequency(outputFolder);
		dataAnalyzer.writeFare2optimalNumberOfBusesFrequency(outputFolder);
	}

}
