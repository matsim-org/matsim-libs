package playground.mmoyo.ptrouterFromMarcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.routes.ExperimentalTransitRoute;
import playground.mmoyo.PTRouter.MyDijkstra;
import playground.mmoyo.PTRouter.PTLink;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.PTRouter.PTTravelCost;
import playground.mmoyo.PTRouter.PTTravelTime;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;

/**experimental class to integrate router in simulation*/
public class MMoyoTransitRouter {
	private final TransitSchedule schedule;
	private final NetworkLayer logicNetwork;
	private LogicIntoPlainTranslator logicToPlainTranslator;
	private MyDijkstra myDijkstra;
	private LogicFactory logicFactory;
	private PTNode origin;
	private PTNode destination;
	private final double firstWalkRange; 
	private PTTravelTime ptTravelTime = new PTTravelTime();
	private final static Logger log = Logger.getLogger(MMoyoTransitRouter.class);
	
	public MMoyoTransitRouter(final TransitSchedule schedule) {
		this(schedule, new MMoyoTransitRouterConfig());
	}

	public MMoyoTransitRouter(final TransitSchedule schedule, final MMoyoTransitRouterConfig config){
		this.schedule = schedule;

		PTValues ptValues = new PTValues();
		this.firstWalkRange = ptValues.FIRST_WALKRANGE;
			
		this.logicFactory = new LogicFactory(this.schedule);
		this.logicNetwork = logicFactory.getLogicNet();
		this.logicToPlainTranslator = logicFactory.getLogicToPlainTranslator();
		
		PTTravelTime ptTravelTime = new PTTravelTime();
		this.myDijkstra = new MyDijkstra(logicNetwork, new PTTravelCost(ptTravelTime), ptTravelTime);	
		this.origin=  new PTNode(new IdImpl("W1"), null);		//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.destination=  new PTNode(new IdImpl("W2"), null);	//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		logicNetwork.addNode(origin);
		logicNetwork.addNode(destination);
	}
	
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		origin.setCoord(fromCoord);
		destination.setCoord(toCoord);
		
		Collection <NodeImpl> nearOriginStops = find2Stations (fromCoord);
		Collection <NodeImpl> nearDestinationStops = find2Stations(toCoord);
		
		List <LinkImpl> walkLinksFromOrigin = createWalkingLinks(origin, nearOriginStops, true);
		List <LinkImpl> walkLinksToDestination = createWalkingLinks(destination, nearDestinationStops, false);
			
		Path path = myDijkstra.calcLeastCostPath(origin, destination, departureTime); 
		
		if (path==null) {
			log.warn("path not found from " + fromCoord + " to " + toCoord +  " departure Time:"  + departureTime);
		}else{
			System.out.println("path found from " + fromCoord + " to " + toCoord +  " departure Time:"  + departureTime);
		}
		
		removeWalkLinks(walkLinksFromOrigin);
		removeWalkLinks(walkLinksToDestination);
		if (path==null){
			return null;
		}
		path.nodes.remove(origin);
		path.nodes.remove(destination);

		//////////////////////split the path in legs
		List<Leg> legList = new ArrayList<Leg>();
		if (path!= null){
			List <PTLink> linkList = new ArrayList<PTLink>();
			double depTime=departureTime;
			double time = departureTime;
			
			int i=1;
			byte lastLinkType= 0;
			for (Link link: path.links){
				PTLink ptLink = (PTLink)link;
				if (i>1){
					if (ptLink.getAliasType() != lastLinkType){
						//if (!lastLinkType.equals("Transfer")){    //transfers will not be described in legs ??<-
							LegImpl newLeg = createLeg(selectMode(lastLinkType), linkList, depTime, time);  
							legList.add(newLeg);
						//}
						depTime=time;
						linkList = new ArrayList<PTLink>();
					}

					//add the egress link
					if (i == path.links.size()){
						time = time + ptLink.getWalkTime();
						linkList.add(ptLink);
						LegImpl newLeg = createLeg(TransportMode.walk, linkList, depTime, time);
						legList.add(newLeg);
					}
					
				}
				time = time + ptTravelTime.getLinkTravelTime(link, time);;
				linkList.add(ptLink);
				lastLinkType = ptLink.getAliasType();
				i++;
			}
			//legList = logicToPlainTranslator.convertToPlainLeg(legList); //translates the listLeg into the plainNetwork
		}
		
		/*
		for (Leg leg:legList){
			System.out.println(leg.toString());
		}
		*/
		
