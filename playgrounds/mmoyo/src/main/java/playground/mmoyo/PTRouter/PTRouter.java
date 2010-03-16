package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;


public class PTRouter{
	private NetworkLayer logicNet;
	private LeastCostPathCalculator myDijkstra;
	private TravelCost ptTravelCost;
	private NodeImpl originNode;
	private NodeImpl destinationNode;
	public PTTravelTime ptTravelTime;   //> make private

	final String ACCESS_PREFIX = "WLO";
	final String EGRESS_PREFIX = "WLD";
	final String ORIGIN_ID = "W1";
	final String DESTINATION_ID = "W2";


	public PTRouter(final NetworkLayer logicNet) {
		init(logicNet);
	}

	public PTRouter(final TransitSchedule schedule, final TransitRouterConfig config){
		init(new LogicFactory(schedule).getLogicNet());
	}

	private final void init(final NetworkLayer logicNet){
		this.logicNet = logicNet;
		this.ptTravelTime =new PTTravelTime();
		this.ptTravelCost = new PTTravelCost(ptTravelTime);
		this.myDijkstra = new MyDijkstra(this.logicNet, ptTravelCost, ptTravelTime);

		Coord firstCoord = logicNet.getNodes().values().iterator().next().getCoord();
		this.originNode=  new Station(new IdImpl(ORIGIN_ID), firstCoord);		//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.destinationNode=  new Station(new IdImpl(DESTINATION_ID), firstCoord);	//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.logicNet.addNode(originNode);
		this.logicNet.addNode(destinationNode);
		
		System.out.println("logicNet.getNodes():" + logicNet.getNodes().size());
		System.out.println("logicNet.getLinks():" + logicNet.getLinks().size());
	}

	/**Calculates pt route between acts*/
	public List<Leg> calcRoute(final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {

		if ( fromAct.getLinkId()==null ||  toAct.getLinkId()== null){
			return calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime);
		}
		if (!fromAct.getLinkId().equals(toAct.getLinkId())){
			return calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime);
		}

