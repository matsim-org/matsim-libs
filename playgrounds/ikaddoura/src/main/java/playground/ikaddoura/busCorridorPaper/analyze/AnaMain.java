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
import java.util.Map;

/**
 * The runFolder is supposed to contain the outputFolders of different randomSeed runs.
 * Each outputFolder has to contain a csv outputFile which has to match the format.
 * 
 * @author ikaddoura
 *
 */
public class AnaMain {

	private String outputFolder = "/Users/Ihab/Desktop/B_NTC_anaOutputRndSeedRuns/";
	private String runFolder = "/Users/Ihab/ils4/kaddoura/welfareBusCorridor_opt3/output/FINAL_OUTPUT/B_NTC/";

//	private String outputFolder = "/Users/Ihab/Desktop/A_TC_anaOutputRndSeedRuns/";
//	private String runFolder = "/Users/Ihab/ils4/kaddoura/welfareBusCorridor_opt3/output/FINAL_OUTPUT/A_TC/";
	
//	private String outputFolder = "/Users/Ihab/Desktop/C_TC_anaOutputRndSeedRuns/";
//	private String runFolder = "/Users/Ihab/ils4/kaddoura/welfareBusCorridor_opt3.1/output/C_TC/";
	
//	private String outputFolder = "/Users/Ihab/Desktop/TEST_analysis_output3/";
//	private String runFolder = "/Users/Ihab/Desktop/TEST/";
	
	public static void main(String[] args) {
		AnaMain ana = new AnaMain();
		ana.run();
	}

	private void run() {
		
		ExtItOutputReader dataReader = new ExtItOutputReader(runFolder);
		dataReader.readData();
		Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana = dataReader.getRunNr2itNr2ana();
		
		File file = new File(outputFolder);
		file.mkdirs();
		
		DataWriter dataWriter = new DataWriter(outputFolder, runNr2itNr2ana);
		dataWriter.writeData("Welfare");
		dataWriter.writeData("PtTrips");
		dataWriter.writeData("CarTrips");
		dataWriter.writeData("MissedBusTrips");
		dataWriter.writeData("AvgT0minusTActDivByT0perCarTrip");
		dataWriter.writeData("OperatorProfit");
		dataWriter.writeData("UserBenefits");
		dataWriter.writeData("AvgWaitingTimeAll");
		dataWriter.writeData("AvgWaitingTimeMissedBus");
		dataWriter.writeData("AvgWaitingTimeNoMissedBus");

		MatrixWriter matrixWriter = new MatrixWriter(outputFolder, runNr2itNr2ana);
		matrixWriter.writeAvgMatrix("Welfare");
		matrixWriter.writeAvgMatrix("PtTrips");
		matrixWriter.writeAvgMatrix("CarTrips");
		matrixWriter.writeAvgMatrix("MissedBusTrips");
		matrixWriter.writeAvgMatrix("AvgT0minusTActDivByT0perCarTrip");
		matrixWriter.writeAvgMatrix("OperatorProfit");
		matrixWriter.writeAvgMatrix("UserBenefits");
		matrixWriter.writeAvgMatrix("AvgWaitingTimeAll");
		matrixWriter.writeAvgMatrix("AvgWaitingTimeMissedBus");
		matrixWriter.writeAvgMatrix("AvgWaitingTimeNoMissedBus");
		
		matrixWriter.writeGlobalMaximumMatrix("Welfare");
		matrixWriter.writeGlobalMaximumMatrix("OperatorProfit");

		matrixWriter.writeNumberOfBuses2optimalFareFrequency();
		matrixWriter.writeFare2optimalNumberOfBusesFrequency();
	}

}
