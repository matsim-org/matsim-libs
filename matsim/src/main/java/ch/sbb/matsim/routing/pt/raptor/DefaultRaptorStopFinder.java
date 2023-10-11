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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.TransitScheduleUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;

/**
 * @author mrieser / Simunto GmbH
 */
public class DefaultRaptorStopFinder implements RaptorStopFinder {

	private final RaptorIntermodalAccessEgress intermodalAE;
	private final Map<String, RoutingModule> routingModules;
    private final Random random = MatsimRandom.getLocalInstance();

	@Inject
	public DefaultRaptorStopFinder(Config config, RaptorIntermodalAccessEgress intermodalAE, Map<String, Provider<RoutingModule>> routingModuleProviders) {
		this.intermodalAE = intermodalAE;

		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		this.routingModules = new HashMap<>();
		if (srrConfig.isUseIntermodalAccessEgress()) {
			for (IntermodalAccessEgressParameterSet params : srrConfig.getIntermodalAccessEgressParameterSets()) {
				String mode = params.getMode();
				this.routingModules.put(mode, routingModuleProviders.get(mode).get());
			}
		}
	}

	public DefaultRaptorStopFinder(RaptorIntermodalAccessEgress intermodalAE, Map<String, RoutingModule> routingModules) {
		this.intermodalAE = intermodalAE;
		this.routingModules = routingModules;
	}

