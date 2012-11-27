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

//	private String runFolder = "/Users/Ihab/Desktop/RUN2/";
	private String outputFolder = "/Users/Ihab/Desktop/analysis_output/A_TC_10000/";
	private String runFolder = "/Users/Ihab/ils/kaddoura/welfareBusCorridor_opt3/output/A_TC_10000/";

	
	public static void main(String[] args) {
		AnaMain ana = new AnaMain();
		ana.run();
	}

	private void run() {
		
		ExtItOutputAnalyzer dataReader = new ExtItOutputAnalyzer(runFolder);
		dataReader.loadData();
		dataReader.loadParameterData();
		
		File file = new File(outputFolder);
		file.mkdirs();
		
		dataReader.writeWelfareData(outputFolder);
		dataReader.writeGlobalMaxWelfareMatrix(outputFolder);
		dataReader.writeNumberOfBuses2optimalFareFrequency(outputFolder);
		dataReader.writeFare2optimalNumberOfBusesFrequency(outputFolder);
	}

}
