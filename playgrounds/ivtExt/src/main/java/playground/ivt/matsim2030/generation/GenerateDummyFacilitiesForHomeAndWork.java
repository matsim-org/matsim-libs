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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.*;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class GenerateDummyFacilitiesForHomeAndWork {
	private static final Logger log =
		Logger.getLogger(GenerateDummyFacilitiesForHomeAndWork.class);

	public static void main(final String[] args) throws Exception {
		final String inputPopFile = args[ 0 ];
		final String outputFacilitiesFile = args[ 1 ];
		final String outputPopFile = args[ 2 ];
		final String outputF2lFile = args[ 3 ];

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

		log.info( "writing facilities" );
		new FacilitiesWriter( facilities ).write( outputFacilitiesFile );

		log.info( "writing facilities 2 links" );
		writeF2l( facilities , outputF2lFile );
	}

	private static void writeF2l(
			final ActivityFacilities facilities,
			final String outputF2lFile) throws IOException {
		final BufferedWriter writer = IOUtils.getBufferedWriter( outputF2lFile );

		final Counter counter = new Counter( "[f2l] association # " );
		writer.write( "fid\tlid" );
		for ( ActivityFacility f : facilities.getFacilities().values() ) {
			counter.incCounter();
			writer.newLine();
			writer.write( f.getId() +"\t"+ f.getLinkId() );
		}
		counter.printCounter();

		writer.close();
	}

	private static ActivityFacility getWorkFacility(
			final Activity act,
			final ActivityFacilities facilities ) {
		return getFacility( act.getCoord() , act.getLinkId() , "work" , 6 * 3600 , 22 * 3600 , facilities );
	}

	private static ActivityFacility getHomeFacility(
			final Activity act,
			final ActivityFacilities facilities ) {
		return getFacility( act.getCoord() , act.getLinkId() , "home" , Time.UNDEFINED_TIME , Time.UNDEFINED_TIME , facilities );
	}

	private static ActivityFacility getFacility(
			final Coord coord,
			final Id<Link> linkId,
			final String type,
			final double opening,
			final double closing,
			final ActivityFacilities facilities ) {
		final Id<ActivityFacility> id = Id.create( type+"Facility-"+coord.getX()+"_"+coord.getY() , ActivityFacility.class);
		ActivityFacility facility = facilities.getFacilities().get( id );

		if ( facility == null ) {
			facility = facilities.getFactory().createActivityFacility( id , coord, linkId );
			facilities.addActivityFacility( facility );

			final ActivityOption option = facilities.getFactory().createActivityOption( type );
			if ( opening != Time.UNDEFINED_TIME && closing != Time.UNDEFINED_TIME ) {
				option.addOpeningTime( new OpeningTimeImpl( opening , closing ) );
			}

			facility.addActivityOption( option );
		}

		if ( !facility.getLinkId().equals( linkId ) ) {
			log.warn( "Activities linked to facility "+id+" have different links" );
		}

		return facility;
	}

}

