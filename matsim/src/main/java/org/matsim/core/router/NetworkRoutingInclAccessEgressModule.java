/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import static org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import javax.annotation.Nullable;

/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that routes from a {@link Facility} to another {@link Facility}, as we need
 * in MATSim.
 *
 * @author thibautd, nagel
 */
public final class NetworkRoutingInclAccessEgressModule implements RoutingModule {

	private static final Logger log = LogManager.getLogger(NetworkRoutingInclAccessEgressModule.class);

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network filteredNetwork;
	private final LeastCostPathCalculator routeAlgo;
	private final Scenario scenario;
	private final RoutingModule accessToNetworkRouter;
	private final RoutingModule egressFromNetworkRouter;
	private final Config config;

	private static boolean hasWarnedAccessEgress = false;
	private final RoutingConfigGroup.AccessEgressType accessEgressType;
	private final TimeInterpretation timeInterpretation;

	private final MultimodalLinkChooser multimodalLinkChooser;

	/**
	 * If not null given, the main routing will be performed on an inverted network.
	 */
	@Nullable
	private final Network invertedNetwork;

	NetworkRoutingInclAccessEgressModule(
			final String mode,
			final LeastCostPathCalculator routeAlgo, Scenario scenario, Network filteredNetwork, @Nullable Network invertedNetwork,
			final RoutingModule accessToNetworkRouter,
			final RoutingModule egressFromNetworkRouter,
			final TimeInterpretation timeInterpretation,
			final MultimodalLinkChooser multimodalLinkChooser) {
		this.multimodalLinkChooser = multimodalLinkChooser;
		Gbl.assertNotNull(scenario.getNetwork());
		Gbl.assertIf(!scenario.getNetwork().getLinks().isEmpty()); // otherwise network for mode probably not defined
		this.filteredNetwork = filteredNetwork;
		this.invertedNetwork = invertedNetwork;
		this.routeAlgo = routeAlgo;
		this.mode = mode;
		this.scenario = scenario;
		this.populationFactory = scenario.getPopulation().getFactory();
		this.config = scenario.getConfig();
		this.accessToNetworkRouter = accessToNetworkRouter;
		this.egressFromNetworkRouter = egressFromNetworkRouter;
		this.accessEgressType = config.routing().getAccessEgressType();
		this.timeInterpretation = timeInterpretation;
		if (accessEgressType.equals(AccessEgressType.none)) {
			throw new RuntimeException("trying to use access/egress but not switched on in config.  "
					+ "currently not supported; there are too many other problems");
		} else if (accessEgressType.equals(AccessEgressType.walkConstantTimeToLink) && !hasWarnedAccessEgress) {
			hasWarnedAccessEgress = true;
			log.warn("you are using AccessEgressType=" + AccessEgressType.walkConstantTimeToLink +
					". That means, access and egress won't get network-routed - even if you specified corresponding RoutingModules for access and egress ");
		}
		if (invertedNetwork != null && !(routeAlgo instanceof InvertedLeastPathCalculator)) {
			throw new IllegalArgumentException("Inverted network must be used with inverted least path calculator.");
		}
	}

	@Override
	public synchronized List<? extends PlanElement> calcRoute(RoutingRequest request) {
		// I need this "synchronized" since I want mobsim agents to be able to call this during the mobsim.  So when the
		// mobsim is multi-threaded, multiple agents might call this here at the same time.  kai, nov'17

		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link accessActLink = multimodalLinkChooser.decideAccessLink(request, filteredNetwork);
		Link egressActLink = multimodalLinkChooser.decideEgressLink(request, filteredNetwork);

		double now = departureTime;

		List<PlanElement> result = new ArrayList<>();

		// === access:
		{
			List<? extends PlanElement> accessTrip = computeAccessTripFromFacilityToLinkIfNecessary(fromFacility, person, accessActLink, now, populationFactory, mode,
					scenario.getConfig(), request.getAttributes());
			if(accessTrip == null ) return null; //access trip could not get routed so we return null for the entire trip => will lead to the tripRouter to call fallbackRoutingModule
			now = timeInterpretation.decideOnElementsEndTime(accessTrip, now).seconds();
			result.addAll(accessTrip);
		}

		// === compute the network leg:
		{
			Leg newLeg = this.populationFactory.createLeg(this.mode);
			newLeg.setDepartureTime(now);
			now += routeLeg(person, newLeg, accessActLink, egressActLink, now);

			result.add(newLeg);
			//			log.warn( newLeg );
		}

		// === egress:
		{
			List<PlanElement> egressTrip = computeEgressTripFromLinkToFacilityIfNecessary(toFacility, person, egressActLink, now, result.get(result.size() - 1), populationFactory, mode,
					scenario.getConfig(), request.getAttributes());
			if(egressTrip == null ) return null; //egress trip could not get routed so we return null for the entire trip => will lead to the tripRouter to call fallbackRoutingModule
			result.addAll(egressTrip);
		}

		return result;
	}

