package playground.dhosse.prt.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.router.TransitRouter;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;

public class PrtNetworkLegRouter implements TransitRouter {

	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final LeastCostPathCalculator router;
	
	public PrtNetworkLegRouter(final Network network, final LeastCostPathCalculator router, final ModeRouteFactory routeFactory){
		this.network = network;
		this.routeFactory = routeFactory;
		this.router = router;
	}

	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord,
			double departureTime, Person person) {
		
		Node fromNode = PrtData.getNearestRank(fromCoord).getLink().getFromNode();
		Node toNode = PrtData.getNearestRank(toCoord).getLink().getFromNode();
		
		Path p = this.router.calcLeastCostPath(fromNode, toNode, departureTime, person, null);
		
		if(p == null) return null;
		
		return convertPathToLegList(departureTime, p, fromCoord, toCoord, person);
		
	}
	
	private List<Leg> convertPathToLegList(double departureTime,
			Path p, Coord fromCoord, Coord toCoord, Person person){
		
		double time = departureTime;
		
		List<Leg> legs = new ArrayList<Leg>();
		
		Link startLink = null;
		Link endLink = null;
		
		if(((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getLinkId() == null || 
				((ActivityImpl)person.getSelectedPlan().getPlanElements().get(2)).getLinkId() == null){
			startLink = ((NetworkImpl)this.network).getNearestLinkExactly(fromCoord);
			endLink = ((NetworkImpl)this.network).getNearestLinkExactly(toCoord);
			
			((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).setLinkId(startLink.getId());
			((ActivityImpl)person.getSelectedPlan().getPlanElements().get(2)).setLinkId(endLink.getId());
		} else{
			
			startLink = this.network.getLinks().get(((Activity)person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
			endLink = this.network.getLinks().get(((Activity)person.getSelectedPlan().getPlanElements().get(2)).getLinkId());
			
		}

		Link fromActivityRankLink = p.links.get(0);
		Link toActivityRankLink = p.links.get(p.links.size()-1);
		
		if(person.getId().toString().equals("3025")){
			System.out.println("");
		}
		
		if(endLink != startLink && fromActivityRankLink != toActivityRankLink){
			
			if(fromActivityRankLink != startLink){
				
				Leg walkLeg = new LegImpl(TransportMode.walk);
				Path path = this.router.calcLeastCostPath(startLink.getFromNode(),
						fromActivityRankLink.getFromNode(), time, person, null);
				GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
						startLink.getId(), fromActivityRankLink.getId());
				route.setStartLinkId(startLink.getId());
				route.setEndLinkId(fromActivityRankLink.getId());
				route.setTravelTime((int)path.travelTime);
				walkLeg.setRoute(route);
				
				legs.add(walkLeg);
				
				time += path.travelTime;
				
				
			} else{
				
				Leg walkLeg = new LegImpl(TransportMode.walk);
				GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(TransportMode.walk,
						startLink.getId(), fromActivityRankLink.getId());
				route.setTravelTime(0);
				route.setDistance(0.);
				walkLeg.setRoute(route);
				
				legs.add(walkLeg);
				
			}
			
			Leg prtLeg = new LegImpl(PrtRequestCreator.MODE);
			Path path = this.router.calcLeastCostPath(fromActivityRankLink.getFromNode(),
					toActivityRankLink.getFromNode(), time, person, null);
			GenericRouteImpl route = (GenericRouteImpl) this.routeFactory.createRoute(PrtRequestCreator.MODE,
					fromActivityRankLink.getId(), toActivityRankLink.getId());
			route.setStartLinkId(fromActivityRankLink.getId());
			route.setEndLinkId(toActivityRankLink.getId());
			route.setTravelTime((int)path.travelTime);
			prtLeg.setRoute(route);
			
			legs.add(prtLeg);
			
			time += path.travelTime;
			
			if(endLink != toActivityRankLink){
				
				Leg walkLeg = new LegImpl(TransportMode.walk);
				path = this.router.calcLeastCostPath(toActivityRankLink.getFromNode(),
						endLink.getFromNode(), time, person, null);
				route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
						toActivityRankLink.getId(), endLink.getId());
				route.setStartLinkId(toActivityRankLink.getId());
				route.setEndLinkId(endLink.getId());
				route.setTravelTime((int)path.travelTime);
				walkLeg.setRoute(route);
				
				legs.add(walkLeg);
				
			} else{
				
				Leg walkLeg = new LegImpl(TransportMode.walk);
				route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
						toActivityRankLink.getId(), endLink.getId());
				route.setTravelTime(0);
				route.setDistance(0.);
				walkLeg.setRoute(route);
				
				legs.add(walkLeg);
				
			}
			
		} else{
			
			Leg walkLeg = new LegImpl(TransportMode.walk);
			GenericRouteImpl route = (GenericRouteImpl)this.routeFactory.createRoute(TransportMode.walk,
					startLink.getId(), endLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.);
			walkLeg.setRoute(route);
			
			legs.add(walkLeg);
			
		}
		
		return legs;
		
	}
	
}
