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
package playground.ivt.kticompatibility;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.pt.PtConstants;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;
import playground.meisterk.kti.router.SwissHaltestellen;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class KtiPtRoutingModule implements RoutingModule {
	private static final double KTI_CROWFLY_FACTOR = 1.5;
	private final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );

	private final  PlansCalcRouteConfigGroup config;
	private final NetworkImpl network;
	private final KtiPtRoutingModuleInfo info;

	public KtiPtRoutingModule(
			final PlansCalcRouteConfigGroup config,
			final KtiPtRoutingModuleInfo info,
			final Network network) {
		this.network = (NetworkImpl) network;
		this.config = config;
		this.info = info;
	}

	@Override
	public List<PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final SwissHaltestelle stop1 = info.ptStops.getClosestLocation( fromFacility.getCoord() );
		final SwissHaltestelle stop2 = info.ptStops.getClosestLocation( toFacility.getCoord() );

		final List<PlanElement> trip = new ArrayList<PlanElement>();

		final Link linkStartPt = NetworkUtils.getNearestLink(network, stop1.getCoord());
		final Link linkEndPt = NetworkUtils.getNearestLink(network, stop2.getCoord());

		// access
		// ---------------------------------------------------------------------
		final double distanceLeg1 =
			CoordUtils.calcEuclideanDistance( 
					fromFacility.getCoord(),
					stop1.getCoord() ) * KTI_CROWFLY_FACTOR;
		final double travelTimeLeg1 = distanceLeg1 * config.getTeleportedModeSpeeds().get(TransportMode.walk);

		final Leg walk1 = new LegImpl( TransportMode.transit_walk );
		final Route route1 = new GenericRouteImpl( fromFacility.getLinkId() , linkStartPt.getId() );
		walk1.setTravelTime( travelTimeLeg1 );
		route1.setTravelTime( travelTimeLeg1 );
		route1.setDistance( distanceLeg1 );
		walk1.setRoute( route1 );
		trip.add( walk1 );

		trip.add(
				createInteraction(
					stop1.getCoord(),
					linkStartPt.getId() ) );

		// pt
		// ---------------------------------------------------------------------
		if (true)
			throw new RuntimeException("Reference to balmermi removed! the rest of the code is not working");

	//	final Layer municipalities = info.world.getLayer("municipality");
	//	final List<Zone> froms = municipalities.getNearestLocations( stop1.getCoord() );
	//	final List<Zone> tos = municipalities.getNearestLocations( stop2.getCoord() );
	//	final Zone fromMunicipality = froms.get(0);
	//	final Zone toMunicipality = tos.get(0);
/*
		final Entry ptTravelTimeEntry =
			info.ptTravelTimes.getEntry(
					fromMunicipality.getId().toString(),
					toMunicipality.getId().toString() );

		final Leg ptLeg = new LegImpl( TransportMode.pt );
		final Route ptRoute = new GenericRouteImpl( linkStartPt.getId() , linkEndPt.getId() );

		final double ptDistance =
			// factor hard-coded KTI-like
			KTI_CROWFLY_FACTOR * CoordUtils.calcEuclideanDistance(
				stop1.getCoord(),
				stop2.getCoord() );
		final double ptTravelTime =
			Double.isNaN( ptTravelTimeEntry.getValue() ) ?
				// A value of NaN in the travel time matrix indicates that the matrix
				// contains no valid value for this entry.
				// In this case, the travel time is calculated with the distance of
				// the relation and an average speed.
				ptDistance / info.intrazonalSpeed :
				ptTravelTimeEntry.getValue() * 60;

		ptLeg.setTravelTime( ptTravelTime );
		ptRoute.setTravelTime( ptTravelTime );
		ptRoute.setDistance( ptDistance );
		ptLeg.setRoute( ptRoute );
		trip.add( ptLeg );

		trip.add(
				createInteraction(
					stop2.getCoord(),
					linkEndPt.getId() ) );

		// egress
		// ---------------------------------------------------------------------
		final double distanceLeg2 =
			CoordUtils.calcEuclideanDistance( 
					stop2.getCoord(),
					toFacility.getCoord() ) * KTI_CROWFLY_FACTOR;
		final double travelTimeLeg2 = distanceLeg2 * config.getTeleportedModeSpeeds().get(TransportMode.walk);

		final Leg walk2 = new LegImpl( TransportMode.transit_walk );
		final Route route2 = new GenericRouteImpl( linkEndPt.getId() , toFacility.getLinkId() );
		walk2.setTravelTime( travelTimeLeg2 );
		route2.setTravelTime( travelTimeLeg2 );
		route2.setDistance( distanceLeg2 );
		walk2.setRoute( route2 );
		trip.add( walk2 );
*/
		return trip;
	}

	private static Activity createInteraction(
			final Coord coord,
			final Id link) {
		final Activity act = new ActivityImpl( PtConstants.TRANSIT_ACTIVITY_TYPE , coord , link );
		act.setMaximumDuration( 0 );
		return act;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return stages;
	}

	public static class KtiPtRoutingModuleInfo {
		private final Matrix ptTravelTimes;
		private final SwissHaltestellen ptStops;
		//private final World world;
		private final double intrazonalSpeed;

		public KtiPtRoutingModuleInfo(
				final KtiPtConfigGroup config,
				final Network network) {
			this( config.getIntrazonalPtSpeed(),
					config.getWorldFile(),
					config.getTravelTimesFile(),
					config.getPtStopsFile(),
					network);
		}

		public KtiPtRoutingModuleInfo(
				final double intrazonalSpeed,
				// I hate constuctors which read into files,
				// but do not want to let the dirtyness out of here
				final String worldFile,
				final String travelTimeMatrixFile,
				final String ptStopsFile,
				final Network network) {
			this.intrazonalSpeed = intrazonalSpeed;
			final KtiConfigGroup dummyGroup = new KtiConfigGroup();
			dummyGroup.setWorldInputFilename( worldFile );
			dummyGroup.setPtTraveltimeMatrixFilename( travelTimeMatrixFile );
			dummyGroup.setPtHaltestellenFilename( ptStopsFile );

			final PlansCalcRouteKtiInfo ptInfo = new PlansCalcRouteKtiInfo( dummyGroup );
			ptInfo.prepare( network );

			this.ptTravelTimes = ptInfo.getPtTravelTimes();
			this.ptStops = ptInfo.getHaltestellen();
			//this.world = ptInfo.getLocalWorld();
		}
	}
}