	private List<PlanElement> computeEgressTripFromLinkToFacilityIfNecessary(final Facility toFacility, final Person person,
																final Link egressActLink, double departureTime, PlanElement previousPlanElement,
																final PopulationFactory populationFactory, final String stageActivityType,
																Config config, Attributes routingAttributes) {

		log.debug("do bushwhacking leg from link=" + egressActLink.getId() + " to facility=" + toFacility.toString());

		if (isNotNeedingBushwhackingLeg(toFacility)) {
			return Collections.emptyList();
		}

		Coord startCoord = NetworkUtils.findNearestPointOnLink(toFacility.getCoord(),egressActLink);
		Gbl.assertNotNull(startCoord);
		final Id<Link> startLinkId = egressActLink.getId();

		List<PlanElement> egressTrip = new ArrayList<>();
		// check whether we already have an identical interaction activity directly before
		if (previousPlanElement instanceof Leg) {
			final Activity interactionActivity = createInteractionActivity(startCoord, startLinkId, stageActivityType);
			egressTrip.add(interactionActivity);
		} else {
			// don't add another (interaction) activity
			// TODO: assuming that this is an interaction activity, e.g. walk - drt interaction - walk
			// Not clear what we should do if it is not an interaction activity (and how that could happen).
		}

		Id<Link> endLinkId = toFacility.getLinkId();
		if (endLinkId == null) {
			endLinkId = startLinkId;
		}

		if (mode.equals(TransportMode.walk)) {
			Leg egressLeg = populationFactory.createLeg(TransportMode.non_network_walk);
			// (Here we need the non_network_walk!! ... since we need a way to bushwhack from the facility to the walk network!  kai, may'21)

			egressLeg.setDepartureTime(departureTime);
			routeBushwhackingLeg(person, egressLeg, startCoord, toFacility.getCoord(), departureTime, startLinkId, endLinkId, populationFactory, config);
			egressTrip.add(egressLeg);
		} else if (accessEgressType.equals(AccessEgressType.walkConstantTimeToLink)) {
			Leg egressLeg = populationFactory.createLeg(TransportMode.walk);
			egressLeg.setDepartureTime(departureTime);
			routeBushwhackingLeg(person, egressLeg, startCoord, toFacility.getCoord(), departureTime, startLinkId, endLinkId, populationFactory, config);
			double egressTime = NetworkUtils.getLinkEgressTime(egressActLink, mode).orElseThrow(()->new RuntimeException("Egress Time not set for link "+ egressActLink.getId()));
			egressLeg.setTravelTime(egressTime);
			egressLeg.getRoute().setTravelTime(egressTime);
			egressTrip.add(egressLeg);
		} else if ( accessEgressType==AccessEgressType.accessEgressModeToLink || accessEgressType==AccessEgressType.accessEgressModeToLinkPlusTimeConstant ) {
			Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(egressActLink,startCoord);
			List<? extends PlanElement> networkRoutedEgressTrip = egressFromNetworkRouter.calcRoute(DefaultRoutingRequest.of(fromFacility, toFacility, departureTime, person, routingAttributes));
			if(networkRoutedEgressTrip == null) return null;
			if (this.accessEgressType.equals(RoutingConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant)){
				double egressTime = NetworkUtils.getLinkEgressTime(egressActLink,mode).orElseThrow(()->new RuntimeException("Egress Time not set for link "+ egressActLink.getId().toString()));
				Leg leg0 = TripStructureUtils.getLegs(networkRoutedEgressTrip).get(0);
				double travelTime = leg0.getTravelTime().seconds()+egressTime;
				leg0.setTravelTime(travelTime);
				leg0.getRoute().setTravelTime(travelTime);
			}
			egressTrip.addAll(networkRoutedEgressTrip);
		} else {
			throw new RuntimeException( "should not happen" );
		}
		return egressTrip;
	}

