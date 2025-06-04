/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

/**
 * @author mrieser / SBB
 */
public final class RaptorUtils {
    private static final Logger log = LogManager.getLogger( RaptorUtils.class );

    private RaptorUtils() {
    }

    public static RaptorStaticConfig createStaticConfig(Config config) {
        RoutingConfigGroup routingConfig = config.routing();
        SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

        RaptorStaticConfig staticConfig = new RaptorStaticConfig();

		staticConfig.setBeelineWalkConnectionDistance(config.transitRouter().getMaxBeelineWalkConnectionDistance());
//		RoutingConfigGroup.TeleportedModeParams walk = routingConfig.getModeRoutingParams().get(TransportMode.walk );

        // the code below also exists in TransitRouterConfig.  kai, mar'25
        RoutingConfigGroup.TeleportedModeParams params = routingConfig.getTeleportedModeParams().get( TransportMode.transit_walk );
        if ( params==null ) {
            params = routingConfig.getTeleportedModeParams().get( TransportMode.non_network_walk );
        }
        if ( params==null ) {
            params = routingConfig.getTeleportedModeParams().get( TransportMode.walk) ;
        }
        if ( params==null ) {
            log.error( "teleported mode params do not exist for " + TransportMode.transit_walk + ", " + TransportMode.non_network_walk + ", nor "
                               + TransportMode.walk + ".  At least one of them needs to be defined for TransitRouterConfig.  Aborting ...");
            // yyyy I do not know which of this is conceptually the correct one.  It should _not_ be walk since that may be routed on the network, see TransitRouterConfig.  kai, mar'25
        }
        Gbl.assertNotNull( params );
        RoutingConfigGroup.TeleportedModeParams walk = params;

        staticConfig.setBeelineWalkSpeed(walk.getTeleportedModeSpeed() / walk.getBeelineDistanceFactor());
		staticConfig.setBeelineWalkDistanceFactor(walk.getBeelineDistanceFactor());
		staticConfig.setTransferWalkMargin(srrConfig.getTransferWalkMargin());
		staticConfig.setIntermodalLegOnlyHandling(srrConfig.getIntermodalLegOnlyHandling());
		staticConfig.setMinimalTransferTime(config.transitRouter().getAdditionalTransferTime());
		staticConfig.setTransferCalculation(srrConfig.getTransferCalculation());

        staticConfig.setUseModeMappingForPassengers(srrConfig.isUseModeMappingForPassengers());
        if (srrConfig.isUseModeMappingForPassengers()) {
            for (SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet mapping : srrConfig.getModeMappingForPassengers()) {
                staticConfig.addModeMappingForPassengers(mapping.getRouteMode(), mapping.getPassengerMode());
            }
        }

		for (SwissRailRaptorConfigGroup.ModeToModeTransferPenalty penalty : srrConfig.getModeToModeTransferPenaltyParameterSets()){
			staticConfig.addModeToModeTransferPenalty(penalty.fromMode,penalty.toMode,penalty.transferPenalty);
		}
        staticConfig.setUseCapacityConstraints(srrConfig.isUseCapacityConstraints());

        return staticConfig;
    }

    public static RaptorParameters createParameters(Config config) {
        SwissRailRaptorConfigGroup advancedConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

        TransitRouterConfig trConfig = new TransitRouterConfig(config);
        RaptorParameters raptorParams = new RaptorParameters(advancedConfig);
        raptorParams.setBeelineWalkSpeed(trConfig.getBeelineWalkSpeed());

        raptorParams.setSearchRadius(config.transitRouter().getSearchRadius());
        raptorParams.setExtensionRadius(config.transitRouter().getExtensionRadius());
        raptorParams.setDirectWalkFactor(config.transitRouter().getDirectWalkFactor());

        raptorParams.setMarginalUtilityOfWaitingPt_utl_s(trConfig.getMarginalUtilityOfWaitingPt_utl_s());

        ScoringConfigGroup pcsConfig = config.scoring();
        double marginalUtilityPerforming = pcsConfig.getPerforming_utils_hr() / 3600.0;
        for (Map.Entry<String, ScoringConfigGroup.ModeParams> e : pcsConfig.getModes().entrySet()) {
            String mode = e.getKey();
            ScoringConfigGroup.ModeParams modeParams = e.getValue();
            double marginalUtility_utl_s = modeParams.getMarginalUtilityOfTraveling()/3600.0 - marginalUtilityPerforming;
            raptorParams.setMarginalUtilityOfTravelTime_utl_s(mode, marginalUtility_utl_s);
        }

        double costPerHour = advancedConfig.getTransferPenaltyCostPerTravelTimeHour();
        if (costPerHour == 0.0) {
            // for backwards compatibility, use the default utility of line switch.
            raptorParams.setTransferPenaltyFixCostPerTransfer(-trConfig.getUtilityOfLineSwitch_utl());
        } else {
            raptorParams.setTransferPenaltyFixCostPerTransfer(advancedConfig.getTransferPenaltyBaseCost());
        }
        raptorParams.setTransferPenaltyPerTravelTimeHour(costPerHour);
        raptorParams.setTransferPenaltyMinimum(advancedConfig.getTransferPenaltyMinCost());
        raptorParams.setTransferPenaltyMaximum(advancedConfig.getTransferPenaltyMaxCost());

        return raptorParams;
    }

