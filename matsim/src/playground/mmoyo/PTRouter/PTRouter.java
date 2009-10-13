package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.router.TransitRouterConfig;
import playground.marcel.pt.routes.ExperimentalTransitRoute;
import playground.mmoyo.TransitSimulation.LogicFactory;


public class PTRouter{
	private NetworkLayer logicNet;
	private LeastCostPathCalculator myDijkstra;
	private TravelCost ptTravelCost;
	public PTTravelTime ptTravelTime;   //> make private 
	NodeImpl originNode;
	NodeImpl destinationNode;
	PTValues ptValues = new PTValues();

	public PTRouter(final NetworkLayer logicNet) {
		InitObjects(logicNet);
	}
	
	public PTRouter(final TransitSchedule schedule, final TransitRouterConfig config){
		LogicFactory logicFactory = new LogicFactory(schedule);
		 //this.logicToPlainTranslator = logicFactory.getLogicToPlainTranslator();
		InitObjects(logicFactory.getLogicNet());
	}
	
	private void InitObjects(final NetworkLayer logicNet){
		this.logicNet = logicNet;
		this.ptTravelTime =new PTTravelTime();
		this.ptTravelCost = new PTTravelCost(ptTravelTime);
		this.myDijkstra = new MyDijkstra(logicNet, ptTravelCost, ptTravelTime);	
		this.originNode=  new PTNode(new IdImpl("W1"), null);		//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.destinationNode=  new PTNode(new IdImpl("W2"), null);	//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.logicNet.addNode(originNode);
		this.logicNet.addNode(destinationNode);	
	}
	
	/**invokes findPTpath and returns a set of legs of it*/
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		Path path = findPTPath(fromCoord, toCoord, departureTime);
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
						TransportMode mode;	
						if (lastLinkType==2){mode= TransportMode.pt;}else{mode= TransportMode.walk;}		   	
						
						LegImpl newLeg = createLeg(mode, linkList, depTime, time);  
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
		
		return legList;
	}	
	
	public Path findPTPath(final Coord coord1, final Coord coord2, final double time){
		originNode.setCoord(coord1);
		destinationNode.setCoord(coord2);
		
		Collection <NodeImpl> nearOriginStops = findnStations (originNode);
		Collection <NodeImpl> nearDestinationStops = findnStations (destinationNode);
		
		nearOriginStops.remove(destinationNode);
		nearDestinationStops.remove(originNode);
		
		List <LinkImpl> walkLinksFromOrigin = createWalkingLinks(originNode, nearOriginStops, true);
		List <LinkImpl> walkLinksToDestination = createWalkingLinks(destinationNode, nearDestinationStops, false);
			
		Path path = myDijkstra.calcLeastCostPath(originNode, destinationNode, time); 
			
		removeWalkLinks(walkLinksFromOrigin);
		removeWalkLinks(walkLinksToDestination);
		if (path!=null){
			path.nodes.remove(originNode);
			path.nodes.remove(destinationNode);
		}
		//logicNet.removeNode(origin);
		//logicNet.removeNode(destination);
		return path;
	}

	/** looks for  a number of near stations around a node*/
	private Collection <NodeImpl> findnStations(final NodeImpl node){
		Collection <NodeImpl> nearStations;
		double walkRange = ptValues.FIRST_WALKRANGE;
		Coord coord = node.getCoord();
		do{
			nearStations = logicNet.getNearestNodes(coord, walkRange);
			walkRange += 300;
		} while (nearStations.size()<ptValues.INI_STATIONS_NUM);
		nearStations.remove(node);
		return nearStations;
	}

	/**Creates a temporary origin or destination node avoids the method net.createNode because it is not necessary to rebuild the quadtree	 */
	public NodeImpl createWalkingNode(Id id, Coord coord) {
		NodeImpl node = new PTNode(id, coord);
		//logicNet.getNodes().put(id, node);
		logicNet.addNode(node);
		return node;
	}
	
	public List <LinkImpl> createWalkingLinks(NodeImpl walkNode, Collection <NodeImpl> nearNodes, boolean to){
		List<LinkImpl> newWalkLinks = new ArrayList<LinkImpl>();
		Id idLink;
		NodeImpl fromNode;
		NodeImpl toNode;
		int x=0;
		String type;
		for (NodeImpl node : nearNodes){
			if (to){
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
			
			PTLink link = new PTLink(idLink, fromNode, toNode, logicNet, type); 
			//PTLink link= logicNet.createAndAddLink(idLink, fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()) , 1, 1, 1, "0", type);
			newWalkLinks.add(link);
		}
		return newWalkLinks;
	}

	public void removeWalkLinks(Collection<LinkImpl> WalkingLinkList){
		//->use link factory
		for (LinkImpl link : WalkingLinkList){
			logicNet.removeLink(link);
		}
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
		
		if (firstLink.getAliasType() ==2){ //Standard link
			TransitLine line = firstLink.getTransitLine();
			TransitRoute route = firstLink.getTransitRoute();
			TransitStopFacility accessStop = ((PTNode)firstLink.getFromNode()).getTransitStopFacility();
			TransitStopFacility egressStop = ((PTNode)routeLinks.get(routeLinks.size()-1).getToNode()).getTransitStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			ptRoute.setDistance(distance);
			ptRoute.setTravelTime(travTime);
		}else{        						//walking links : access, transfer, detTransfer, egress
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

	public void printRoute(Path path){
		if (path!=null){
			System.out.print("\nLinks: ");
			Id transitRouteId = new IdImpl("");
			for (Node node : path.nodes){
				PTNode ptNode= (PTNode)node;
				if(ptNode.getTransitRoute().getId()==transitRouteId){
					System.out.print(ptNode.getId().toString() + " ");
				}else{
					System.out.println("\n" + ptNode.getTransitRoute().getId().toString());
					System.out.print(ptNode.getId().toString() + " ");
				}
				transitRouteId= ptNode.getTransitRoute().getId();	
			}
			System.out.println("\nTravel cost of route=" + path.travelCost + "  time of route:" + path.travelTime);
		}else{
			System.out.println("The route is null");
		}
	}	
	
	
	///////////////////////////////// Methods for Counter/////////////////
	double timeCoeficient;
	double distanceCoeficient;
	double transferPenalty;
	
	public PTRouter(final NetworkLayer logicNet, final double timeCoeficient, final double distanceCoeficient, final double transferPenalty) {
		this.logicNet = logicNet;
		this.ptTravelTime =new PTTravelTime();
		this.ptTravelCost = new PTTravelCost(ptTravelTime, timeCoeficient, distanceCoeficient, transferPenalty);
		this.timeCoeficient = timeCoeficient;
		this.distanceCoeficient = distanceCoeficient; 
		this.transferPenalty = transferPenalty;
		this.myDijkstra = new MyDijkstra(logicNet, ptTravelCost, ptTravelTime);	
		originNode= createWalkingNode(new IdImpl("W1"), null);   //this is faster than network.createNode but uses PTNode
		destinationNode= createWalkingNode(new IdImpl("W2"), null);
	}
	
	public double getTimeCoeficient(){
		return this.timeCoeficient;		
	}
			 
	public double getDistanceCoeficient(){
		return this.distanceCoeficient;
	}
	
	public double getTransferPenalty(){
		return this.transferPenalty;
	}
	////////////////////////////////////////////////////////////////////////////////////////
	
}


