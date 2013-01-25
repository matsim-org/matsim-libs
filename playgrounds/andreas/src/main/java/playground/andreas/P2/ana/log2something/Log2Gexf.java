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

package playground.andreas.P2.ana.log2something;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * 
 * Converts a pLogger file into gexf format
 * 
 * @author aneumann
 *
 */
public class Log2Gexf {

	private static final Logger log = Logger.getLogger(Log2Gexf.class);
	
	
	public static void convertLog2Gexf(String inputFile, String outputFile) {
		ArrayList<LogElement> logElements = LogReader.readFile(inputFile);
		ArrayList<PlanElement> planElements = LogElement2PlanElement.logElement2PlanElement(logElements);
		planElements = FindAncestorOfPlanElements.findAncestorOfPlanElements(planElements);
		PlanElement2Gexf.planElement2Gexf(planElements, outputFile);
	}
	
	public static void main(String[] args) {
		String inputFile = "f:/bvg5_b285b.pCoopLogger.txt";
		String outputFile = "f:/bvg5_b285b.pCoopLogger_output.gexf";
		
		Log2Gexf.convertLog2Gexf(inputFile, outputFile);
	}
	
}
