/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalPlansCalcRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.multimodal.router;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.christoph.multimodal.router.costcalculator.BufferedTravelTime;
import playground.christoph.multimodal.router.costcalculator.MultiModalTravelTimeCost;
import playground.christoph.multimodal.router.costcalculator.TravelTimeCalculatorWithBuffer;

public class MultiModalPlansCalcRoute extends PlansCalcRoute {

	private RouteFactory walkRouteFactory = new LinkNetworkRouteFactory();
	private RouteFactory bikeRouteFactory = new LinkNetworkRouteFactory();
	private RouteFactory ptRouteFactory = new LinkNetworkRouteFactory();
	private RouteFactory rideRouteFactory = new LinkNetworkRouteFactory();
	
	private IntermodalLeastCostPathCalculator walkRouteAlgo;
	private IntermodalLeastCostPathCalculator bikeRouteAlgo;
	private IntermodalLeastCostPathCalculator ptRouteAlgo;
	private IntermodalLeastCostPathCalculator rideRouteAlgo;
	
	private LeastCostPathCalculatorFactory factory;
	private PersonalizableTravelTime travelTime;	
	
	public MultiModalPlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final PersonalizableTravelCost costCalculator,
			final PersonalizableTravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		super(group, network, costCalculator, timeCalculator, factory);
		
		this.factory = factory;
		this.travelTime = timeCalculator;
		initRouteAlgos(network, timeCalculator);
	}

	public MultiModalPlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}

	private void initRouteAlgos(Network network, PersonalizableTravelTime travelTime) {	
		/*
		 * Car
		 * Add mode restriction because now the network is multi-modal and not car-only.
		 */
		LeastCostPathCalculator carRouteAlgo = this.getLeastCostPathCalculator();
		if (carRouteAlgo instanceof IntermodalLeastCostPathCalculator) {
			Set<String> carModeRestrictions = new TreeSet<String>();
			carModeRestrictions.add(TransportMode.car);
			((IntermodalLeastCostPathCalculator)carRouteAlgo).setModeRestriction(carModeRestrictions);
		}
		
		
		/*
		 * Walk
		 */
		MultiModalTravelTimeCost walkTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.walk);
		walkRouteAlgo = (IntermodalLeastCostPathCalculator)factory.createPathCalculator(network, walkTravelTimeCost, walkTravelTimeCost);
			
		Set<String> walkModeRestrictions = new TreeSet<String>();
		walkModeRestrictions.add(TransportMode.walk);
		walkModeRestrictions.add(TransportMode.car);
		walkRouteAlgo.setModeRestriction(walkModeRestrictions);
		
		
		/*
		 * Bike
		 */
		MultiModalTravelTimeCost bikeTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.bike);
		bikeRouteAlgo = (IntermodalLeastCostPathCalculator)factory.createPathCalculator(network, bikeTravelTimeCost, bikeTravelTimeCost);
		
		/*
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		Set<String> bikeModeRestrictions = new TreeSet<String>();
		bikeModeRestrictions.add(TransportMode.bike);
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.car);
		bikeRouteAlgo.setModeRestriction(bikeModeRestrictions);
		
		
		/*
		 * PT
		 * If possible, we use "real" TravelTimes from previous iterations - if not,
		 * freeSpeedTravelTimes are used.
		 * We set the ScaleFactor to 1.25 which means that the returned TravelTimes
		 * will be higher (e.g. to respect stops that a bus performs to pick up agents).
		 */
		MultiModalTravelTimeCost ptTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.pt);
		if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
			BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
			bufferedTravelTime.setScaleFactor(1.25);
			ptTravelTimeCost.setTravelTime(bufferedTravelTime);
		}
		
		ptRouteAlgo = (IntermodalLeastCostPathCalculator)factory.createPathCalculator(network, ptTravelTimeCost, ptTravelTimeCost);
		
		/*
		 * We assume PT trips are possible on every road that can be used by cars.
		 * 
		 * Additionally we also allow pt trips to use walk and / or bike only links.
		 * On those links the traveltimes are quite high and we can assume that they
		 * are only use e.g. to walk from the origin to the bus station or from the
		 * bus station to the destination.
		 */
		Set<String> ptModeRestrictions = new TreeSet<String>();
//		ptModeRestrictions.add(TransportMode.pt);
		ptModeRestrictions.add(TransportMode.car);
		ptModeRestrictions.add(TransportMode.bike);
		ptModeRestrictions.add(TransportMode.walk);
		ptRouteAlgo.setModeRestriction(ptModeRestrictions);
		
		
		/*
		 * Ride
		 * If possible, we use "real" TravelTimes from previous iterations - if not,
		 * freeSpeedTravelTimes are used.
		 */
		MultiModalTravelTimeCost rideTravelTimeCost = new MultiModalTravelTimeCost(this.configGroup, TransportMode.ride);
		if (travelTime instanceof TravelTimeCalculatorWithBuffer) {
			BufferedTravelTime bufferedTravelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) travelTime);
			bufferedTravelTime.setScaleFactor(1.0);
			rideTravelTimeCost.setTravelTime(bufferedTravelTime);
		}
		
		rideRouteAlgo = (IntermodalLeastCostPathCalculator)factory.createPathCalculator(network, rideTravelTimeCost, rideTravelTimeCost);
		
		/*
		 * We assume ride trips are possible on every road that can be used by cars.
		 */
		Set<String> rideModeRestrictions = new TreeSet<String>();
		rideModeRestrictions.add(TransportMode.car);
		rideRouteAlgo.setModeRestriction(rideModeRestrictions);
	}
	
	/*
	 * As long as the core does not use PersonalizableTravelTime we set the
	 * person in the TimeCalculator here and then let the superclass do the
	 * remaining work.
	 */
	@Override
	protected void handlePlan(Person person, final Plan plan) {
		if (travelTime != null) {
			travelTime.setPerson(person);
		}
		super.handlePlan(person, plan);
	}
	
	@Override
	protected double handleWalkLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, walkRouteAlgo, walkRouteFactory);
	}
	
	@Override
	protected double handleBikeLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, bikeRouteAlgo, bikeRouteFactory);
	}
	
	@Override
	protected double handlePtLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, ptRouteAlgo, ptRouteFactory);
	}
	
	@Override
	protected double handleRideLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return handleMultiModalLeg(leg, fromAct, toAct, depTime, rideRouteAlgo, rideRouteFactory);
	}
	
	/*
	 * Adapted from PlansCalcRoute.handleCarLeg(...) - exchanged only the routeAlgo and the routeFactory.
	 */
	private double handleMultiModalLeg(Leg leg, Activity fromAct, Activity toAct, double depTime, 
			IntermodalLeastCostPathCalculator routeAlgo, RouteFactory routeFactory)
	{
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}
}
