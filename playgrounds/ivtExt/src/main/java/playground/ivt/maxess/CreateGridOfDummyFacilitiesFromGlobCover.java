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
package playground.ivt.maxess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import playground.ivt.maxess.gisutils.GlobCoverTypeIdentifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Script to generate fake facilities around a population, based on land cover types from the ESA (http://due.esrin.esa.int/page_globcover.php)
 * @author thibautd
 */
public class CreateGridOfDummyFacilitiesFromGlobCover {
	private static final Logger log = Logger.getLogger( CreateGridOfDummyFacilitiesFromGlobCover.class );
	public static void main( final String... args ) {
		final GlobCoverFacilityCreationConfigGroup configGroup = new GlobCoverFacilityCreationConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup );

		// only needs to contain population
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final GlobCoverTypeIdentifier landUseTypeIdentifier =
				new GlobCoverTypeIdentifier(
						config.global().getCoordinateSystem(),
						configGroup.getGeoTiffFilePath() );

		final Bounds bounds = new Bounds( configGroup.getBuffer_m() );
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			for ( Activity a : TripStructureUtils.getActivities( person.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE ) ) {
				bounds.addCoord( a.getCoord() );
			}
		}

		final ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		final Counter counter = new Counter( "look at grid cell # " );
		int ignoredCells = 0;
		for ( double x = bounds.getMinX(); x <= bounds.getMaxX(); x += configGroup.getGridStepSize_m() ) {
			for ( double y = bounds.getMinY(); y <= bounds.getMaxY(); y += configGroup.getGridStepSize_m() ) {
				counter.incCounter();
				final Coord coord = new Coord( x , y );
				if ( landUseTypeIdentifier.getLandCover( coord ) != GlobCoverTypeIdentifier.LandCover.artificial ) {
					ignoredCells++;
					continue;
				}
				final ActivityFacility f =
						facilities.getFactory().createActivityFacility(
								Id.create(
										"f-"+x+"-"+y,
										ActivityFacility.class ),
								coord );

				for ( String type : configGroup.getActivityTypes() ) {
					final ActivityOption activityOption = facilities.getFactory().createActivityOption( type );
					f.addActivityOption( activityOption );
				}
				facilities.addActivityFacility( f );
			}
		}
		counter.printCounter();
		log.info( "ignored "+ignoredCells+" cells out of "+counter.getCounter() );

		new FacilitiesWriter( facilities ).write( configGroup.getOutputFacilities() );
	}

	private static class Bounds {
		private final double buffer;

		private double minX = Double.POSITIVE_INFINITY;
		private double minY = Double.POSITIVE_INFINITY;
		private double maxX = Double.NEGATIVE_INFINITY;
		private double maxY = Double.NEGATIVE_INFINITY;

		private Bounds( double buffer ) {
			if ( buffer < 0 ) log.warn( "negative buffer: facilities will not reach the envelope of the population!" );
			this.buffer = buffer;
		}

		public void addCoord( final Coord c ) {
			minX = Math.min( minX , c.getX() );
			minY = Math.min( minY , c.getY() );

			maxX = Math.max( maxX , c.getX() );
			maxY = Math.max( maxY , c.getY() );

			assert minX <= maxX;
			assert minY <= maxY;
		}

		public double getMaxX() {
			return maxX + buffer;
		}

		public double getMaxY() {
			return maxY + buffer;
		}

		public double getMinX() {
			return minX - buffer;
		}

		public double getMinY() {
			return minY - buffer;
		}
	}

	public static class GlobCoverFacilityCreationConfigGroup extends ReflectiveConfigGroup {
		public static final String GROUP_NAME = "globcoverFacilityCreation";

		private double buffer_m = 50000;
		private String geoTiffFilePath = null;
		private String outputFacilities = null;
		private double gridStepSize_m = 500;

		// south african types by default
		private Set<String> activityTypes = CollectionUtils.stringToSet( "h,s,v,o,w,e1,e2,m,e3,l" );

		public GlobCoverFacilityCreationConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "buffer_m" )
		public double getBuffer_m() {
			return buffer_m;
		}

		@StringSetter( "buffer_m" )
		public void setBuffer_m( double buffer_m ) {
			this.buffer_m = buffer_m;
		}

		@StringGetter( "geoTiffFilePath" )
		public String getGeoTiffFilePath() {
			return geoTiffFilePath;
		}

		@StringSetter( "geoTiffFilePath" )
		public void setGeoTiffFilePath( String geoTiffFilePath ) {
			this.geoTiffFilePath = geoTiffFilePath;
		}

		@StringGetter( "outputFacilities" )
		public String getOutputFacilities() {
			return outputFacilities;
		}

		@StringSetter( "outputFacilities" )
		public void setOutputFacilities( String outputFacilities ) {
			this.outputFacilities = outputFacilities;
		}

		@StringGetter( "gridStepSize_m" )
		public double getGridStepSize_m() {
			return gridStepSize_m;
		}

		@StringSetter( "gridStepSize_m" )
		public void setGridStepSize_m( double gridStepSize_m ) {
			this.gridStepSize_m = gridStepSize_m;
		}

		@StringGetter( "activityTypes" )
		private String getActivityTypesString() {
			return CollectionUtils.setToString( getActivityTypes() );
		}

		@StringSetter( "activityTypes" )
		private void setActivityTypesString( String activityTypes ) {
			setActivityTypes( CollectionUtils.stringToSet( activityTypes ) );
		}

		public Set<String> getActivityTypes() {
			return activityTypes;
		}

		public void setActivityTypes( Set<String> activityTypes ) {
			this.activityTypes = activityTypes;
		}
	}
}
