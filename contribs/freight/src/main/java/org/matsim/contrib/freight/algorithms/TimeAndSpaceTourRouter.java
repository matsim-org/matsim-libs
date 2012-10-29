package org.matsim.contrib.freight.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.vrp.utils.matsim2vrp.MatsimVehicleAdapter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

public class TimeAndSpaceTourRouter {
	
private static Logger logger = Logger.getLogger(ReRouteVehicles.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;

	public TimeAndSpaceTourRouter(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}
	
	public void route(ScheduledTour tour) {
		MatsimVehicleAdapter matsimVehicle = new MatsimVehicleAdapter(tour.getVehicle());
		double currTime = tour.getDeparture();
		Id prevLink = tour.getTour().getStartLinkId();
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
				act.setExpectedActStart(startAct);
				act.setExpectedActEnd(currTime);
				prevLink = act.getLocation();
			}
		}
		Id endLink = tour.getTour().getEndLinkId();
		route(prevLeg,prevLink,endLink, null, matsimVehicle);
	}
	
	private void route(Leg prevLeg, Id fromLinkId, Id toLinkId, Person person, Vehicle vehicle) {
		if(fromLinkId.equals(toLinkId)){
			prevLeg.setExpectedTransportTime(0);
			LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(fromLinkId,toLinkId);
			route.setDistance(0.0);
			route.setTravelTime(0.0);
//			route.setVehicleId(vehicle.getId());
			prevLeg.setRoute(route);
			return;
		}
		Path path = router.calcLeastCostPath(network.getLinks().get(fromLinkId).getToNode(), network.getLinks().get(toLinkId).getFromNode(), prevLeg.getDepartureTime(), person, vehicle);
		double travelTime = path.travelTime;
		
		/*
		 *ACHTUNG. Konsistenz zu VRP 
		 */
		double toLinkTravelTime = this.travelTime.getLinkTravelTime(network.getLinks().get(toLinkId),prevLeg.getDepartureTime()+travelTime, person, vehicle);
		travelTime += toLinkTravelTime;
		prevLeg.setExpectedTransportTime(travelTime);
		NetworkRoute route = createRoute(fromLinkId,path,toLinkId);
//		route.setVehicleId(vehicle.getId());
		prevLeg.setRoute(route);
	}
	
	private NetworkRoute createRoute(Id fromLink, Path path, Id toLink) {
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(fromLink, toLink);
		route.setLinkIds(fromLink, getLinkIds(path.links), toLink);
		return route;
	}
	
	private List<Id> getLinkIds(List<Link> links) {
		List<Id> linkIds = new ArrayList<Id>();
		for(Link l : links){
			linkIds.add(l.getId());
		}
		return linkIds;
	}


}
