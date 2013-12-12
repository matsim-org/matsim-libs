/* *********************************************************************** *
 * project: org.matsim.*
 * RunTest.java
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
package playground.thibautd.mobsim.pseudoqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author thibautd
 */
public class RunTest {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		final String qSimEventsFile = args[ 1 ];
		final String pSimEventsFile = args[ 2 ];

		final Config config = ConfigUtils.loadConfig( configFile );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final EventsManager events = EventsUtils.createEventsManager();

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					config.travelTimeCalculator());
		events.addHandler( travelTime );

		/* scope of writer */ {
			final EventWriterXML writer = new EventWriterXML( qSimEventsFile );
			events.addHandler( writer );

			new QSimFactory().createMobsim(
						scenario,
						events).run();

			writer.closeFile();
			events.removeHandler( writer );
		}

		/* scope of writer */ {
			final EventWriterXML writer = new EventWriterXML( pSimEventsFile );
			events.addHandler( writer );

			new QSimWithPseudoEngineFactory(
						travelTime.getLinkTravelTimes()
					).createMobsim(
						scenario,
						events).run();

			writer.closeFile();
		}
	}
}

