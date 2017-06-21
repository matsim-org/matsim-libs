/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.sccer.stem;

import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.LambdaCounter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;


/**
 * @author thibautd
 */
public class SummarizePlans {
	private static final Logger log = Logger.getLogger( SummarizePlans.class );

	public static void main( final String... args ) {
		// should be the experienced plans
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "--config" , "-c" , null );
		parser.setDefaultValue( "--plans" , "-p" , null );
		parser.setDefaultValue( "--network" , "-n" , null );
		parser.setDefaultValue( "--output" , "-o" , "plans_summary.dat" );
		final ArgParser.Args parsed = parser.parseArgs( args );

		final String inConfig = parsed.getValue( "--config" );
		final String inPlans = parsed.getValue( "--plans" );
		final String inNetwork = parsed.getValue( "--network" );
		final String outDat = parsed.getValue( "--output" );

		final Config config =
				inConfig == null ?
						ConfigUtils.createConfig() :
						ConfigUtils.loadConfig( inConfig );
		if ( inPlans != null ) config.plans().setInputFile( inPlans );
		if ( inNetwork != null ) config.network().setInputFile( inNetwork );

		run( config , outDat );
	}

	public static void run( final Config config , final String outDat ) {

		config.global().setNumberOfThreads( 2 );

		// we want to use the streaming reader instead.
		final URL populationFile = config.plans().getInputFileURL( config.getContext() );
		config.plans().setInputFile( null );
		config.plans().setInputPersonAttributeFile( null );
		config.households().setInputFile( null );
		config.households().setInputHouseholdAttributesFile( null );

		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final Injector injector =
				org.matsim.core.controler.Injector.createInjector(
						config,
						new EventsManagerModule(),
						new TravelTimeCalculatorModule(),
						new TravelDisutilityModule(),
						new TripRouterModule(),
						new ScenarioByInstanceModule( scenario ) );
		final PlanRouter router = new PlanRouter( injector.getInstance( TripRouter.class ) );
		final PersonPrepareForSim prepare = new PersonPrepareForSim( router , scenario );

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outDat ) ) {
			writer.write( "distance_m\tstop_s" );

			final StreamingPopulationReader reader = new StreamingPopulationReader( scenario );
			// for debugging of streaming
			final LambdaCounter counter = new LambdaCounter( c -> log.info( "process person # "+ c +" from population of size "+scenario.getPopulation().getPersons().size() ) );
			reader.addAlgorithm( person -> {
				//counter.incCounter();
				prepare.run( person );
				final Plan plan = person.getSelectedPlan();
				setStartTimes( plan );
				final double traveledDistance = getCarTraveledDistance( plan );
				final double longestCarStop = getLongestCarStop( plan );

				try {
					writer.newLine();
					writer.write( traveledDistance + "\t" + longestCarStop );
				}
				catch ( IOException e ) {
					throw new UncheckedIOException( e );
				}
			} );

			reader.parse( populationFile );
			//counter.printCounter();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static void setStartTimes( final Plan plan ) {
		double now = 0;

		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Activity ) {
				final Activity act = (Activity) pe;
				act.setStartTime( now );
				if ( act.getEndTime() != Time.UNDEFINED_TIME ) now = act.getEndTime();
				else now += act.getMaximumDuration();
			}
			else {
				final Leg leg = (Leg) pe;
				now += leg.getTravelTime();
			}
		}
	}

	private static double getLongestCarStop( final Plan plan ) {
		boolean wasCar = false;
		double maxStop = 0;

		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( wasCar && ((Activity) pe).getType().startsWith( "work" ) ) {
				final double duration = ((Activity) pe).getEndTime() - ((Activity) pe).getStartTime();
				maxStop = Math.max( maxStop , duration );
			}
			wasCar = pe instanceof Leg && ((Leg) pe).getMode().equals( "car" );
		}

		return maxStop;
	}

	private static double getCarTraveledDistance( final Plan plan ) {
		return carLegsStream( plan )
				.mapToDouble( l -> l.getRoute().getDistance() )
				.sum();
	}

	private static Stream<Leg> carLegsStream( final Plan plan ) {
		return plan.getPlanElements().stream()
				.filter( pe -> pe instanceof Leg )
				.map( pe -> (Leg) pe )
				.filter( l -> l.getMode().equals( "car" ) );
	}
}