    public static List<? extends PlanElement> convertRouteToLegs(RaptorRoute route, double transferWalkMargin) {
        List<PlanElement> legs = new ArrayList<>(route.parts.size());
        double lastArrivalTime = Double.NaN;
        boolean firstPtLegProcessed = false;
        Leg previousTransferWalkleg = null;
        for (RaptorRoute.RoutePart part : route.parts) {
            if (part.planElements != null) {
                for (PlanElement pe : part.planElements) {
                    if (pe instanceof Leg leg) {
						legs.add(leg);
                        if (leg.getDepartureTime().isUndefined()) {
                            leg.setDepartureTime(lastArrivalTime);
                        }
                        lastArrivalTime = leg.getDepartureTime().seconds() + leg.getTravelTime().seconds();
                    }
                    else {
                    	Activity act = (Activity) pe;
                    	legs.add(act);
                    }
                }
            } else if (part.line != null) {
                // a pt leg
                Leg ptLeg = PopulationUtils.createLeg(part.mode);
                ptLeg.setDepartureTime(part.depTime);
                ptLeg.setTravelTime(part.getChainedArrivalTime() - part.depTime);

                ptLeg.setRoute(convertRoutePart(part));

				legs.add(ptLeg);
                lastArrivalTime = part.getChainedArrivalTime();
                firstPtLegProcessed = true;
                if (previousTransferWalkleg != null) {
                    //adds the margin only to legs in between pt legs
                    double traveltime = Math.max(0, previousTransferWalkleg.getTravelTime().seconds() - transferWalkMargin);
                    previousTransferWalkleg.setTravelTime(traveltime);
                    previousTransferWalkleg.getRoute().setTravelTime(traveltime);

                }
            } else {
                // a non-pt leg
                Leg walkLeg = PopulationUtils.createLeg(part.mode);
                walkLeg.setDepartureTime(part.depTime);
                double travelTime = part.arrivalTime - part.depTime;
                walkLeg.setTravelTime(travelTime);
                Id<Link> startLinkId = part.fromStop == null ? (route.fromFacility == null ? null : route.fromFacility.getLinkId()) : part.fromStop.getLinkId();
                Id<Link> endLinkId = part.toStop == null ? (route.toFacility == null ? null : route.toFacility.getLinkId()) : part.toStop.getLinkId();
                Route walkRoute = RouteUtils.createGenericRouteImpl(startLinkId, endLinkId);
                walkRoute.setTravelTime(travelTime);
                walkRoute.setDistance(part.distance);
                walkLeg.setRoute(walkRoute);
                legs.add(walkLeg);
                lastArrivalTime = part.arrivalTime;
                if (firstPtLegProcessed) {
                    previousTransferWalkleg = walkLeg;
                }
            }
        }

        return legs;
    }


	/**
	 * Create passenger routes recursively.
	 */
	private static DefaultTransitPassengerRoute convertRoutePart(RaptorRoute.RoutePart part) {

		if (part == null)
			return null;

		DefaultTransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(part.fromStop, part.line, part.route, part.toStop, convertRoutePart(part.chainedPart));
		ptRoute.setBoardingTime(part.boardingTime);
		ptRoute.setTravelTime(part.arrivalTime - part.depTime);
		ptRoute.setDistance(part.distance);

		return ptRoute;
	}
}
