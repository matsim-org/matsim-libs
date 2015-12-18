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
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.ivt.utils.MoreIOUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * @author thibautd
 */
public class TransformCoordinatesOfScenario {
	private static final Logger log = Logger.getLogger( TransformCoordinatesOfScenario.class );
	
	public static void main( final String... args ) {
		final TransformationConfigGroup configGroup = new TransformationConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup );

		final String outputDirectory = configGroup.getOutputDirectory();
		MoreIOUtils.initOut( outputDirectory );

		try {
			final Scenario scenario = ScenarioUtils.loadScenario( config );

			final CoordinateTransformation transformation =
					TransformationFactory.getCoordinateTransformation(
							configGroup.getFromCrs(),
							configGroup.getToCrs() );

			if ( config.network().getInputFile() != null ) {
				log.info( "transform network");
				new NetworkTransform( transformation ).run( scenario.getNetwork() );
				new NetworkWriter( scenario.getNetwork() ).write( outputDirectory+"/network_"+configGroup.toCrs+".xml.gz" );
			}

			if ( config.plans().getInputFile() != null ) {
				log.info( "transform population");
				transform( transformation, scenario.getPopulation() );
				new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( outputDirectory+"/population_"+configGroup.toCrs+".xml.gz" );
			}

			if ( config.facilities().getInputFile() != null ) {
				log.info( "transform facilities");
				transform( transformation, scenario.getActivityFacilities() );
				new FacilitiesWriter( scenario.getActivityFacilities() ).write( outputDirectory+"/facilities_"+configGroup.toCrs+".xml.gz" );
			}

			if ( config.transit().getTransitScheduleFile() != null ) {
				log.info( "transform schedule");
				transform( transformation , scenario.getTransitSchedule() );
				new TransitScheduleWriter( scenario.getTransitSchedule() ).writeFile( outputDirectory+"/schedule_"+configGroup.toCrs+".xml.gz" );
			}

			new ConfigWriter( config ).write( outputDirectory+"/output_config.xml.gz" );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	private static void transform(
			final CoordinateTransformation transformation,
			final TransitSchedule transitSchedule ) {
		for ( TransitStopFacility stop : new ArrayList<>( transitSchedule.getFacilities().values() ) ) {
			final TransitStopFacility newStop =
					transitSchedule.getFactory().createTransitStopFacility(
							stop.getId(),
							transformation.transform(
									stop.getCoord() ),
							stop.getIsBlockingLane() );
			newStop.setLinkId( stop.getLinkId() );
			newStop.setName( stop.getName() );
			newStop.setStopPostAreaId( stop.getStopPostAreaId() );

			transitSchedule.removeStopFacility( stop );
			transitSchedule.addStopFacility( newStop );
		}
	}

	private static void transform(
			final CoordinateTransformation transformation,
			final Population population ) {
		for ( Person person : population.getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				for ( Activity activity : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ) ) {
					(( ActivityImpl) activity).setCoord( transformation.transform( activity.getCoord() ) );
				}
			}
		}
	}

	private static void transform(
			final CoordinateTransformation transformation,
			final ActivityFacilities facilities ) {
		for ( ActivityFacility f : facilities.getFacilities().values()) {
			((ActivityFacilityImpl) f).setCoord( transformation.transform( f.getCoord() ) );
		}
	}

	private static class TransformationConfigGroup extends ReflectiveConfigGroup {
		private static final String GROUP_NAME = "transformation";

		private String fromCrs = TransformationFactory.CH1903_LV03_Plus_GT;
		private String toCrs = TransformationFactory.CH1903_LV03_GT;

		private String outputDirectory = null;

		public TransformationConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "fromCrs" )
		public String getFromCrs() {
			return fromCrs;
		}

		@StringSetter( "fromCrs" )
		public void setFromCrs( String fromCrs ) {
			this.fromCrs = fromCrs;
		}

		@StringGetter( "toCrs" )
		public String getToCrs() {
			return toCrs;
		}

		@StringSetter( "toCrs" )
		public void setToCrs( String toCrs ) {
			this.toCrs = toCrs;
		}

		@StringGetter( "outputDirectory" )
		public String getOutputDirectory() {
			if ( outputDirectory == null ) {
				final Calendar calendar = Calendar.getInstance();
				final String dateStamp = calendar.get( Calendar.YEAR )+""+(calendar.get( Calendar.MONTH )+1)+""+calendar.get( Calendar.DAY_OF_MONTH )+"-";
				this.outputDirectory = "output/"+dateStamp+"transformedScenario_"+getFromCrs()+"_to_"+getToCrs();
			}
			return outputDirectory;
		}

		@StringSetter( "outputDirectory" )
		public void setOutputDirectory( String outputDirectory ) {
			this.outputDirectory = outputDirectory;
		}
	}
}
