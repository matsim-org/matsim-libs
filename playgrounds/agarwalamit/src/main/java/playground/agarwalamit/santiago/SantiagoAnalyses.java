/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.santiago;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;

/**
 * @author amit
 */

public class SantiagoAnalyses {

	private static final String RUN_DIR = "../../../../repos/runs-svn/santiago/output/";
	private static final String [] RUN_CASES = {"ctd","outerCordon","triangleCordon"}; 

	public static void main(String[] args) {
		SantiagoAnalyses sa = new SantiagoAnalyses();
		sa.writeModalShare();
	}

	public void writeModalShare(){
		for(String rc :RUN_CASES) {
			String eventsFile = RUN_DIR+rc+"/output_events.xml.gz";
			String outputFile = RUN_DIR+"/analysis/modalSplit_"+rc+".txt";
			ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
			msc.run();
			msc.writeResults(outputFile);
		}
	}
}