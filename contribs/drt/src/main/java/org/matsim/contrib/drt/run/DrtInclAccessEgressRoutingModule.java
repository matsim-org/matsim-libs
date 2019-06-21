/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run;

import com.google.inject.name.Named;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.matsim.core.router.NetworkRoutingInclAccessEgressModule.*;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public class DrtInclAccessEgressRoutingModule implements RoutingModule {
	private static final Logger LOGGER = Logger.getLogger( DrtInclAccessEgressRoutingModule.class );

	private final DrtConfigGroup drtCfg;
	private final Network network;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final PopulationFactory populationFactory;
	private final RoutingModule walkRouter;
	private final DrtStageActivityType drtStageActivityType;

	DrtInclAccessEgressRoutingModule( DrtConfigGroup drtCfg, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
						    @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
						    TravelDisutilityFactory travelDisutilityFactory, PopulationFactory populationFactory,
						    @Named(TransportMode.walk) RoutingModule walkRouter ) {
//		LOGGER.setLevel( Level.DEBUG );

		this.drtCfg = drtCfg;
		this.network = network;
		this.travelTime = travelTime;
		this.populationFactory = populationFactory;
		this.walkRouter = walkRouter;
		this.drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());

		// Euclidean with overdoFactor > 1.0 could lead to 'experiencedTT < unsharedRideTT',
		// while the benefit would be a marginal reduction of computation time ==> so stick to 1.0
		router = new FastAStarEuclideanFactory().createPathCalculator(network,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		LOGGER.debug( "entering calcRoute ..." );
		LOGGER.debug("fromFacility=" + fromFacility.toString() ) ;
		LOGGER.debug( "toFacility=" + toFacility.toString() );

		Gbl.assertNotNull( fromFacility );
		Gbl.assertNotNull( toFacility );

		Link accessActLink = FacilitiesUtils.decideOnLink( fromFacility, network ) ;
		Link egressActLink = FacilitiesUtils.decideOnLink( toFacility, network ) ;

		double now = departureTime ;

		if (accessActLink == egressActLink){
			if( drtCfg.isPrintDetailedWarnings() ){
				LOGGER.error( "Start and end stop are the same, agent will walk using mode "
							    + drtStageActivityType.drtWalk
							    + ". Agent Id:\t"
							    + person.getId() );
			}
			Leg leg = (Leg) walkRouter.calcRoute( fromFacility, toFacility, departureTime, person ).get( 0 );
			leg.setDepartureTime( now );
			leg.setMode( drtStageActivityType.drtWalk );
			LOGGER.debug( "travel time on walk leg=" + leg.getTravelTime() );
			return Collections.singletonList( leg ) ;
		}

		List<PlanElement> result = new ArrayList<>() ;

		// === access:
		{
			now = addBushwhackingLegFromFacilityToLinkIfNecessary( fromFacility, person, accessActLink, now, result, populationFactory, drtStageActivityType.drtStageActivity ) ;
		}

		// === drt proper:
		{
			VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath( accessActLink, egressActLink, departureTime, router, travelTime );
			double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
			double maxTravelTime = getMaxTravelTime( drtCfg, unsharedRideTime );
			double unsharedDistance = VrpPaths.calcDistance( unsharedPath );//includes last link

			DrtRoute route = populationFactory.getRouteFactories().createRoute( DrtRoute.class, accessActLink.getId(), egressActLink.getId() );
			route.setDistance( unsharedDistance );
			route.setTravelTime( maxTravelTime );
			route.setUnsharedRideTime( unsharedRideTime );
			route.setMaxWaitTime( drtCfg.getMaxWaitTime() );

			Leg leg = populationFactory.createLeg( drtCfg.getMode() );
			leg.setDepartureTime( departureTime );
			leg.setTravelTime( maxTravelTime );
			leg.setRoute( route );

			result.add( leg ) ;
			now += maxTravelTime ;
		}

		// === egress:
		{
			addBushwhackingLegFromLinkToFacilityIfNecessary( toFacility, person, egressActLink, now, result, populationFactory, drtStageActivityType.drtStageActivity) ;
		}

		return result ;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	/**
	 * Calculates the maximum travel time defined as: drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta()
	 *
	 * @param drtCfg
	 * @param unsharedRideTime ride time of the direct (shortest-time) route
	 * @return maximum travel time
	 */
	public static double getMaxTravelTime(DrtConfigGroup drtCfg, double unsharedRideTime) {
		return drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta();
	}
}
