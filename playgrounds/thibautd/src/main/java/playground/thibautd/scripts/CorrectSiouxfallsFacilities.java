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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTimeImpl;

/**
 * @author thibautd
 */
public class CorrectSiouxfallsFacilities {
	private static final Logger log = Logger.getLogger( CorrectSiouxfallsFacilities.class );

	private static ActivityOption getActivityOption( final String type ) {
		final ActivityOption option = new ActivityOptionImpl( type );
		switch ( type ) {
			case "home":
				option.addOpeningTime( new OpeningTimeImpl( 0 , 30 * 3600 ) );
				break;
			case "secondary":
				option.addOpeningTime( new OpeningTimeImpl( 8 * 3600 , 20 * 3600 ) );
				break;
			case "work":
				option.addOpeningTime( new OpeningTimeImpl( 8 * 3600 , 18 * 3600 ) );
				break;
			default:
				throw new RuntimeException( type );
		}
		return option;
	}

	public static void main( final String[] args ) {
		final String inputPopulation = args[ 0 ];
		final String inputFacilities = args[ 1 ];
		final String outputFacilities = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader( sc ).readFile(inputPopulation);
		new MatsimFacilitiesReader( sc ).readFile(inputFacilities);

		for (Person p : sc.getPopulation().getPersons().values() ) {
			final Plan plan = p.getSelectedPlan();

			for ( Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ) ) {
				final ActivityFacility facility = sc.getActivityFacilities().getFacilities().get( act.getFacilityId() );
				if ( !facility.getActivityOptions().keySet().contains( act.getType() ) ) {
					log.warn( "add type "+act.getType()+" for facility "+facility.getId() );
					facility.addActivityOption( getActivityOption( act.getType() ) );
				}
			}
		}

		new FacilitiesWriter( sc.getActivityFacilities() ).write(outputFacilities);
	}
}