	private static boolean isNotNeedingBushwhackingLeg(Facility toFacility) {
		if (toFacility.getCoord() == null) {
			// facility does not have a coordinate; we cannot bushwhack
			return true;
		}
		// trip ends on link; no need to bushwhack (this is, in fact, not totally clear: might be link on network of other mode)
		return toFacility instanceof LinkWrapperFacility;
	}

	private List<? extends PlanElement> computeAccessTripFromFacilityToLinkIfNecessary(final Facility fromFacility, final Person person,
																  final Link accessActLink, double departureTime,
																  final PopulationFactory populationFactory, final String stageActivityType,
																  Config config, Attributes routingAttributes) {
		if (isNotNeedingBushwhackingLeg(fromFacility)) {
			return Collections.emptyList();
		}

		Coord endCoord = NetworkUtils.findNearestPointOnLink(fromFacility.getCoord(),accessActLink);
		List<PlanElement> accessTrip = new ArrayList<>();

		if (mode.equals(TransportMode.walk)) {
			Leg accessLeg = populationFactory.createLeg(TransportMode.non_network_walk);
			// (Here we need the non_network_walk!! ... since we need a way to bushwhack from the facility to the walk network!  kai, may'21)

			accessLeg.setDepartureTime(departureTime);

			Id<Link> startLinkId = fromFacility.getLinkId();
			if (startLinkId == null) {
				startLinkId = accessActLink.getId();
			}

			routeBushwhackingLeg(person, accessLeg, fromFacility.getCoord(), endCoord, departureTime, startLinkId, accessActLink.getId(), populationFactory, config);
			// yyyy might be possible to set the link ids to null. kai & dominik, may'16

			accessTrip.add(accessLeg);
		} else if (accessEgressType.equals(RoutingConfigGroup.AccessEgressType.walkConstantTimeToLink)) {
			Leg accessLeg = populationFactory.createLeg(TransportMode.walk);
			accessLeg.setDepartureTime(departureTime);
			Id<Link> startLinkId = fromFacility.getLinkId();
			if (startLinkId == null) {
				startLinkId = accessActLink.getId();
			}
			routeBushwhackingLeg(person, accessLeg, fromFacility.getCoord(), endCoord, departureTime, startLinkId, accessActLink.getId(), populationFactory,
					config);

			double accessTime = NetworkUtils.getLinkAccessTime(accessActLink, mode).orElseThrow(()->new RuntimeException("Access Time not set for link "+ accessActLink.getId().toString()));
			accessLeg.setTravelTime(accessTime);
			accessLeg.getRoute().setTravelTime(accessTime);
			accessTrip.add(accessLeg);
//			now += accessTime;
		} else if ( accessEgressType==AccessEgressType.accessEgressModeToLink || accessEgressType==AccessEgressType.accessEgressModeToLinkPlusTimeConstant ) {
			Facility toFacility = FacilitiesUtils.wrapLinkAndCoord(accessActLink,endCoord);
			List<? extends PlanElement> networkRoutedAccessTrip = accessToNetworkRouter.calcRoute(DefaultRoutingRequest.of(fromFacility, toFacility, departureTime, person, routingAttributes));
			if (networkRoutedAccessTrip == null) return null; //no access trip could be computed for accessMode
			if (this.accessEgressType.equals(RoutingConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant)){
				double accessTime = NetworkUtils.getLinkAccessTime(accessActLink,mode).orElseThrow(()->new RuntimeException("Access Time not set for link "+ accessActLink.getId().toString()));
				Leg leg0 = TripStructureUtils.getLegs(networkRoutedAccessTrip).get(0);
				double travelTime = leg0.getTravelTime().seconds()+accessTime;
				leg0.setTravelTime(travelTime);
				leg0.getRoute().setTravelTime(travelTime);
			}
			accessTrip.addAll(networkRoutedAccessTrip);
		} else {
			throw new RuntimeException( "should not happen" );
		}

		final Activity interactionActivity = createInteractionActivity(endCoord, accessActLink.getId(), stageActivityType);
		accessTrip.add(interactionActivity);
		return accessTrip;
	}

