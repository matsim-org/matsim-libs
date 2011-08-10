/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTestAggregator.java
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

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.benjamin.scenarios.munich.analysis.EmissionsPerPersonColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.EmissionsPerPersonWarmEventHandler;

/**
 * @author benjamin
 *
 */
public class EmissionTestAggregator {

	private static final String emissionFile = "../../runs-svn/testEvents.xml";

	public static void main(String[] args) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		EmissionsPerPersonWarmEventHandler warmHandler = new EmissionsPerPersonWarmEventHandler();
		EmissionsPerPersonColdEventHandler coldHandler = new EmissionsPerPersonColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(emissionFile);
	}
}