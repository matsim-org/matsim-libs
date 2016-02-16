/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingRoutingModule.java
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
package eu.eunoiaproject.bikesharing.framework.router;

import com.google.inject.Inject;
import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingRoute;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * a {@link RoutingModule} for bike sharing trips.
 * Bike sharing trips are composed of an access walk
 * a bike part, and an egress walk.
 * The choice of the station is randomized, so that during the process,
 * agents learn the best station (the best ration bike availability/travel distance)
 * by themselves.
 *
 * @author thibautd
 */
public class BikeSharingRoutingModule implements RoutingModule {
	private final StageActivityTypes stageTypes = new StageActivityTypesImpl( BikeSharingConstants.INTERACTION_TYPE );


	private final Random random;
	private final BikeSharingFacilities bikeSharingFacilities;
	private final double searchRadius;

	private final RoutingModule walkRouting;
	private final RoutingModule bikeRouting;

	@Inject
	public BikeSharingRoutingModule(
			final Scenario scenario,
			@Named( TransportMode.walk )
			final RoutingModule walkRouting,
			@Named( TransportMode.bike )
			final RoutingModule bikeRouting) {
		this(MatsimRandom.getLocalInstance(),
				(BikeSharingFacilities) scenario.getScenarioElement( BikeSharingFacilities.ELEMENT_NAME ),
				((BikeSharingConfigGroup) scenario.getConfig().getModule( BikeSharingConfigGroup.GROUP_NAME )).getSearchRadius(),
				walkRouting,
				bikeRouting );
	}

	public BikeSharingRoutingModule(
			final Random random,
			final BikeSharingFacilities bikeSharingFacilities,
			final double searchRadius,
			final RoutingModule walkRouting,
			final RoutingModule bikeRouting) {
		this.random = random;
		this.bikeSharingFacilities = bikeSharingFacilities;
		this.searchRadius = searchRadius;
		this.walkRouting = walkRouting;
		this.bikeRouting = bikeRouting;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final double directDistance = CoordUtils.calcEuclideanDistance( fromFacility.getCoord() , toFacility.getCoord() );

		final double maxSearchRadius = directDistance / 3d;
		final BikeSharingFacility startStation = chooseCloseStation( fromFacility , maxSearchRadius );
		final BikeSharingFacility endStation = chooseCloseStation( toFacility , maxSearchRadius );

		if ( startStation == endStation ) {
			final List<PlanElement> trip = new ArrayList< >( 2 );
			// "tag" trip as bike sharing.
			trip.add( createInteraction( fromFacility ) );
			trip.addAll(
					walkRouting.calcRoute(
							fromFacility,
							toFacility,
							departureTime,
							person));
			return trip;
		}

		final List<PlanElement> trip = new ArrayList< >( 5 );

		trip.addAll(
				createWalkSubtrip(
					fromFacility,
					startStation,
					departureTime,
					person ) );
		trip.add( createInteraction( startStation ) );
		trip.addAll(
				createBikeSharingSubtrip(
					startStation,
					endStation,
					departureTime,
					person ) );
		trip.add( createInteraction( endStation ) );
		trip.addAll(
				createWalkSubtrip(
					endStation,
					toFacility,
					departureTime,
					person ) );

		return trip;
	}

	private List<? extends PlanElement> createBikeSharingSubtrip(
			final BikeSharingFacility startStation,
			final BikeSharingFacility endStation,
			final double departureTime,
			final Person person) {
		final List<? extends PlanElement> trip =
			bikeRouting.calcRoute(
					startStation,
					endStation,
					departureTime,
					person );
		
		if ( trip.size() != 1 ) throw new RuntimeException( "unable to handle complex bike trip "+trip );

		final Leg leg = (Leg) trip.get( 0 );
		if ( !leg.getMode().equals( TransportMode.bike ) ) throw new RuntimeException( "unexpected mode for "+leg );

		leg.setMode( BikeSharingConstants.MODE );
		leg.setRoute( convertToBikeSharingRoute( leg.getRoute() , startStation , endStation ) );

		return trip;
	}

	private static BikeSharingRoute convertToBikeSharingRoute(
			final Route route,
			final BikeSharingFacility startStation,
			final BikeSharingFacility endStation) {
		final BikeSharingRoute bsRoute =
			route instanceof NetworkRoute ?
				new BikeSharingRoute(
						(NetworkRoute) route,
						startStation.getId(),
						endStation.getId() ) :
				new BikeSharingRoute(
						startStation,
						endStation );

		bsRoute.setDistance( route.getDistance() );
		bsRoute.setTravelTime( route.getTravelTime() );

		return bsRoute;
	}

	private static PlanElement createInteraction( final Facility facility ) {
		final Activity act = new ActivityImpl( BikeSharingConstants.INTERACTION_TYPE , facility.getCoord() );
		act.setMaximumDuration( 0 );
		((ActivityImpl) act).setLinkId( facility.getLinkId() );
		// XXX This may cause problems if IDs of ActivityFacilities and BikeSharingFacilities overlap...
		((ActivityImpl) act).setFacilityId( facility.getId() );
		return act;
	}

	private List<? extends PlanElement> createWalkSubtrip(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final List<? extends PlanElement> trip =
			walkRouting.calcRoute(
					fromFacility,
					toFacility,
					departureTime,
					person );
		return trip;
	}

	private BikeSharingFacility chooseCloseStation(
			final Facility facility,
			final double maxSearchRadius ) {
		final Collection<BikeSharingFacility> stationsInRadius =
			bikeSharingFacilities.getCurrentQuadTree().getDisk(
					facility.getCoord().getX(),
					facility.getCoord().getY(),
					Math.min(
							searchRadius,
							maxSearchRadius));
		return stationsInRadius.isEmpty() ?
			bikeSharingFacilities.getCurrentQuadTree().getClosest(
					facility.getCoord().getX(),
					facility.getCoord().getY()) :
			new ArrayList<BikeSharingFacility>( stationsInRadius ).get( random.nextInt( stationsInRadius.size() ) );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return stageTypes;
	}
}

