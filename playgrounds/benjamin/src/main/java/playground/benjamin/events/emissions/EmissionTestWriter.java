/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTestWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.events.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author benjamin
 *
 */
public class EmissionTestWriter {
	private static final Logger logger = Logger.getLogger(EmissionTestWriter.class);

	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
				
		Id linkId = new IdImpl("17");
		Id vehicleId = new IdImpl("38");
		
		Map<WarmPollutant, Double> warmEmissions = new HashMap<WarmPollutant, Double>();
		warmEmissions.put(WarmPollutant.CO2_TOTAL, 1000.0);
		warmEmissions.put(WarmPollutant.FC, 2000.0);
		warmEmissions.put(WarmPollutant.NO2, 3000.0);
		warmEmissions.put(WarmPollutant.NOX, 4000.0);
		warmEmissions.put(WarmPollutant.PM, 5000.0);
		Event warmEvent1 = new WarmEmissionEventImpl(77, linkId, vehicleId, warmEmissions);
		Event warmEvent2 = new WarmEmissionEventImpl(144, linkId, vehicleId, warmEmissions);
		
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();
		coldEmissions.put(ColdPollutant.CO, 100.0);
		coldEmissions.put(ColdPollutant.FC, 200.0);
		coldEmissions.put(ColdPollutant.HC, 300.0);
		coldEmissions.put(ColdPollutant.NO2, 400.0);
		coldEmissions.put(ColdPollutant.NOX, 500.0);
		coldEmissions.put(ColdPollutant.PM, 600.0);
		Event coldEvent1 = new ColdEmissionEventImpl(55, linkId, vehicleId, coldEmissions);
		Event coldEvent2 = new ColdEmissionEventImpl(110, linkId, vehicleId, coldEmissions);
		
		String outputfile = "../../runs-svn/testEvents.xml";
		EventWriterXML eWriter = new EventWriterXML(outputfile);
		eventsManager.addHandler(eWriter);
		eventsManager.processEvent(warmEvent1);
		eventsManager.processEvent(warmEvent2);
		eventsManager.processEvent(coldEvent1);
		eventsManager.processEvent(coldEvent2);
		eWriter.closeFile();
		logger.info("Finished writing output to " + outputfile);
	}
}