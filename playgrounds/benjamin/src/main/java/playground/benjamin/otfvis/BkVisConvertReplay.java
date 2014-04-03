/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.otfvis;

import org.apache.log4j.Logger;
import org.matsim.contrib.otfvis.OTFVis;

import playground.benjamin.BkPaths;


public class BkVisConvertReplay {
	private final static Logger logger = Logger.getLogger(BkVisConvertReplay.class);

	static boolean convert = true;
	static String runNumber = "";
	static String eventsNumber = "500";
	
	static String runPath = BkPaths.RUNSSVN + "run" + runNumber + "/";
	static String eventsPath = runPath + "ITERS/it." + eventsNumber + "/";

	public static void main(String[] args) {
		if(convert){ // converter-modus
			String networkFile = runPath + runNumber + ".output_network.xml.gz";
			String eventsFile =  eventsPath + runNumber + "." + eventsNumber + ".events.xml.gz";
			String outputFile = eventsPath + runNumber + "." + eventsNumber + ".events.mvi";
			String[] array = {
					"blah",
					eventsFile, 
					networkFile,
					outputFile,
					"600"};
			OTFVis.convert(array);
			logger.info("Events successfully converted to " + outputFile);
		
		} else{ // mvi-modus
//			String otffile = eventsPath + runNumber + "." + eventsNumber + ".otfvis.mvi";
			String otffile = eventsPath + runNumber + "." + eventsNumber + ".events.mvi";

		OTFVis.main(new String[] {otffile});
		}
	}	
}
