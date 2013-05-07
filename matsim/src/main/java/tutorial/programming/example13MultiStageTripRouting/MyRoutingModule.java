/* *********************************************************************** *
 * project: org.matsim.*
 * MyRoutingModule.java
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
package tutorial.programming.example13MultiStageTripRouting;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * {@link RoutingModule} for a mode consisting in going to a teleportation
 * station by public transport, being instantly teleported to a second station
 * and going to the final destination by public transport.
 * @author thibautd
 */
public class MyRoutingModule implements RoutingModule {
	public static final String STAGE = "teleportationInteraction";
	public static final String TELEPORTATION_LEG_MODE = "teleportationLeg";

	private final RoutingModule ptDelegate;
	private final PopulationFactory populationFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final Facility station1, station2;

	/**
	 * Creates a new instance.
	 * @param ptDelegate the routing module to use to compute the PT subtrips
	 * @param populationFactory used to create legs, activities and routes
	 * @param station1 {@link Facility} representing the first teleport station
	 * @param station2 {@link Facility} representing the second teleport station
	 */
	public MyRoutingModule(
			final RoutingModule ptDelegate,
			final PopulationFactory populationFactory,
			final Facility station1,
			final Facility station2) {
		this.ptDelegate = ptDelegate;
		this.populationFactory = populationFactory;
		this.modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		this.station1 = station1;
		this.station2 = station2;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		// choose the direction which minimizes pt distance.
		final boolean from1to2 =
			CoordUtils.calcDistance( fromFacility.getCoord() , station1.getCoord() )
			+ CoordUtils.calcDistance( station2.getCoord() , toFacility.getCoord() ) <
			CoordUtils.calcDistance( fromFacility.getCoord() , station2.getCoord() )
			+ CoordUtils.calcDistance( station1.getCoord() , toFacility.getCoord() );

		final Facility originStation = from1to2 ? station1 : station2;
		final Facility destinationStation = from1to2 ? station2 : station1;

		// route the access trip
		trip.addAll(
				ptDelegate.calcRoute(
					fromFacility,
					originStation,
					departureTime,
					person ) );

		// create a dummy activity at the teleportation origin
		final Activity firstInteraction =
			populationFactory.createActivityFromLinkId(
					STAGE,
					originStation.getLinkId());
		firstInteraction.setMaximumDuration( 0 );
		trip.add( firstInteraction );

		// create the teleportation leg
		final Leg teleportationLeg =
			populationFactory.createLeg( TELEPORTATION_LEG_MODE );
		teleportationLeg.setTravelTime( 0 );
		final Route teleportationRoute =
			modeRouteFactory.createRoute(
					TELEPORTATION_LEG_MODE,
					originStation.getLinkId(),
					destinationStation.getLinkId());
		teleportationRoute.setTravelTime( 0 );
		teleportationLeg.setRoute( teleportationRoute );
		trip.add( teleportationLeg );

		// create a dummy activity at the teleportation destination
		final Activity secondInteraction =
			populationFactory.createActivityFromLinkId(
					STAGE,
					destinationStation.getLinkId());
		secondInteraction.setMaximumDuration( 0 );
		trip.add( secondInteraction );

		// route the egress trip
		trip.addAll(
				ptDelegate.calcRoute(
					destinationStation,
					toFacility,
					departureTime + calcDuration( trip ),
					person ) );

		return trip;
	}

	private static double calcDuration(final List<PlanElement> trip) {
		double dur = 0;
		// assume there's code computing trip duration here.
		return dur;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();

		// trips for this mode contain the ones we create, plus the ones of the
		// pt router we use.
		stageTypes.addActivityTypes( ptDelegate.getStageActivityTypes() );
		stageTypes.addActivityTypes( new StageActivityTypesImpl( STAGE ) );

		return stageTypes;
	}
}

