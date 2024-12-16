/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.TourActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Router routing scheduledTours.
 *
 * @author sschroeder
 *
 */
public class CarrierTimeAndSpaceTourRouter{

	static class MatsimVehicleAdapter implements Vehicle {

		private final CarrierVehicle carrierVehicle;

		private final Attributes attributes = new AttributesImpl();

		public MatsimVehicleAdapter(CarrierVehicle vehicle) {
			this.carrierVehicle = vehicle;
		}

		@Override
		public Id<Vehicle> getId() {
			return carrierVehicle.getId();
		}

		@Override
		public VehicleType getType() {
			return carrierVehicle.getType();
		}

		public CarrierVehicle getCarrierVehicle() {
			return carrierVehicle;
		}

		@Override
		public Attributes getAttributes() {
			return this.attributes;
		}
	}


	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger( CarrierTimeAndSpaceTourRouter.class );

	private final LeastCostPathCalculator router;

	private final Network network;

	private final TravelTime travelTime;

	/**
	 * Constructs the timeAndSpaceRouter with a leastCostPathCalculator, network and travelTime.
	 * @param router				the leastCostPathCalculator
	 * @param network				the network
	 * @param travelTime			the travelTime
	 * @see LeastCostPathCalculator, Network, TravelTime
	 */
	public CarrierTimeAndSpaceTourRouter( LeastCostPathCalculator router, Network network, TravelTime travelTime ) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}

	/**
	 * Routes a scheduledTour in time and space.
	 *
	 * <p>Uses a leastCostPathCalculator to calculate a route/path from one activity to another. It starts at the departureTime of
	 * the scheduledTour and determines activity arrival and departure times considering activities time-windows.
	 * @param tour	the scheduledTour to be routed.
	 */
	public void route(ScheduledTour tour) {
		MatsimVehicleAdapter matsimVehicle = new MatsimVehicleAdapter(tour.getVehicle());
		double currTime = tour.getDeparture();
		Id<Link> prevLink = tour.getTour().getStartLinkId();
		Leg prevLeg = null;
		for(TourElement e : tour.getTour().getTourElements()){
			if(e instanceof Leg){
				prevLeg = (Leg) e;
				prevLeg.setDepartureTime(currTime);
			}
			if(e instanceof TourActivity act){
				route(prevLeg, prevLink, act.getLocation(), matsimVehicle);
				assert prevLeg != null;
				double expectedArrival = currTime + prevLeg.getExpectedTransportTime();
				act.setExpectedArrival(expectedArrival);
				double startAct = Math.max(expectedArrival, act.getTimeWindow().getStart());
				currTime = startAct + act.getDuration();
				prevLink = act.getLocation();
			}
		}
		Id<Link> endLink = tour.getTour().getEndLinkId();
		route(prevLeg,prevLink,endLink, matsimVehicle);
	}

	private void route(Leg prevLeg, Id<Link> fromLinkId, Id<Link> toLinkId, Vehicle vehicle) {
		if(fromLinkId.equals(toLinkId)){
			prevLeg.setExpectedTransportTime(0);
			NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
			route.setDistance(0.0);
			route.setTravelTime(0.0);
//			route.setVehicleId(vehicle.getId());
			prevLeg.setRoute(route);
			return;
		}
		Path path = router.calcLeastCostPath(network.getLinks().get(fromLinkId).getToNode(), network.getLinks().get(toLinkId).getFromNode(), prevLeg.getExpectedDepartureTime(), null, vehicle);
		double travelTime = path.travelTime;

		/*
		 *ACHTUNG. Konsistenz zu VRP
		 */
		double toLinkTravelTime = this.travelTime.getLinkTravelTime(network.getLinks().get(toLinkId),prevLeg.getExpectedDepartureTime()+travelTime, null, vehicle);
		travelTime += toLinkTravelTime;
		prevLeg.setExpectedTransportTime(travelTime);
		NetworkRoute route = createRoute(fromLinkId,path,toLinkId);
//		route.setVehicleId(vehicle.getId());
		prevLeg.setRoute(route);
	}

	private NetworkRoute createRoute(Id<Link> fromLink, Path path, Id<Link> toLink) {
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLink, toLink);
		route.setLinkIds(fromLink, getLinkIds(path.links), toLink);
		return route;
	}

	private List<Id<Link>> getLinkIds(List<Link> links) {
		List<Id<Link>> linkIds = new ArrayList<>();
		for(Link l : links){
			linkIds.add(l.getId());
		}
		return linkIds;
	}


}
