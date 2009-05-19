package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.api.basic.v01.TransportMode;

import playground.mmoyo.PTCase2.PTRouter2;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.Pedestrian.Walk;

public class TransitRouteFinder {
	private NetworkLayer net;
	private PTTimeTable2 ptTimeTable;
	private PTRouter2 ptRouter;
	private static Walk walk;
	
	public TransitRouteFinder(PTRouter2 ptRouter ){
		//this.net = net;
		//this.ptTimeTable = ptTimeTable; 
		this.ptRouter = ptRouter; 
	}

	public List<Leg> calculateRoute (Activity fromAct, Activity toAct, Person person ){
		List<Leg> legList = new ArrayList<Leg>();

		double distToWalk = walk.distToWalk(person.getAge());
		Path path = ptRouter.findRoute(fromAct.getCoord(), toAct.getCoord(), fromAct.getEndTime(), distToWalk);

		String linkType;
		String lastLinkType= null;
		boolean first = true;

		List <Link> linkList = new ArrayList<Link>();
		double depTime=0;
		double travTime=0;

		for (Link link: path.links){
			linkType = link.getType();
			
			if (!first){
				if (linkType.equals(lastLinkType)){
					travTime= travTime + 11111;  // -->calculate traveltime of link. maybe read as property 
				}else{
					TransportMode mode = selectMode(lastLinkType);
					createLeg(mode, linkList, depTime, travTime);  
					
					depTime=depTime+ travTime;
					linkList = new ArrayList<Link>();
				}
			}else{
				first= false;
			}
			linkList.add(link);
			lastLinkType = linkType;
		}
		return legList;
	}
	
	private TransportMode selectMode(String linkType){
		TransportMode mode = null;
		if (linkType.equals("Walk") || linkType.equals("DetTransfer")){ 
			mode=  TransportMode.walk;
		}else{
			mode=  TransportMode.pt;
		}
		return mode;
	}
	

	private Leg createLeg(TransportMode mode, final List<Link> routeLinks, final double depTime, final double travTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null);  
		legRoute.setLinks(null, routeLinks, null);
		legRoute.setTravelTime(travTime);

		double distance=0;
		for(Link link : routeLinks) {
			distance= distance + link.getLength();
		} 
		legRoute.setDistance(distance);
		
		Leg leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return leg;
	}
	
	
}
