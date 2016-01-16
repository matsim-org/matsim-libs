/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractHitchHikingDistancesFromEvents.java
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
package playground.thibautd.hitchiking.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class ExtractHitchHikingDistancesFromEvents {
	public static void main(final String[] args) {
		final String networkFile = args[ 0 ];
		final String eventsFile = args[ 1 ];
		final String outfile = args[ 2 ];

		Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( networkFile );
		HitchHikingDistancesEventHandler handler =
			new HitchHikingDistancesEventHandler(
					sc.getNetwork(),
					outfile);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( handler );
		new EventsReaderXMLv1( events ).parse( eventsFile );
		handler.close();
	}
}

