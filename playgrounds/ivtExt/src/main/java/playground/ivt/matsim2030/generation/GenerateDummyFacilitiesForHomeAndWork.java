/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateDummyFacilitiesForHomeAndWork.java
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
package playground.ivt.matsim2030.generation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;

/**
 * @author thibautd
 */
public class GenerateDummyFacilitiesForHomeAndWork {
	public static void main(final String[] args) {
		final String inputPopFile = args[ 0 ];
		final String outputFacilitiesFile = args[ 1 ];
		final String outputPopFile = args[ 2 ];

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming( true );
		final ActivityFacilities facilities = scenario.getActivityFacilities();

		final PopulationWriter writer = new PopulationWriter( population );
		writer.startStreaming( outputPopFile );

		final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );
		population.addAlgorithm( new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				if ( person.getPlans().size() != 1 ) throw new IllegalArgumentException( ""+person.getPlans().size() );

				for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan() , stages ) ) {
					if ( act.getType().equals( "home" ) ) {
						final ActivityFacility fac = getHomeFacility( act , facilities );
						((ActivityImpl) act).setFacilityId( fac.getId() );
					}
					else if ( act.getType().equals( "work" ) ) {
						final ActivityFacility fac = getWorkFacility( act , facilities );
						((ActivityImpl) act).setFacilityId( fac.getId() );
					}
				}

				writer.writePerson( person );
			}
		});

		new MatsimPopulationReader( scenario ).readFile( inputPopFile );

		writer.closeStreaming();
		new FacilitiesWriter( facilities ).write( outputFacilitiesFile );
	}

	private static ActivityFacility getWorkFacility(
			final Activity act,
			final ActivityFacilities facilities ) {
		return getFacility( act.getCoord() , "work" , 6 * 3600 , 22 * 3600 , facilities );
	}

	private static ActivityFacility getHomeFacility(
			final Activity act,
			final ActivityFacilities facilities ) {
		return getFacility( act.getCoord() , "home" , Time.UNDEFINED_TIME , Time.UNDEFINED_TIME , facilities );
	}

	private static ActivityFacility getFacility(
			final Coord coord,
			final String type,
			final double opening,
			final double closing,
			final ActivityFacilities facilities ) {
		final Id id = new IdImpl( type+"Facility-"+coord.getX()+"_"+coord.getY() );
		ActivityFacility facility = facilities.getFacilities().get( id );

		if ( facility == null ) {
			facility = facilities.getFactory().createActivityFacility( id , coord );
			facilities.addActivityFacility( facility );

			final ActivityOption option = facilities.getFactory().createActivityOption( type );
			if ( opening != Time.UNDEFINED_TIME && closing != Time.UNDEFINED_TIME ) {
				option.addOpeningTime( new OpeningTimeImpl( opening , closing ) );
			}

			facility.addActivityOption( option );
		}

		return facility;
	}

}