		return legList;
	}	
	
	private Collection <NodeImpl> find2Stations(final Coord coord){
		
		Collection<NodeImpl> stations = this.logicNetwork.getNearestNodes(coord, this.firstWalkRange);
		if (stations.size() < 2) {
			NodeImpl nearestStation = this.logicNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcDistance(coord, nearestStation.getCoord());
			stations = this.logicNetwork.getNearestNodes(coord, distance + 200.0);
		}
	
		/* original
		Collection <NodeImpl> stations;
		double extWalkRange = this.firstWalkRange;
		do{
			stations = logicNetwork.getNearestNodes(coord, extWalkRange);
			extWalkRange +=  300;
		} while (stations.size()<2);
		*/
		return stations;
	}
	
	public List <LinkImpl> createWalkingLinks(NodeImpl walkNode, Collection <NodeImpl> nearNodes, boolean isAccess){
		nearNodes.remove(walkNode);
		List<LinkImpl> newWalkLinks = new ArrayList<LinkImpl>();
		
		if ((isAccess && nearNodes.contains(this.logicNetwork.getNode("W2"))) ||   //do not create a link (w1)-->(w2)
		   (!isAccess && nearNodes.contains(this.logicNetwork.getNode("W1"))))
			return newWalkLinks;
		
		Id idLink;
		NodeImpl fromNode;
		NodeImpl toNode;
		int x=0;
		String type;
		for (NodeImpl node : nearNodes){
			if (isAccess){
				fromNode= walkNode;
				toNode= node;
				idLink = new IdImpl("WLO" + x++);
				type = "Access";
			}else{
				fromNode= node;
				toNode=  walkNode;
				idLink = new IdImpl("WLD" + x++);
				type = "Egress";
			}
			
			//LinkImpl link= logicNetwork.createAndAddLink(idLink, fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()) , 1, 1, 1, "0", type);
			PTLink link = new PTLink(idLink, fromNode, toNode, logicNetwork, type);
			newWalkLinks.add(link);
		}
		return newWalkLinks;
	}
	
	public void removeWalkLinks(Collection<LinkImpl> WalkingLinkList){
		for (LinkImpl link : WalkingLinkList){
			logicNetwork.removeLink(link);
		}
	}
	
	private TransportMode selectMode(byte aliasType){
		TransportMode mode = null;
		if (aliasType==2){
			mode= TransportMode.pt;}//standard
		else{
			mode= TransportMode.walk;}		   	
		return mode;
	}
		
	private LegImpl createLeg(TransportMode mode, final List<PTLink> routeLinks, final double depTime, final double arrivTime){
		
		PTLink firstLink = routeLinks.get(0);
		double travTime= arrivTime - depTime;
		double distance=0;
		List<Node> routeNodeList = new ArrayList<Node>();
		if (routeLinks.size()>0) routeNodeList.add(routeLinks.get(0).getFromNode());

		for(Link link : routeLinks) {
			distance += link.getLength();
			routeNodeList.add(link.getToNode());
		} 
		LegImpl leg = new LegImpl(mode);
		//GenericRouteImpl ptRoute;
		
		if (firstLink.getAliasType() ==2){ //Standard link
			TransitLine line = firstLink.getTransitLine();
			TransitRoute route = firstLink.getTransitRoute();
			TransitStopFacility accessStop = ((PTNode)firstLink.getFromNode()).getTransitStopFacility();
			TransitStopFacility egressStop = ((PTNode)routeLinks.get(routeLinks.size()-1).getToNode()).getTransitStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			ptRoute.setDistance(distance);
			ptRoute.setTravelTime(travTime);
		}else{        						//walking links : access, transfer, dettransfer, agress
			leg = new LegImpl(TransportMode.walk);
			leg.setTravelTime(firstLink.getWalkTime());
			
			if (firstLink.getAliasType() ==3 || firstLink.getAliasType() ==4){  //transfers
				GenericRouteImpl walkRoute = new GenericRouteImpl(((PTNode)firstLink.getFromNode()).getTransitStopFacility().getLink(), ((PTNode)firstLink.getToNode()).getTransitStopFacility().getLink());
				leg.setRoute(walkRoute);
			}
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrivTime);
		
		return leg;
	}
		
	/* returns the plainNet to visualize but.... class expected*/
	/*
	public Network getTransitRouterNetwork() {
		return logicFactory.getPlainNet();
	}
	*/

}