		//if the two activities are located in the same link, create an undefined transport mode leg between them
		//System.out.println(fromAct.getType() + " " + toAct.getType() + " " + fromAct.getLinkId() + " " + toAct.getLinkId());
		GenericRouteImpl undefinedRoute = new GenericRouteImpl(fromAct.getLinkId(),toAct.getLinkId());
		undefinedRoute.setDistance(CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord()));
		LegImpl undefinedLeg = new LegImpl(TransportMode.undefined);
		undefinedLeg.setTravelTime(undefinedRoute.getDistance() * PTValues.AV_WALKING_SPEED);
		undefinedLeg.setRoute(undefinedRoute);
		List<Leg> legs =  new ArrayList<Leg>();
		legs.add(undefinedLeg);
		return legs;
	}

	/**invokes findPTpath and returns a set of legs of it*/
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		List<Leg> legList = null;
		Path path = findPTPath(fromCoord, toCoord, departureTime);
		if (path!= null){
			legList = new ArrayList<Leg>();
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
						if (lastLinkType==2){mode= TransportMode.pt;}else{mode= TransportMode.undefined;} // must be undefined
							legList.add(createLeg(mode, linkList, depTime, time));
						//}
						depTime=time;
						linkList = new ArrayList<PTLink>();
					}
					//add the egress link
					if (i == path.links.size()){
						time += ptLink.getWalkTime();
						linkList.add(ptLink);
						legList.add(createLeg(TransportMode.undefined, linkList, depTime, time));
					}
				}
				time += ptTravelTime.getLinkTravelTime(link, time);
				linkList.add(ptLink);
				lastLinkType = ptLink.getAliasType();
				i++;
			}
			//legList = logicToPlainTranslator.convertToPlainLeg(legList); //translates the listLeg into the plainNetwork
		}

		return legList;
	}

	public Path findPTPath(final Coord coord1, final Coord coord2, final double time){
		//double startTime = System.currentTimeMillis();
		originNode.setCoord(coord1);
		destinationNode.setCoord(coord2);

		Collection <NodeImpl> nearOriginStops = find2Stations (originNode);
		Collection <NodeImpl> nearDestinationStops = find2Stations (destinationNode);
		
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

	/**looks for a number of near stations around a node*/
	private Collection <NodeImpl> findnStations(final NodeImpl node){
		Collection <NodeImpl> nearStations;
		double walkRange = PTValues.FIRST_WALKRANGE;
		do{
			nearStations = logicNet.getNearestNodes(node.getCoord(), walkRange);  //walkRange
			walkRange += PTValues.WALKRANGE_EXT;
		} while (nearStations.size()<PTValues.INI_STATIONS_NUM);
		nearStations.remove(node);
		return nearStations;
	}

	//looks at least two stations, this is to match the multiNodeDijstra router
	private Collection <NodeImpl> find2Stations (final Node node){   
		Collection <NodeImpl> nearStations = logicNet.getNearestNodes(node.getCoord(), PTValues.FIRST_WALKRANGE);  //walkRange
		if (nearStations.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			Node nearestNode = this.logicNet.getNearestNode(node.getCoord());
			double distance = CoordUtils.calcDistance(node.getCoord(), nearestNode.getCoord());
			nearStations = logicNet.getNearestNodes(node.getCoord(), distance + PTValues.WALKRANGE_EXT);
		}
		nearStations.remove(node);
		return nearStations; 
	}
	
	
	public List <LinkImpl> createWalkingLinks(NodeImpl walkNode, Collection <NodeImpl> nearNodes, boolean to){
		List<LinkImpl> newWalkLinks = new ArrayList<LinkImpl>();
		int x=0;
		for (NodeImpl node : nearNodes){
			if (to){
				newWalkLinks.add(new PTLink(new IdImpl(ACCESS_PREFIX + x++), walkNode, node, logicNet, PTValues.ACCESS_STR));
			}else{
				newWalkLinks.add(new PTLink(new IdImpl(EGRESS_PREFIX + x++), node, walkNode, logicNet, PTValues.EGRESS_STR));
			}
		}
		return newWalkLinks;
	}

	public void removeWalkLinks(Collection<LinkImpl> WalkingLinkList){
		for (LinkImpl link : WalkingLinkList){
			logicNet.removeLink(link);
		}
	}

	private LegImpl createLeg(TransportMode mode, final List<PTLink> routeLinks, final double depTime, final double arrivTime){
		PTLink firstLink = routeLinks.get(0);
		//double travTime= arrivTime - depTime;
		//List<Node> routeNodeList = new ArrayList<Node>();
		//if (routeLinks.size()>0) routeNodeList.add(routeLinks.get(0).getFromNode());

		LegImpl leg = new LegImpl(mode);

		if (firstLink.getAliasType() ==2){ //Standard link
			double travTime= 0;
			double distance=0;
			for(PTLink ptLink : routeLinks) {
				distance += ptLink.getLength();
				travTime+= (ptLink.getTravelTime());
				//routeNodeList.add(link.getToNode());
			}

			TransitLine line = firstLink.getTransitLine();
			TransitRoute route = firstLink.getTransitRoute();
			TransitStopFacility accessStop = ((Station)firstLink.getFromNode()).getTransitStopFacility();
			TransitStopFacility egressStop = ((Station)routeLinks.get(routeLinks.size()-1).getToNode()).getTransitStopFacility();
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			leg.setRoute(ptRoute);
			leg.setTravelTime(travTime);
			ptRoute.setDistance(distance);
			//ptRoute.setTravelTime(travTime);
		}else{        						//walking links : access, transfer, detTransfer, egress
			leg = new LegImpl(TransportMode.undefined);   // it must be mode undefined
			leg.setTravelTime(firstLink.getWalkTime());
			GenericRouteImpl undefinedRoute;
			if (firstLink.getAliasType() ==3 || firstLink.getAliasType() ==4){  //transfers
				undefinedRoute = new GenericRouteImpl(((Station)firstLink.getFromNode()).getTransitStopFacility().getLinkId(), ((Station)firstLink.getToNode()).getTransitStopFacility().getLinkId());
			}else{
				undefinedRoute = new GenericRouteImpl(firstLink.getId(),firstLink.getId());
			}
			undefinedRoute.setDistance(firstLink.getLength());
			leg.setRoute(undefinedRoute);
		}

		//leg.setDepartureTime(depTime);
		//leg.setArrivalTime(arrivTime);


		return leg;
	}

	private LegImpl createLeg2(boolean isStandardLeg, TransportMode mode, TransitLine line, TransitRoute route, TransitStopFacility accessStop , TransitStopFacility egressStop,  final List<PTLink> routeLinks){
		double distance=0;
		double travelTime=0;

		LegImpl leg= null;
		if (isStandardLeg){ //Standard link
			for(PTLink ptLink : routeLinks) {
				distance += ptLink.getLength();
				travelTime += ptLink.getTravelTime();
			}
			ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
			//ptRoute.setDistance(distance);
			//ptRoute.setTravelTime(travelTime);
			leg = new LegImpl(mode);
			leg.setRoute(ptRoute);

		}else{        						//walking links : access, transfer, detTransfer, egress
			PTLink firstLink = routeLinks.get(0);
			leg = new LegImpl(TransportMode.walk);
			leg.setTravelTime(firstLink.getWalkTime());

			if (firstLink.getAliasType() ==3 || firstLink.getAliasType() ==4){  //transfers
				leg.setRoute(new GenericRouteImpl(((Station)firstLink.getFromNode()).getTransitStopFacility().getLinkId(), ((Station)firstLink.getToNode()).getTransitStopFacility().getLinkId()));
			}
		}
		//leg.setDepartureTime(depTime);??
		//leg.setArrivalTime(arrivTime);??
		return leg;
	}

	public void printRoute(Path path){
		if (path!=null){
			System.out.print("\nLinks: ");
			Id transitRouteId = new IdImpl("");
			for (Node node : path.nodes){
				Station ptNode= (Station)node;
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


}


