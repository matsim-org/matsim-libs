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
package playground.thibautd.pseudoqsim.pseudoqsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.population.algorithms.PersonPrepareForSim;

/**
 * @author thibautd
 */
public class RunTest {
	private static final Logger log =
		Logger.getLogger(RunTest.class);

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		final String qSimEventsFile = args.length > 1 ? args[ 1 ] : null;
		final String pSimEventsFile = args.length > 2 ? args[ 2 ] : null;

		final Config config = ConfigUtils.loadConfig( configFile );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final EventsManager events = EventsUtils.createEventsManager();

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					config.travelTimeCalculator());
		events.addHandler( travelTime );

		new PersonPrepareForSim(
				new PlanRouter(
					new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).instantiateAndConfigureTripRouter(
							new RoutingContextImpl(
								new RandomizingTimeDistanceTravelDisutility(
									travelTime.getLinkTravelTimes(),
									config.planCalcScore() ),
								travelTime.getLinkTravelTimes() ) )
				),
				scenario).run( scenario.getPopulation() );


		long timeQSim, timePSim;
		/* scope of writer */ {
			final EventWriterXML writer =
				qSimEventsFile != null ?
					new EventWriterXML( qSimEventsFile ) :
					null;
			if (writer != null) events.addHandler( writer );

			log.info( "running actual simulation..." );
			timeQSim = -System.currentTimeMillis();
			QSimUtils.createDefaultQSim(scenario, events).run();
			timeQSim += System.currentTimeMillis();
			log.info( "running actual simulation... DONE" );

			if (writer != null) writer.closeFile();
			if (writer != null) events.removeHandler( writer );
		}

		/* scope of writer */ {
			final EventWriterXML writer =
				pSimEventsFile != null ?
					new EventWriterXML( pSimEventsFile ) :
					null;
			if (writer != null) events.addHandler( writer );

			log.info( "running pseudo simulation..." );
			timePSim = -System.currentTimeMillis();
			new QSimWithPseudoEngineFactory(
						travelTime.getLinkTravelTimes()
					).createMobsim(
						scenario,
						events).run();
			timePSim += System.currentTimeMillis();
			log.info( "running pseudo simulation... DONE" );

			if (writer != null) writer.closeFile();
		}

		log.info( "actual simulation took "+timeQSim+" ms." );
		log.info( "pseudo simulation took "+timePSim+" ms." );
	}
}

