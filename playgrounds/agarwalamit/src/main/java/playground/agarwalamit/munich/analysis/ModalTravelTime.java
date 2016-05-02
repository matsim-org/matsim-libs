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
package playground.agarwalamit.munich.analysis;

import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;

/**
 * @author amit
 */

public class ModalTravelTime {

	public static void main(String[] args) {

		String dir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runCase : runCases){
			new ModalTravelTime().run(dir+runCase+"/ITERS/it.1500/1500.events.xml.gz", dir+runCase+"/analysis/modalTravelTimes.txt");
		}
	}

	private  void run(String eventsFile, String outputFile){
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);
		mtta.run();
		mtta.writeResults(outputFile);
	}
}
