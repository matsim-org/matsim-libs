/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractTripModeShares30kmFromBellevue.java
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
package playground.thibautd.socnetsimusages.analysis;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.router.BikeSharingModeIdentifier;
import eu.eunoiaproject.bikesharing.framework.router.MainModeIdentifierForMultiModalAccessPt;
import eu.eunoiaproject.bikesharing.framework.router.TransitMultiModalAccessRoutingModule;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.socnetsim.jointtrips.JointMainModeIdentifier;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.ivt.utils.AcceptAllFilter;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.SubpopulationFilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author thibautd
 */
public class ExtractTripModeSharesAroundBellevue {
	private static final Logger log =
		Logger.getLogger(ExtractTripModeSharesAroundBellevue.class);

	private static final Coord BELLEVUE_COORD = new Coord((double) 683518, (double) 246836);
	private static final StageActivityTypes STAGES =
		new StageActivityTypesImpl(
				Arrays.asList(
					TransitMultiModalAccessRoutingModule.DEPARTURE_ACTIVITY_TYPE,
					BikeSharingConstants.INTERACTION_TYPE,
					PtConstants.TRANSIT_ACTIVITY_TYPE,
					JointActingTypes.INTERACTION ) );
	private static final MainModeIdentifier MODE_IDENTIFIER = 
		new MainModeIdentifier() {
			// Beurk...
			final MainModeIdentifier delegate =
				new MainModeIdentifierForMultiModalAccessPt(
					new BikeSharingModeIdentifier(
						new JointMainModeIdentifier(
								new MainModeIdentifierImpl() ) ) );

			@Override
			public String identifyMainMode(final List<? extends PlanElement> tripElements) {
				if ( tripElements.size() == 1 &&
						((Leg) tripElements.get( 0 )).getMode().equals( TransportMode.transit_walk ) ) {
					return TransportMode.walk;
				}
				return delegate.identifyMainMode( tripElements );
			}
		};

	// MZ: just use ht ecrow fly distance
	private static final double CROW_FLY_FACTOR = 1;
	private static final boolean USE_NET_DIST = false;

	private enum Filtering {od, homeCoord;}

	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-p" , "--plans-file" , null );
		parser.setDefaultValue( "-f" , "--facilities-file" , null );
		parser.setDefaultValue( "-n" , "--network-file" , null );
		parser.setDefaultValue( "-a" , "--attributes-file" , null );
		parser.setDefaultValue( "-s" , "--subpopulation" , null );
		parser.setDefaultValue( "-r" , "--radius_km" , "20" );
		parser.setDefaultValue( "-filter" , "--filtering-method" , Filtering.od.toString() );

		parser.setDefaultValue( "-o" , "--output-file" , null );