	@Override
	public List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person, double departureTime, Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data, RaptorStopFinder.Direction type) {
		if (type == Direction.ACCESS) {
			return findAccessStops(fromFacility, toFacility, person, departureTime, routingAttributes, parameters, data);
		}
		if (type == Direction.EGRESS) {
			return findEgressStops(fromFacility, toFacility, person, departureTime, routingAttributes, parameters, data);
		}
		return Collections.emptyList();
	}

	private List<InitialStop> findAccessStops(Facility fromFacility, Facility toFacility, Person person, double departureTime,
			Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data) {
		SwissRailRaptorConfigGroup srrCfg = parameters.getConfig();
		if (srrCfg.isUseIntermodalAccessEgress()) {
			return findIntermodalStops(fromFacility, toFacility, person, departureTime, routingAttributes, Direction.ACCESS, parameters, data);
		} else {
			double distanceFactor = data.config.getBeelineWalkDistanceFactor();
			List<TransitStopFacility> stops = findNearbyStops(fromFacility, parameters, data);
			List<InitialStop> initialStops = stops.stream().map(stop -> {
				double beelineDistance = CoordUtils.calcEuclideanDistance(stop.getCoord(), fromFacility.getCoord());
				double accessTime = TransitScheduleUtils.getStopAccessTime(stop);
				double travelTime = Math.ceil(beelineDistance / parameters.getBeelineWalkSpeed()) + accessTime;
				double disutility = travelTime * -parameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.walk);
				return new InitialStop(stop, disutility, travelTime, beelineDistance * distanceFactor, TransportMode.walk);
			}).collect(Collectors.toList());
			return initialStops;
		}
	}

	private List<InitialStop> findEgressStops(Facility fromFacility, Facility toFacility, Person person, double departureTime, Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data) {
		SwissRailRaptorConfigGroup srrCfg = parameters.getConfig();
		if (srrCfg.isUseIntermodalAccessEgress()) {
			return findIntermodalStops(fromFacility, toFacility, person, departureTime, routingAttributes, Direction.EGRESS, parameters, data);
		} else {
			double distanceFactor = data.config.getBeelineWalkDistanceFactor();
			List<TransitStopFacility> stops = findNearbyStops(toFacility, parameters, data);
			List<InitialStop> initialStops = stops.stream().map(stop -> {
				double beelineDistance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toFacility.getCoord());
				double egressTime = TransitScheduleUtils.getStopEgressTime(stop);
				double travelTime = Math.ceil(beelineDistance / parameters.getBeelineWalkSpeed()) + egressTime;
				double disutility = travelTime * -parameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.walk);
				return new InitialStop(stop, disutility, travelTime, beelineDistance * distanceFactor, TransportMode.walk);
			}).collect(Collectors.toList());
			return initialStops;
		}
	}

	private List<InitialStop> findIntermodalStops(Facility fromFacility, Facility toFacility, Person person, double departureTime,
			Attributes routingAttributes, Direction direction, RaptorParameters parameters, SwissRailRaptorData data) {
		SwissRailRaptorConfigGroup srrCfg = parameters.getConfig();
		Facility facility = Direction.ACCESS == direction ? fromFacility : toFacility;
		double x = facility.getCoord().getX();
		double y = facility.getCoord().getY();
		List<InitialStop> initialStops = new ArrayList<>();
        switch (srrCfg.getIntermodalAccessEgressModeSelection()) {
            case CalcLeastCostModePerStop:
                for (IntermodalAccessEgressParameterSet parameterSet : srrCfg.getIntermodalAccessEgressParameterSets()) {
                    addInitialStopsForParamSet(fromFacility, toFacility, person, departureTime, routingAttributes, direction, parameters, data, x, y, initialStops, parameterSet);
                }
                break;
            case RandomSelectOneModePerRoutingRequestAndDirection:
                int counter = 0;
                do {
                    int rndSelector = random.nextInt(srrCfg.getIntermodalAccessEgressParameterSets().size());
                    addInitialStopsForParamSet(fromFacility, toFacility, person, departureTime, routingAttributes, direction, parameters, data, x, y,
                            initialStops, srrCfg.getIntermodalAccessEgressParameterSets().get(rndSelector));
                    counter++;
                    // try again if no initial stop was found for the parameterset. Avoid infinite loop by limiting number of tries.
                } while (initialStops.isEmpty() && counter < 2 * srrCfg.getIntermodalAccessEgressParameterSets().size());
                break;
            default:
                throw new RuntimeException(srrCfg.getIntermodalAccessEgressModeSelection() + " : not implemented!");
        }
        return initialStops;
    }

    private void addInitialStopsForParamSet(Facility fromFacility, Facility toFacility, Person person, double departureTime, Attributes routingAttributes, Direction direction, RaptorParameters parameters, SwissRailRaptorData data, double x, double y, List<InitialStop> initialStops, IntermodalAccessEgressParameterSet paramset) {
        String mode = paramset.getMode();
        String linkIdAttribute = paramset.getLinkIdAttribute();
        String personFilterAttribute = paramset.getPersonFilterAttribute();
        String personFilterValue = paramset.getPersonFilterValue();
        String stopFilterAttribute = paramset.getStopFilterAttribute();
        String stopFilterValue = paramset.getStopFilterValue();

        Facility facility = Direction.ACCESS == direction ? fromFacility : toFacility;
        boolean personMatches = true;
        if (personFilterAttribute != null) {
            Object attr = person.getAttributes().getAttribute(personFilterAttribute);
            String attrValue = attr == null ? null : attr.toString();
            personMatches = personFilterValue.equals(attrValue);
        }

        if (personMatches) {
            QuadTree<TransitStopFacility> filteredStopsQT;
            if (stopFilterAttribute != null) {
                data.prepareStopFilterQuadTreeIfNotExistent(stopFilterAttribute, stopFilterValue);
                filteredStopsQT = data.stopFilterAttribute2Value2StopsQT.get(stopFilterAttribute).get(stopFilterValue);
            } else {
                filteredStopsQT = data.stopsQT;
            }

            double distance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
            double tripBasedSearchRadius = distance * paramset.getShareTripSearchRadius();
            double searchRadius = Math.min(paramset.getInitialSearchRadius(), paramset.getMaxRadius());
            searchRadius  = Math.min(searchRadius, tripBasedSearchRadius);
            Collection<TransitStopFacility> stopFacilities = filteredStopsQT.getDisk(x, y, searchRadius);
            if (stopFacilities.size() < 2) {
                TransitStopFacility nearestStop = filteredStopsQT.getClosest(x, y);
                double nearestDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), nearestStop.getCoord());
                searchRadius = Math.min(nearestDistance + paramset.getSearchExtensionRadius(), paramset.getMaxRadius());
                stopFacilities = filteredStopsQT.getDisk(x, y, searchRadius);
            }

            for (TransitStopFacility stop : stopFacilities) {
                Facility stopFacility = stop;
                if (linkIdAttribute != null) {
                    Object attr = stop.getAttributes().getAttribute(linkIdAttribute);
                    if (attr != null) {
                        stopFacility = new ChangedLinkFacility(stop, Id.create(attr.toString(), Link.class));
                    }
                }

                List<? extends PlanElement> routeParts;
                if (direction == Direction.ACCESS) {
                    RoutingModule module = this.routingModules.get(mode);
                    routeParts = module.calcRoute(DefaultRoutingRequest.of(facility, stopFacility, departureTime, person, routingAttributes));
                } else { // it's Egress
                    // We don't know the departure time for the egress trip, so just use the original departureTime,
                    // although it is wrong and might result in a wrong traveltime and thus wrong route.
                    RoutingModule module = this.routingModules.get(mode);
                    routeParts = module.calcRoute(DefaultRoutingRequest.of(stopFacility, facility, departureTime, person, routingAttributes));
                    if (routeParts == null) {
                        // the router for the access/egress mode could not find a route, skip that access/egress mode
                        continue;
                    }
                }
                if (routeParts == null) {
                    // the router for the access/egress mode could not find a route, skip that access/egress mode
                    continue;
                }
				double accessTime = TransitScheduleUtils.getStopAccessTime(stop);
				double egressTime = TransitScheduleUtils.getStopEgressTime(stop);
				if ((stopFacility != stop) || accessTime>0.0 || egressTime>0.0) {
					if (direction == Direction.ACCESS) {
						Leg transferLeg = PopulationUtils.createLeg(TransportMode.walk);
						Route transferRoute = RouteUtils.createGenericRouteImpl(stopFacility.getLinkId(), stop.getLinkId());
						transferRoute.setTravelTime(accessTime);
						transferRoute.setDistance(0);
						transferLeg.setRoute(transferRoute);
						transferLeg.setTravelTime(accessTime);

						List<PlanElement> tmp = new ArrayList<>(routeParts.size() + 1);
						tmp.addAll(routeParts);
						tmp.add(transferLeg);
						routeParts = tmp;
					} else {
						Leg transferLeg = PopulationUtils.createLeg(TransportMode.walk);
						Route transferRoute = RouteUtils.createGenericRouteImpl(stop.getLinkId(), stopFacility.getLinkId());
                        transferRoute.setTravelTime(egressTime);
                        transferRoute.setDistance(0);
                        transferLeg.setRoute(transferRoute);
                        transferLeg.setTravelTime(egressTime);

                        List<PlanElement> tmp = new ArrayList<>(routeParts.size() + 1);
                        tmp.add(transferLeg);
                        tmp.addAll(routeParts);
                        routeParts = tmp;
                    }
                }
                RaptorIntermodalAccessEgress.RIntermodalAccessEgress accessEgress = this.intermodalAE.calcIntermodalAccessEgress(routeParts, parameters, person, direction);
                InitialStop iStop = new InitialStop(stop, accessEgress.disutility, accessEgress.travelTime, accessEgress.routeParts);
                initialStops.add(iStop);

                if (direction == Direction.EGRESS) {
									// clear the (wrong) departureTime so users don't get confused
									// do it only after passing it to RIntermodalAccessEgress, in case this wants to make some use of it.
									for (PlanElement pe : routeParts) {
										if (pe instanceof Leg) {
											((Leg) pe).setDepartureTimeUndefined();
										}
									}
								}
						}
        }
	}

	private List<TransitStopFacility> findNearbyStops(Facility facility, RaptorParameters parameters, SwissRailRaptorData data) {
		double x = facility.getCoord().getX();
		double y = facility.getCoord().getY();
		Collection<TransitStopFacility> stopFacilities = data.stopsQT.getDisk(x, y, parameters.getSearchRadius());
		if (stopFacilities.size() < 2) {
			TransitStopFacility  nearestStop = data.stopsQT.getClosest(x, y);
			double nearestDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), nearestStop.getCoord());
			stopFacilities = data.stopsQT.getDisk(x, y, nearestDistance + parameters.getExtensionRadius());
		}
		if (stopFacilities instanceof List) {
			return (List<TransitStopFacility>) stopFacilities;
		}
		return new ArrayList<>(stopFacilities);
	}

	private static class ChangedLinkFacility implements Facility, Identifiable<TransitStopFacility> {

		private final TransitStopFacility delegate;
		private final Id<Link> linkId;

		ChangedLinkFacility(final TransitStopFacility delegate, final Id<Link> linkId) {
			this.delegate = delegate;
			this.linkId = linkId;
		}

		@Override
		public Id<Link> getLinkId() {
			return this.linkId;
		}

		@Override
		public Coord getCoord() {
			return this.delegate.getCoord();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return this.delegate.getCustomAttributes();
		}

		@Override
		public Id<TransitStopFacility> getId() {
			return this.delegate.getId();
		}
	}
}
