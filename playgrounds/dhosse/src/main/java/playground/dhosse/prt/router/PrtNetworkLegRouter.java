package playground.dhosse.prt.router;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.pt.router.TransitRouter;

public class PrtNetworkLegRouter implements TransitRouter{

	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord,
			double departureTime, Person person) {
		// TODO Auto-generated method stub
		return null;
	}

//	private final Network network;
//	private final ModeRouteFactory routeFactory;
//	private final LeastCostPathCalculator router;
//	
//	public PrtNetworkLegRouter(final Network network, final LeastCostPathCalculator router, final ModeRouteFactory routeFactory){
//		this.network = network;
//		this.routeFactory = routeFactory;
//		this.router = router;
//	}
//
//	@Override
//	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord,
//			double departureTime, Person person) {
//		
//
//		Link wrappedFromLink = PrtData.getNearestRank(fromCoord).getLink();
//		Node wrappedFromNode = wrappedFromLink.getFromNode();
//		Link wrappedToLink = PrtData.getNearestRank(toCoord).getLink();
//		Node wrappedToNode = wrappedToLink.getFromNode();
//		
//		Path p = this.router.calcLeastCostPath(wrappedFromNode, wrappedToNode, departureTime, person, null);
//		
//		if(p == null) return null;
//		
//		return convertPathToLegList(departureTime, p, fromCoord, toCoord, person, wrappedFromLink, wrappedToLink);
//		
//	}
//	
//	private List<Leg> convertPathToLegList(double departureTime,
//			Path p, Coord fromCoord, Coord toCoord, Person person, Link wrappedFromLink,
//			Link wrappedToLink){
//		
//		double time = departureTime;
//		List<Leg> legs = new ArrayList<Leg>();
//		Leg leg;
//		double prtTravelDistance = 0.;
//		NetworkImpl networkImpl = (NetworkImpl)this.network;
//		
//		//first walk leg
//		leg = new LegImpl(TransportMode.walk);
//		Link startLink = networkImpl.getNearestLinkExactly(fromCoord);
//		Path path = this.router.calcLeastCostPath(startLink.getFromNode(),
//				wrappedFromLink.getFromNode(), time, person, null);
//		GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
//				startLink.getId(), wrappedFromLink.getId());
//		route.setStartLinkId(startLink.getId());
//		route.setEndLinkId(wrappedFromLink.getId());
//		route.setTravelTime(path.travelTime);
//		double distance = 0.;
//		for(Link l : path.links) distance += l.getLength();
//		route.setDistance(distance);
//		leg.setRoute(route);
//		legs.add(leg);
//		time += leg.getTravelTime();
//		
//		//prt leg
//		leg = new LegImpl(PrtRequestCreator.MODE);
//		route = (GenericRouteImpl) this.routeFactory.createRoute(PrtRequestCreator.MODE,
//				wrappedFromLink.getId(), wrappedToLink.getId());
//		route.setStartLinkId(wrappedFromLink.getId());
//		route.setEndLinkId(wrappedToLink.getId());
//		for(Link l : p.links){
//			prtTravelDistance += l.getLength();
//		}
//		route.setDistance(prtTravelDistance);
//		route.setTravelTime((int)p.travelTime);
//		leg.setRoute(route);
//		legs.add(leg);
//		time += leg.getTravelTime();
//		
//		//walk leg to destination
//		leg = new LegImpl(TransportMode.walk);
//		Link endLink = networkImpl.getNearestLinkExactly(toCoord);
//		path = this.router.calcLeastCostPath(wrappedToLink.getFromNode(),
//				endLink.getFromNode(), time, person, null);
//		route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
//				wrappedToLink.getId(), endLink.getId());
//		route.setTravelTime(leg.getTravelTime());
//		route.setStartLinkId(startLink.getId());
//		route.setEndLinkId(wrappedToLink.getId());
//		distance = 0.;
//		for(Link l : path.links) distance += l.getLength();
//		route.setDistance(distance);
//		route.setTravelTime(path.travelTime);
//		leg.setRoute(route);
//		time += leg.getTravelTime();
//		legs.add(leg);
//		
////		Link startLink = null;
////		Link endLink = null;
////		
////		if(((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getLinkId() == null || 
////				((ActivityImpl)person.getSelectedPlan().getPlanElements().get(2)).getLinkId() == null){
////			startLink = ((NetworkImpl)this.network).getNearestLinkExactly(fromActivity.getCoord());
////			endLink = ((NetworkImpl)this.network).getNearestLinkExactly(toActivity.getCoord());
////			
////			((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).setLinkId(startLink.getId());
////			((ActivityImpl)person.getSelectedPlan().getPlanElements().get(2)).setLinkId(endLink.getId());
////		} else{
////			
////			startLink = this.network.getLinks().get(((Activity)person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
////			endLink = this.network.getLinks().get(((Activity)person.getSelectedPlan().getPlanElements().get(2)).getLinkId());
////			
////		}
////
////		Link fromActivityRankLink = p.links.get(0);
////		Link toActivityRankLink = p.links.get(p.links.size()-1);
////		
////		if(person.getId().toString().equals("3025")){
////			System.out.println("");
////		}
////		
////		if(endLink != startLink && fromActivityRankLink != toActivityRankLink){
////			
////			if(fromActivityRankLink != startLink){
////				
////				Leg walkLeg = new LegImpl(TransportMode.walk);
////				Path path = this.router.calcLeastCostPath(startLink.getFromNode(),
////						fromActivityRankLink.getFromNode(), time, person, null);
////				GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
////						startLink.getId(), fromActivityRankLink.getId());
////				route.setStartLinkId(startLink.getId());
////				route.setEndLinkId(fromActivityRankLink.getId());
////				route.setTravelTime((int)path.travelTime);
////				walkLeg.setRoute(route);
////				
////				legs.add(walkLeg);
////				
////				time += path.travelTime;
////				
////				
////			} else{
////				
////				Leg walkLeg = new LegImpl(TransportMode.walk);
////				GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
////						startLink.getId(), fromActivityRankLink.getId());
////				route.setTravelTime(0);
////				route.setDistance(0.);
////				walkLeg.setRoute(route);
////				
////				legs.add(walkLeg);
////				
////			}
////			
////			Leg prtLeg = new LegImpl(PrtRequestCreator.MODE);
////			Path path = this.router.calcLeastCostPath(fromActivityRankLink.getFromNode(),
////					toActivityRankLink.getFromNode(), time, person, null);
////			GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(PrtRequestCreator.MODE,
////					fromActivityRankLink.getId(), toActivityRankLink.getId());
////			route.setStartLinkId(fromActivityRankLink.getId());
////			route.setEndLinkId(toActivityRankLink.getId());
////			route.setTravelTime((int)path.travelTime);
////			prtLeg.setRoute(route);
////			
////			legs.add(prtLeg);
////			
////			time += path.travelTime;
////			
////			if(endLink != toActivityRankLink){
////				
////				Leg walkLeg = new LegImpl(TransportMode.walk);
////				path = this.router.calcLeastCostPath(toActivityRankLink.getFromNode(),
////						endLink.getFromNode(), time, person, null);
////				route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
////						toActivityRankLink.getId(), endLink.getId());
////				route.setStartLinkId(toActivityRankLink.getId());
////				route.setEndLinkId(endLink.getId());
////				route.setTravelTime((int)path.travelTime);
////				walkLeg.setRoute(route);
////				
////				legs.add(walkLeg);
////				
////			} else{
////				
////				Leg walkLeg = new LegImpl(TransportMode.walk);
////				route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
////						toActivityRankLink.getId(), endLink.getId());
////				route.setTravelTime(0);
////				route.setDistance(0.);
////				walkLeg.setRoute(route);
////				
////				legs.add(walkLeg);
////				
////			}
////			
////		} else{
////			
////			Leg walkLeg = new LegImpl(TransportMode.walk);
////			GenericRouteImpl route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
////					startLink.getId(), endLink.getId());
////			route.setTravelTime(0);
////			route.setDistance(0.);
////			walkLeg.setRoute(route);
////			
////			legs.add(walkLeg);
////			
////		}
//		
//		return legs;
//		
//	}

}