		main( parser.parseArgs( args ) );
	}

	private static void main(final ArgParser.Args args) throws IOException {
		final String plansFile = args.getValue( "-p" );
		final String outputFile = args.getValue( "-o" );
		// for V4 or distance computation: optional
		final String networkFile = args.getValue( "-n" );
		// useful for V4 only: optional
		final String facilitiesFile = args.getValue( "-f" );

		final String attributesFile = args.getValue( "-a" );
		final String subpopulation = args.getValue( "-s" );

		final double radius_m = args.getDoubleValue( "-r" ) * 1000;
		final Filter filter =
			getFilter(
					args.getEnumValue( "-filter" , Filtering.class ),
					radius_m );

		if ( attributesFile != null ) {
			log.info( "reading subpopulation attribute from "+attributesFile+", using subpopulation "+subpopulation );
		}
		else {
			log.info( "not filtering subpopulations." );
		}

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		if ( facilitiesFile != null ) new MatsimFacilitiesReader( scenario ).parse( facilitiesFile );
		if ( networkFile != null ) new MatsimNetworkReader(scenario.getNetwork()).parse( networkFile );

		final PopulationImpl pop = (PopulationImpl) scenario.getPopulation();

		if ( attributesFile != null ) {
			new ObjectAttributesXmlReader( pop.getPersonAttributes() ).parse( attributesFile );
		}

		final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile );
		writer.write( "agentId\tmain_mode\ttotal_dist" );

		pop.setIsStreaming( true );
		pop.addAlgorithm( new PersonAlgorithm() {
			final playground.ivt.utils.Filter<Id> personFilter = attributesFile != null ?
					new SubpopulationFilter(
						pop.getPersonAttributes(),
						subpopulation ) :
					new AcceptAllFilter<Id>();
			@Override
			public void run(final Person person) {
				if ( !personFilter.accept( person.getId() ) ) return;
				final Plan plan = person.getSelectedPlan();
				if ( !filter.acceptPlan( plan ) ) return;
				for ( Trip trip : TripStructureUtils.getTrips( plan , STAGES ) ) {
					if ( !filter.acceptTrip( trip ) ) continue;
					final String mode = MODE_IDENTIFIER.identifyMainMode( trip.getTripElements() );
					try {
						writer.newLine();
						writer.write( person.getId()+"\t"+mode+"\t"+calcDist( trip , scenario.getNetwork() ) );
					}
					catch (IOException e) {
						throw new RuntimeException( e );
					}
				}			
			}
		} );
		new MatsimPopulationReader( scenario ).parse( plansFile );

		writer.close();
	}

	private static Filter getFilter(
			final Filtering filteringType,
			final double radius_m ) {
		switch ( filteringType ) {
		case homeCoord:
			log.info( "filtering based on home coordinates, using a radius of "+radius_m+" meters" );
			return new HomeCoordFilter( radius_m );
		case od:
			log.info( "filtering based on trip ODs, using a radius of "+radius_m+" meters" );
			return new ODFilter( radius_m ) ;
		default:
			throw new IllegalArgumentException( filteringType+"?" );
		}
	}

	private static double calcDist(final Trip trip, final Network network) {
		double dist = 0;

		for ( Leg l : trip.getLegsOnly() ) {
			final Route r = l.getRoute();
			if ( USE_NET_DIST && r instanceof NetworkRoute )  {
				dist += RouteUtils.calcDistanceExcludingStartEndLink( (NetworkRoute) r , network );
			}
			else {
				// TODO: make configurable?
				dist += CROW_FLY_FACTOR *
					// TODO: use coord of activities
					CoordUtils.calcEuclideanDistance(
							network.getLinks().get( r.getStartLinkId() ).getFromNode().getCoord(),
							network.getLinks().get( r.getEndLinkId() ).getToNode().getCoord() );
			}
		}

		return dist;
	}

	private interface Filter {
		public boolean acceptPlan(final Plan plan);
		public boolean acceptTrip(final Trip trip);
	}

	private static class HomeCoordFilter implements Filter {
		private final double radius;

		public HomeCoordFilter(final double radius_m) {
			this.radius = radius_m;
		}

		@Override
		public boolean acceptPlan(final Plan plan) {
			final Activity act = getHomeActivity( plan );
			return CoordUtils.calcEuclideanDistance( act.getCoord() , BELLEVUE_COORD ) <= radius;
		}

		@Override
		public boolean acceptTrip(final Trip trip) {
			return true;
		}

		private static Activity getHomeActivity(final Plan plan) {
			final Activity activity = (Activity) plan.getPlanElements().get( 0 );
			if ( !activity.getType().equals( "home" ) ) throw new IllegalArgumentException( ""+plan.getPlanElements() );
			return activity;
		}
	}

	private static class ODFilter implements Filter {
		private final double radius;

		public ODFilter(final double radius_m) {
			this.radius = radius_m;
		}

		@Override
		public boolean acceptPlan(final Plan plan) {
			return true;
		}

		@Override
		public boolean acceptTrip(final Trip trip) {
			return CoordUtils.calcEuclideanDistance( trip.getOriginActivity().getCoord() , BELLEVUE_COORD ) <= radius &&
				 CoordUtils.calcEuclideanDistance( trip.getDestinationActivity().getCoord() , BELLEVUE_COORD ) <= radius;
		}
	}
}

