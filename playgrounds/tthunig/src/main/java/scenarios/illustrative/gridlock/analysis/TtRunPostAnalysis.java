/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.gridlock.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author tthunig
 *
 */
public class TtRunPostAnalysis {

	private static String runName = "NONE_basisTWO_ALTERNATING1800.0_demandINCREASING_offset0";
//	private static String runName = "PLANBASED_basisCONFLICTING1800.0_demandINCREASING_offset0";
//	private static String runName = "DOWNSTREAM_basisCONFLICTING1800.0_demandINCREASING_offset0";
//	private static String runName = "SYLVIA_basisCONFLICTING1800.0_demandINCREASING_offset0";
//	private static String runName = "SYLVIAnoFixed_max1.5_basisCONFLICTING1800.0_demandINCREASING_offset0";
//	private static String runName = "SYLVIAnoFixed_max1_basisCONFLICTING1800.0_demandINCREASING_offset0";
	private static String runDir = "../../../runs-svn/gridlock/twoStream/mt-its/" + runName + "/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFile = runDir + "output_events.xml.gz";
		if (args != null && args.length != 0){
			eventsFile = args[0];
		}
		
		EventsManager eventsManager = new EventsManagerImpl();
		TtAnalyzeGridlockInflowOutflow handler = new TtAnalyzeGridlockInflowOutflow();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
		
		writeCumulativeCounts(handler.determineCumulativeInflowPerSec(), handler.determineCumulativeOutflowPerSec());
	}
	
	private static void writeCumulativeCounts(Map<Double, Integer> countsInflow, Map<Double, Integer> countsOutflow) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(runDir + "outInFlow.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time\tcum inflow\tcum outflow";
		stream.println(header);
		for (Double time : countsInflow.keySet()) { // note: inflow and outflow counts have the same keySet
			StringBuffer line = new StringBuffer();
			line.append(time + "\t" + countsInflow.get(time) + "\t" + countsOutflow.get(time));
			stream.println(line.toString());
		}
		stream.close();
	}

}
