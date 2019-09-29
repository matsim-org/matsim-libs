/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author mrieser / SBB
 */
public final class RaptorUtils {

    private RaptorUtils() {
    }

    public static RaptorStaticConfig createStaticConfig(Config config) {
        PlansCalcRouteConfigGroup pcrConfig = config.plansCalcRoute();
        SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

        RaptorStaticConfig staticConfig = new RaptorStaticConfig();

        staticConfig.setBeelineWalkConnectionDistance(config.transitRouter().getMaxBeelineWalkConnectionDistance());

        PlansCalcRouteConfigGroup.ModeRoutingParams walk = pcrConfig.getModeRoutingParams().get(TransportMode.walk);
        staticConfig.setBeelineWalkSpeed(walk.getTeleportedModeSpeed() / walk.getBeelineDistanceFactor());
        staticConfig.setBeelineWalkDistanceFactor(walk.getBeelineDistanceFactor());

        staticConfig.setMinimalTransferTime(config.transitRouter().getAdditionalTransferTime());

        staticConfig.setUseModeMappingForPassengers(srrConfig.isUseModeMappingForPassengers());
        if (srrConfig.isUseModeMappingForPassengers()) {
            for (SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet mapping : srrConfig.getModeMappingForPassengers()) {
                staticConfig.addModeMappingForPassengers(mapping.getRouteMode(), mapping.getPassengerMode());
            }
        }

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

        PlanCalcScoreConfigGroup pcsConfig = config.planCalcScore();
        double marginalUtilityPerforming = pcsConfig.getPerforming_utils_hr() / 3600.0;
        for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> e : pcsConfig.getModes().entrySet()) {
            String mode = e.getKey();
            PlanCalcScoreConfigGroup.ModeParams modeParams = e.getValue();
            double marginalUtility_utl_s = modeParams.getMarginalUtilityOfTraveling()/3600.0 - marginalUtilityPerforming;
            raptorParams.setMarginalUtilityOfTravelTime_utl_s(mode, marginalUtility_utl_s);
        }
        
		for (String fallbackMode : Arrays.asList(TransportMode.non_network_walk,
				TransportMode.transit_walk)) {
			if (!pcsConfig.getModes().containsKey(fallbackMode)) {
				PlanCalcScoreConfigGroup.ModeParams modeParams = pcsConfig.getModes().get(TransportMode.walk);
				double marginalUtility_utl_s = modeParams.getMarginalUtilityOfTraveling() / 3600.0
						- marginalUtilityPerforming;
				raptorParams.setMarginalUtilityOfTravelTime_utl_s(fallbackMode, marginalUtility_utl_s);
			}
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

    public static List<Leg> convertRouteToLegs(RaptorRoute route) {
        List<Leg> legs = new ArrayList<>(route.parts.size());
        double lastArrivalTime = Time.getUndefinedTime();
        for (RaptorRoute.RoutePart part : route.parts) {
            if (part.planElements != null) {
                for (PlanElement pe : part.planElements) {
                    if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        legs.add(leg);
                        if (Time.isUndefinedTime(leg.getDepartureTime())) {
                            leg.setDepartureTime(lastArrivalTime);
                        }
                        lastArrivalTime = leg.getDepartureTime() + leg.getTravelTime();
                    }
                }
            } else if (part.line != null) {
                // a pt leg
                Leg ptLeg = PopulationUtils.createLeg(part.mode);
                ptLeg.setDepartureTime(part.depTime);
                ptLeg.setTravelTime(part.arrivalTime - part.depTime);
                ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(part.fromStop, part.line, part.route, part.toStop);
                ptRoute.setTravelTime(part.arrivalTime - part.depTime);
                ptRoute.setDistance(part.distance);
                ptLeg.setRoute(ptRoute);
                legs.add(ptLeg);
                lastArrivalTime = part.arrivalTime;
            } else {
                // a non-pt leg
                Leg walkLeg = PopulationUtils.createLeg(part.mode);
                walkLeg.setDepartureTime(part.depTime);
                walkLeg.setTravelTime(part.arrivalTime - part.depTime);
                Id<Link> startLinkId = part.fromStop == null ? (route.fromFacility == null ? null : route.fromFacility.getLinkId()) : part.fromStop.getLinkId();
                Id<Link> endLinkId =  part.toStop == null ? (route.toFacility == null ? null : route.toFacility.getLinkId()) : part.toStop.getLinkId();
                Route walkRoute = RouteUtils.createGenericRouteImpl(startLinkId, endLinkId);
                walkRoute.setTravelTime(part.arrivalTime - part.depTime);
                walkRoute.setDistance(part.distance);
                walkLeg.setRoute(walkRoute);
                legs.add(walkLeg);
                lastArrivalTime = part.arrivalTime;
            }
        }

        return legs;
    }
}
