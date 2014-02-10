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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingRoute;

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
	private final PlansCalcRouteConfigGroup config;
	private final double searchRadius;

	public BikeSharingRoutingModule(
			final Random random,
			final BikeSharingFacilities bikeSharingFacilities,
			final double searchRadius,
			final PlansCalcRouteConfigGroup config ) {
		this.random = random;
		this.bikeSharingFacilities = bikeSharingFacilities;
		this.config = config;
		this.searchRadius = searchRadius;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final BikeSharingFacility startStation = chooseCloseStation( fromFacility );
		final BikeSharingFacility endStation = chooseCloseStation( toFacility );

		final List<PlanElement> trip = new ArrayList<PlanElement>();

		trip.add( createWalkLeg( fromFacility , startStation ) );
		trip.add( createInteraction( startStation ) );
		trip.add( createBikeSharingLeg( startStation , endStation ) );
		trip.add( createInteraction( startStation ) );
		trip.add( createWalkLeg( endStation , toFacility ) );

		return trip;
	}

	private PlanElement createBikeSharingLeg(
			final BikeSharingFacility startStation,
			final BikeSharingFacility endStation) {
		final Leg leg = new LegImpl( BikeSharingConstants.MODE );
		final double dist = CoordUtils.calcDistance(startStation.getCoord(), endStation.getCoord());

		final Route route = new BikeSharingRoute( startStation , endStation );
		final double estimatedNetworkDistance = dist * config.getBeelineDistanceFactor();

		final int travTime = (int) (estimatedNetworkDistance / config.getTeleportedModeSpeeds().get( TransportMode.bike ) );
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);

		leg.setRoute(route);
		leg.setTravelTime(travTime);

		return leg;
	}

	private static PlanElement createInteraction( final Facility facility ) {
		final Activity act = new ActivityImpl( BikeSharingConstants.INTERACTION_TYPE , facility.getCoord() );
		act.setMaximumDuration( 0 );
		((ActivityImpl) act).setLinkId( facility.getLinkId() );
		return act;
	}

	private PlanElement createWalkLeg(
			final Facility fromFacility,
			final Facility toFacility) {
		final Leg leg = new LegImpl( TransportMode.walk );
		final double dist = CoordUtils.calcDistance(fromFacility.getCoord(), toFacility.getCoord());

		final Route route = new GenericRouteImpl( fromFacility.getLinkId(), toFacility.getLinkId() );
		final double estimatedNetworkDistance = dist * config.getBeelineDistanceFactor();

		final int travTime = (int) (estimatedNetworkDistance / config.getTeleportedModeSpeeds().get( TransportMode.walk ) );
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);

		leg.setRoute(route);
		leg.setTravelTime(travTime);

		return leg;
	}

	private BikeSharingFacility chooseCloseStation(final Facility facility) {
		final Collection<BikeSharingFacility> stationsInRadius =
			bikeSharingFacilities.getCurrentQuadTree().get(
					facility.getCoord().getX(),
					facility.getCoord().getY(),
					searchRadius );
		return stationsInRadius.isEmpty() ?
			bikeSharingFacilities.getCurrentQuadTree().get(
					facility.getCoord().getX(),
					facility.getCoord().getY()) :
			new ArrayList<BikeSharingFacility>( stationsInRadius ).get( random.nextInt( stationsInRadius.size() ) );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return stageTypes;
	}
}

