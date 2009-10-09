package playground.mmoyo.iterations;

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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.marcel.pt.router.TransitRouterConfig;
import playground.mmoyo.PTRouter.MyDijkstra;
import playground.mmoyo.PTRouter.PTTravelCost;
import playground.mmoyo.PTRouter.PTTravelTime;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.*;

/**experimental class to integrate router in simulation*/
public class TransitRouter {
	private final TransitSchedule schedule;
	private final NetworkLayer logicNetwork;
	private LogicIntoPlainTranslator logicToPlainTranslator;
	private MyDijkstra myDijkstra;
	private LogicFactory logicFactory;
	private NodeImpl origin;
	private NodeImpl destination;
	private final double firstWalkRange; 
	private PTTravelTime ptTravelTime = new PTTravelTime();
	
	public TransitRouter(final TransitSchedule schedule) {
		this(schedule, new TransitRouterConfig());
	}

	public TransitRouter(final TransitSchedule schedule, final TransitRouterConfig config) {
		this.schedule = schedule;

		PTValues ptValues = new PTValues();
		this.firstWalkRange = ptValues.firstWalkRange();
			
		this.logicFactory = new LogicFactory(this.schedule);
		this.logicNetwork = logicFactory.getLogicNet();
		this.logicToPlainTranslator = logicFactory.getLogicToPlainTranslator();
		
		PTTravelTime ptTravelTime = new PTTravelTime();
		this.myDijkstra = new MyDijkstra(logicNetwork, new PTTravelCost(ptTravelTime), ptTravelTime);	
		this.origin=  new NodeImpl(new IdImpl("W1"));		//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		this.destination=  new NodeImpl(new IdImpl("W2"));	//transitNetwork.getFactory().createNode(new IdImpl("W1"), null);
		logicNetwork.addNode(origin);
		logicNetwork.addNode(destination);
	}
	
	private Collection <NodeImpl> find2Stations(final Coord coord){
		Collection <NodeImpl> stations;
		double extWalkRange = this.firstWalkRange;
		do{
			stations = logicNetwork.getNearestNodes(coord, extWalkRange);
			extWalkRange +=  300;
		} while (stations.size()<2);
		return stations;
	}
	
	public List <LinkImpl> createWalkingLinks(NodeImpl walkNode, Collection <NodeImpl> nearNodes, boolean to){
		//->move to link factory
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
			
			LinkImpl link= logicNetwork.createAndAddLink(idLink, fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()) , 1, 1, 1, "0", type);
			newWalkLinks.add(link);
		}
		return newWalkLinks;
	}
	
	public void removeWalkLinks(Collection<LinkImpl> WalkingLinkList){
		for (LinkImpl link : WalkingLinkList){
			logicNetwork.removeLink(link);
		}
	}
	
	public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		origin.setCoord(fromCoord);
		destination.setCoord(toCoord);
		
		Collection <NodeImpl> nearOriginStops = find2Stations (fromCoord);
		Collection <NodeImpl> nearDestinationStops = find2Stations(toCoord);
		
		List <LinkImpl> walkLinksFromOrigin = createWalkingLinks(origin, nearOriginStops, true);
		List <LinkImpl> walkLinksToDestination = createWalkingLinks(destination, nearDestinationStops, false);
			
		Path path = myDijkstra.calcLeastCostPath(origin, destination, departureTime); 
			
		removeWalkLinks(walkLinksFromOrigin);
		removeWalkLinks(walkLinksToDestination);
		if (path==null){
			return null;
		}
		path.nodes.remove(origin);
		path.nodes.remove(destination);

		///////split the path in legs
		List<Leg> legList = new ArrayList<Leg>();
		List <Link> linkList = new ArrayList<Link>();
		double legDepTime=departureTime;
		double travTime= departureTime;
			
		int i=1;
		String linkType;
		String lastLinkType= null;
		for (Link link: path.links){
			linkType = ((LinkImpl)link).getType();
			if (i>1){
				if (!linkType.equals(lastLinkType)){
					if (!lastLinkType.equals("Transfer")){    //transfers will not be described in legs
						LegImpl newLeg = createLeg(selectMode(lastLinkType), linkList, legDepTime, travTime);  
						legList.add(newLeg);
					}
					legDepTime=travTime;
					linkList = new ArrayList<Link>();
				}
				if (i == path.links.size()){
					travTime = travTime + ptTravelTime.getLinkTravelTime(link, travTime);
					LegImpl newLeg = createLeg(selectMode(linkType), linkList, legDepTime, travTime);
					legList.add(newLeg);
				}
			}
			travTime = travTime + ptTravelTime.getLinkTravelTime(link, travTime);
			linkList.add(link);
			lastLinkType = linkType;
			i++;
		}
		legList = logicToPlainTranslator.convertToPlainLeg(legList); //translates the listLeg into the plainNetwork
		return legList;
		
	}	
		
	private TransportMode selectMode(final String linkType){
		TransportMode mode = null;
		if (linkType.equals("Standard")){ 
			mode=  TransportMode.pt;
		}else{
			mode=  TransportMode.walk;
		}
		return mode;
	}
		
	private LegImpl createLeg(TransportMode mode, final List<Link> routeLinks, final double depTime, final double arrivTime){
		double travTime= arrivTime - depTime;  
		List<Node> routeNodeList = new ArrayList<Node>();
		
		double distance=0;
		if (routeLinks.size()>0) routeNodeList.add(routeLinks.get(0).getFromNode());
		for(Link link : routeLinks) {
			distance= distance + link.getLength();
			routeNodeList.add(link.getToNode());
		} 
		
		NetworkRouteWRefs legRoute = new LinkNetworkRouteImpl(null, null);
		legRoute.setDistance(distance);
		legRoute.setLinks(null, routeLinks, null);
		legRoute.setTravelTime(travTime);
		legRoute.setNodes(null, routeNodeList, null);

		LegImpl leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrivTime);

		return leg;
	}
		
	/* returns the plainnet to visualize but.... clases expect*/
	public Network getTransitRouterNetwork() {
		return logicFactory.getPlainNet();
	}

}