	private static Activity createInteractionActivity(final Coord interactionCoord, final Id<Link> interactionLink, final String mode) {
        return PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(interactionCoord, interactionLink, mode);
	}

	private static void routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, Config config) {
		final RoutingConfigGroup.TeleportedModeParams params;
		RoutingConfigGroup.TeleportedModeParams tmp;
		final Map<String, RoutingConfigGroup.TeleportedModeParams> paramsMap = config.routing().getModeRoutingParams();
		if ((tmp = paramsMap.get(TransportMode.non_network_walk)) != null) {
			params = tmp;
		} else if ((tmp = paramsMap.get(TransportMode.walk)) != null) {
			params = tmp;
		} else {
			log.fatal( "Teleportation (= mode routing) params neither defined for " + TransportMode.walk + " nor for " + TransportMode.non_network_walk + ".  There are two cases:" );
			log.fatal( "(1) " + TransportMode.walk + " is teleported.  Then you need to define the corresponding teleportation (= mode routing) params for " + TransportMode.walk + "." );
			log.fatal( "(2) " + TransportMode.walk + " is routed on the network.  Then you need to define the corresponding teleportation (= mode routing) params for "
						  + TransportMode.non_network_walk + ".");
			log.fatal("The old default fallback bevhavior was disabled in may'21.");
			throw new RuntimeException( "Need teleportation params for bushwhacking modes.  See log statements above." );

//			params = new ModeRoutingParams();
//			// old defaults
//			params.setBeelineDistanceFactor(1.3);
//			params.setTeleportedModeSpeed(2.0);

			// yyyyyy The above may be a source for buggy behavior: If the teleportation params are cleared, then presumably also the
			// non-network routing is cleared, and then here it will fall back on the auto-magic behavior.  kai, may'21.

			// Not sure if this can happen:
			// (1) If walk is a non-network mode, then using the params from that is what we want.
			// (2) If walk is a network mode, then the walk params cannot be defined as teleportation mode params at the same time.
			// But presumably there are other cases.  kai, jun'22
		}

		routeBushwhackingLeg(person, leg, fromCoord, toCoord, depTime, dpLinkId, arLinkId, pf, params);
	}

	static void routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, TeleportedModeParams params ) {
		// I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
		// map facilities, and if you follow through to the teleportation routers one even finds activity wrappers, which is yet another
		// complication which I certainly don't want here.  kai, dec'15

		// dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
		// for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15

		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);

		// create an empty route, but with realistic travel time
		Route route = pf.getRouteFactories().createRoute(Route.class, dpLinkId, arLinkId);

		Gbl.assertNotNull(params);
		double beelineDistanceFactor = params.getBeelineDistanceFactor();
		double networkTravelSpeed = params.getTeleportedModeSpeed();

		double estimatedNetworkDistance = dist * beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / networkTravelSpeed);


		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode=" + this.mode + "]";
	}

	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Link fromLink, Link toLink, double depTime) {
		double travTime;

		Node startNode = fromLink.getToNode();    // start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		if (toLink != fromLink) { // (a "true" route)

			if (invertedNetwork != null) {
				startNode = invertedNetwork.getNodes().get(Id.create(fromLink.getId(), Node.class));
				endNode = invertedNetwork.getNodes().get(Id.create(toLink.getId(), Node.class));
			}

			Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, leg.getMode());
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, vehicle);
			if (path == null) {
				throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + " for mode " + mode + ".");
			}

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());

			double relPosOnDepartureLink = 1.0;
			double relPosOnArrivalLink = 1.0;

			double maxSpeedOnToLink = Math.min(vehicle.getType().getMaximumVelocity(),toLink.getFreespeed());
			double travelTimeEstimateOnToLink = (toLink.getLength() / maxSpeedOnToLink) * relPosOnArrivalLink;
			route.setTravelTime((int) (path.travelTime+travelTimeEstimateOnToLink));

			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, relPosOnDepartureLink, relPosOnArrivalLink, this.filteredNetwork));
			route.setVehicleId(vehicleId);
			leg.setRoute(route);
			travTime = (int) path.travelTime;

		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			route.setVehicleId(VehicleUtils.getVehicleId(person, leg.getMode()));
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);

		return travTime;
	}
}
