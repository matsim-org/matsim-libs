/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtRoutingModule.java
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
package playground.thibautd.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.pt.PtConstants;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;
import playground.meisterk.kti.router.SwissHaltestellen;

/**
 * @author thibautd
 */
public class KtiPtRoutingModule implements RoutingModule {
	private final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );

	private final Matrix ptTravelTimes;
	private final SwissHaltestellen ptStops;
	private final PlansCalcRouteConfigGroup config;
	private final NetworkImpl network;

	public KtiPtRoutingModule(
			final PlansCalcRouteConfigGroup config,
			// I hate constuctors which read into files,
			// but do not want to let the dirtyness out of here
			final String worldFile,
			final String travelTimeMatrixFile,
			final String ptStopsFile,
			final Network network) {
		this.network = (NetworkImpl) network;
		this.config = config;

		final KtiConfigGroup dummyGroup = new KtiConfigGroup();
		dummyGroup.setWorldInputFilename( worldFile );
		dummyGroup.setPtTraveltimeMatrixFilename( travelTimeMatrixFile );
		dummyGroup.setPtHaltestellenFilename( ptStopsFile );

		final PlansCalcRouteKtiInfo ptInfo = new PlansCalcRouteKtiInfo( dummyGroup );
		ptInfo.prepare( network );

		this.ptTravelTimes = ptInfo.getPtTravelTimes();
		this.ptStops = ptInfo.getHaltestellen();
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final SwissHaltestelle stop1 = ptStops.getClosestLocation( fromFacility.getCoord() );
		final SwissHaltestelle stop2 = ptStops.getClosestLocation( toFacility.getCoord() );

		final List<PlanElement> trip = new ArrayList<PlanElement>();

		final Link endWalk1 = network.getNearestLink( stop1.getCoord() );
		final Link startWalk2 = network.getNearestLink( stop2.getCoord() );
		// access
		// ---------------------------------------------------------------------
		final double distanceLeg1 =
			CoordUtils.calcDistance( 
					fromFacility.getCoord(),
					stop1.getCoord() ) * config.getBeelineDistanceFactor();
		final double travelTimeLeg1 = distanceLeg1 * config.getWalkSpeed();

		final Leg walk1 = new LegImpl( TransportMode.walk );
		final Route route1 = new GenericRouteImpl( fromFacility.getLinkId() , endWalk1.getId() );
		route1.setTravelTime( travelTimeLeg1 );
		walk1.setRoute( route1 );
		trip.add( walk1 );

		// pt
		// ---------------------------------------------------------------------
		final Entry ptTravelTimeEntry = ptTravelTimes.getEntry( stop1.getId() , stop2.getId() );

		final Leg ptLeg = new LegImpl( TransportMode.pt );
		final Route ptRoute = new GenericRouteImpl( endWalk1.getId() , startWalk2.getId() );
		ptRoute.setTravelTime( ptTravelTimeEntry.getValue() );
		ptRoute.setDistance(
				// factor hard-coded KTI-like
				1.5 * CoordUtils.calcDistance(
					stop1.getCoord(),
					stop2.getCoord() ) );
		ptLeg.setRoute( ptRoute );
		trip.add( ptLeg );

		// egress
		// ---------------------------------------------------------------------
		final double distanceLeg2 =
			CoordUtils.calcDistance( 
					stop2.getCoord(),
					toFacility.getCoord() ) * config.getBeelineDistanceFactor();
		final double travelTimeLeg2 = distanceLeg2 * config.getWalkSpeed();

		final Leg walk2 = new LegImpl( TransportMode.walk );
		final Route route2 = new GenericRouteImpl( startWalk2.getId() , toFacility.getLinkId() );
		route2.setTravelTime( travelTimeLeg2 );
		route2.setDistance( distanceLeg2 );
		walk2.setRoute( route2 );
		trip.add( walk2 );

		assert trip.size() == 3;
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return stages;
	}
}

