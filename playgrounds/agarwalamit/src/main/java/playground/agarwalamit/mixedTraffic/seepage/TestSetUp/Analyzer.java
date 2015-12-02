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
package playground.agarwalamit.mixedTraffic.seepage.TestSetUp;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author amit
 */
public class Analyzer {
	public static final Logger log = Logger.getLogger(Analyzer.class);

	public static void main(String[] args) {
		String outputDir =  "/Users/amit/Documents/repos/shared-svn/projects/mixedTraffic/seepage/xt_1Link/seepage/";
		String eventsFile = outputDir+"ITERS/it.0/0.events.xml.gz";
		
		Analyzer ana = new Analyzer();
		ana.analyzeFlow(eventsFile);
	}

	private void analyzeFlow(String eventsFile){
		EventsManager events = EventsUtils.createEventsManager();
		AverageLinkFlowHandler linkFlow = new AverageLinkFlowHandler();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(linkFlow);
		reader.readFile(eventsFile);
		
		log.info("Inflow : - "+linkFlow.getInflow().toString());
		log.info("Outflow : - "+linkFlow.getOutflow().toString());
		
	}
}
