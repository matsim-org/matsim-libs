/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomPlansFromFacilities.java
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
package playground.thibautd.scripts.scenariohandling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.utils.ArgParser;

/**
 * @author thibautd
 */
public class GenerateRandomPlansFromFacilities {
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser( args );

		parser.setDefaultValue( "--inputfacilities" , null );
		parser.setDefaultValue( "--outputplans" , null );

		parser.setDefaultValue( "--planssize" , "4" );
		parser.setDefaultValue( "--popsize" , "1000" );
		parser.setDefaultValue( "--hometype" , "home" );

		final String inputFacilities = parser.getValue( "--inputfacilities" );
		final String outputPlans = parser.getValue( "--outputplans" );

		final int plansSize = Integer.parseInt( parser.getValue( "--planssize" ) );
		final int popSize = Integer.parseInt( parser.getValue( "--popsize" ) );
		final String homeType = parser.getValue( "--hometype" );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimFacilitiesReader( sc ).readFile( inputFacilities );

		final List<ActivityFacility> homeLocations = new ArrayList<ActivityFacility>();
		final List<ActivityFacility> nonHomeLocations = new ArrayList<ActivityFacility>();

		for ( ActivityFacility f : sc.getActivityFacilities().getFacilities().values() ) {
			if ( f.getActivityOptions().containsKey( homeType ) ) homeLocations.add( f );
			if ( containsOtherKey( f.getActivityOptions() , homeType ) ) nonHomeLocations.add( f );
		}

		final Random random = new Random( 2345 );
		for ( int i=0; i < popSize; i++ ) {
			final Person person = sc.getPopulation().getFactory().createPerson( new IdImpl( "person-"+i ) );
			sc.getPopulation().addPerson( person );

			final Plan plan = sc.getPopulation().getFactory().createPlan();
			person.addPlan( plan );

			final ActivityFacility home = homeLocations.get( random.nextInt( homeLocations.size() ) );
			plan.addActivity(
					createActivity(
						sc.getPopulation().getFactory(),
						home,
						homeType,
						random.nextDouble() * 12d * 3600d ) );

			for ( int n = 0; n < plansSize; n++ ) {
				final ActivityFacility fac = nonHomeLocations.get( random.nextInt( nonHomeLocations.size() ) );
				final String type = getRandomOtherType( random , fac , homeType );

				plan.addLeg( sc.getPopulation().getFactory().createLeg( "car" ) );
				plan.addActivity(
						createActivity(
							sc.getPopulation().getFactory(),
							fac,
							type,
							8 + random.nextDouble() * 12d * 3600d ) );
			}

			plan.addLeg( sc.getPopulation().getFactory().createLeg( "car" ) );
			plan.addActivity(
					createActivity(
						sc.getPopulation().getFactory(),
						home,
						homeType,
						Time.UNDEFINED_TIME ) );

		}

		// metadata
		final StringBuilder meta = new StringBuilder();
		meta.append( "generated with " );
		meta.append( GenerateRandomPlansFromFacilities.class.getName() );
		for ( String arg : args ) meta.append( " "+arg );
		meta.append( " on the " );
		meta.append( new Date().toString() );

		sc.getPopulation().setName( meta.toString() );

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPlans );
	}

	private static String getRandomOtherType(
			final Random random,
			final ActivityFacility fac,
			final String homeType) {
		final List<String> types = new ArrayList<String>( fac.getActivityOptions().keySet() );
		types.remove( homeType );
		return types.get( random.nextInt( types.size() ) );
	}

	private static Activity createActivity(
			final PopulationFactory factory,
			final ActivityFacility facility,
			final String type,
			final double endTime) {
		final Activity act = factory.createActivityFromCoord( type , facility.getCoord() );
		((ActivityImpl) act).setLinkId( facility.getLinkId() );
		((ActivityImpl) act).setFacilityId( facility.getId() );
		((ActivityImpl) act).setEndTime( endTime );
		return act;
	}

	private static boolean containsOtherKey(
			final Map<String, ActivityOption> activityOptions,
			final String homeType) {
		for ( String k : activityOptions.keySet() ) {
			if ( ! homeType.equals( k ) ) return true;
		}
		return false;
	}
}

