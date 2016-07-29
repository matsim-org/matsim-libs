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

package playground.benjamin.scenarios.manteuffel;

import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author benjamin
 *
 */
public class ManteuffelFilterEvents {

	private final String inputPath = "../../runs-svn/manteuffelstrasse/";
	
	private final String emissionEventsFile = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEvents.xml.gz";
	private final String morningFile = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsMorning.xml.gz";
	private final String eveningFile = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEventsEvening.xml.gz";

//	private final String emissionEventsFile = inputPath + "p1/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEvents.xml.gz";
//	private final String morningFile = inputPath + "p1/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsMorning.xml.gz";
//	private final String eveningFile = inputPath + "p1/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEventsEvening.xml.gz";
	
//	private final String emissionEventsFile = inputPath + "p2/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEvents.xml.gz";
//	private final String morningFile = inputPath + "p2/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsMorning.xml.gz";
//	private final String eveningFile = inputPath + "p2/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEventsEvening.xml.gz";
	
//	private final String emissionEventsFile = inputPath + "p3/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEvents.xml.gz";
//	private final String morningFile = inputPath + "p3/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsMorning.xml.gz";
//	private final String eveningFile = inputPath + "p3/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEventsEvening.xml.gz";
	
//	private final String emissionEventsFile = inputPath + "p4/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEvents.xml.gz";
//	private final String morningFile = inputPath + "p4/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsMorning.xml.gz";
//	private final String eveningFile = inputPath + "p4/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEventsEvening.xml.gz";
	
	private void run() {
		EventsManager eventsManager = EventsUtils.createEventsManager();		
		EventWriterXML mew = new EventWriterXML(morningFile);
		EventWriterXML eew = new EventWriterXML(eveningFile);
		eventsManager.addHandler(new ManteuffelEmissionEventsFilter(mew, eew));
		
		EmissionEventsReader emissionEventsReader = new EmissionEventsReader(eventsManager);
		emissionEventsReader.readFile(emissionEventsFile);
		
		mew.closeFile();
		eew.closeFile();
	}

	public static void main(String[] args) {
		new ManteuffelFilterEvents().run();
	}
}
