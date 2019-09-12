package org.matsim.contrib.freight.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Router routing scheduledTours.
 * 
 * @author sschroeder
 *
 */
public class TimeAndSpaceTourRouter {
	
	static class MatsimVehicleAdapter implements Vehicle {

		private CarrierVehicle carrierVehicle;

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

	}
	
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(TimeAndSpaceTourRouter.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;

	/**
	 * Constructs the timeAndSpaceRouter with a leastCostPathCalculator, network and travelTime.
	 * @param router
	 * @param network
	 * @param travelTime
	 * @see LeastCostPathCalculator, Network, TravelTime
	 */
	public TimeAndSpaceTourRouter(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
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
	 * @param tour
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
			if(e instanceof TourActivity){
				TourActivity act = (TourActivity) e;
				route(prevLeg, prevLink, act.getLocation(), null, matsimVehicle);
				double expectedArrival = currTime + prevLeg.getExpectedTransportTime();
				act.setExpectedArrival(expectedArrival);
				double startAct = Math.max(expectedArrival, act.getTimeWindow().getStart()); 
				currTime = startAct + act.getDuration();
				prevLink = act.getLocation();
			}
		}
		Id<Link> endLink = tour.getTour().getEndLinkId();
		route(prevLeg,prevLink,endLink, null, matsimVehicle);
	}
	
	private void route(Leg prevLeg, Id<Link> fromLinkId, Id<Link> toLinkId, Person person, Vehicle vehicle) {
		if(fromLinkId.equals(toLinkId)){
			prevLeg.setExpectedTransportTime(0);
			NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
			route.setDistance(0.0);
			route.setTravelTime(0.0);
//			route.setVehicleId(vehicle.getId());
			prevLeg.setRoute(route);
			return;
		}
		Path path = router.calcLeastCostPath(network.getLinks().get(fromLinkId).getToNode(), network.getLinks().get(toLinkId).getFromNode(), prevLeg.getExpectedDepartureTime(), person, vehicle);
		double travelTime = path.travelTime;
		
		/*
		 *ACHTUNG. Konsistenz zu VRP 
		 */
		double toLinkTravelTime = this.travelTime.getLinkTravelTime(network.getLinks().get(toLinkId),prevLeg.getExpectedDepartureTime()+travelTime, person, vehicle);
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
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		for(Link l : links){
			linkIds.add(l.getId());
		}
		return linkIds;
	}


}
