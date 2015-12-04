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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Parses the siouxfalls population, and makes all chains H-L-H, without effecting locations.
 * Then creates a Facilities file that allows those chains
 * @author thibautd
 */
public class TransformSiouxfallsPopulation {
	private static final OpeningTime HOME_OPENTIME = new OpeningTimeImpl( 0 , 30 * 3600 );
	private static final OpeningTime[] LEISURE_OPENTIMES = new OpeningTime[]{
			new OpeningTimeImpl( 8 * 3600 , 10 * 3600 ),
			new OpeningTimeImpl( 8 * 3600 , 16 * 3600 ),
			new OpeningTimeImpl( 10 * 3600 , 17 * 3600 ),
			new OpeningTimeImpl( 7 * 3600 , 18 * 3600 ),
			new OpeningTimeImpl( 12 * 3600 , 20 * 3600 ),
			new OpeningTimeImpl( 20 * 3600 , 24 * 3600 )
	};

	public static void main( final String[] args ) {
		final String inputPopulation = args[ 0 ];
		final String outputPopulation = args[ 1 ];
		final String outputFacilities = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).readFile( inputPopulation );

		final Map<Id<ActivityFacility>, Coord> homes = new LinkedHashMap<>();
		final Map<Id<ActivityFacility>, Coord> leisures = new LinkedHashMap<>();

		for (Person p : sc.getPopulation().getPersons().values() ) {
			final Plan plan = p.getSelectedPlan();
			((ActivityImpl) plan.getPlanElements().get( 2 )).setType("secondary");

			homes.put(
					((Activity) plan.getPlanElements().get(0)).getFacilityId(),
					((Activity) plan.getPlanElements().get(0)).getCoord() );
			leisures.put(
					((Activity) plan.getPlanElements().get(2)).getFacilityId(),
					((Activity) plan.getPlanElements().get(2)).getCoord() );
		}

		for ( Map.Entry<Id<ActivityFacility>, Coord> e : homes.entrySet() ) {
			final ActivityFacilitiesFactory factory = sc.getActivityFacilities().getFactory();
			final ActivityFacility f =
					factory.createActivityFacility(
							e.getKey(),
							e.getValue());
			sc.getActivityFacilities().addActivityFacility( f );

			final ActivityOption option = factory.createActivityOption("home");
			option.addOpeningTime(HOME_OPENTIME);
			f.addActivityOption(option);
		}

		final Random r = new Random( 345 );
		for ( Map.Entry<Id<ActivityFacility>, Coord> e : leisures.entrySet() ) {
			final ActivityFacilitiesFactory factory = sc.getActivityFacilities().getFactory();
			ActivityFacility f = sc.getActivityFacilities().getFacilities().get( e.getKey() );

			if ( f == null ) {
				f = factory.createActivityFacility(
						e.getKey(),
						e.getValue());
				sc.getActivityFacilities().addActivityFacility(f);
			}

			final ActivityOption option = factory.createActivityOption("secondary");
			option.addOpeningTime(LEISURE_OPENTIMES[r.nextInt(LEISURE_OPENTIMES.length)]);
			f.addActivityOption(option);
		}

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPopulation );
		new FacilitiesWriter( sc.getActivityFacilities() ).write( outputFacilities );
	}
}
