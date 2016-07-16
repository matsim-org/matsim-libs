package playground.toronto.sotr.routernetwork2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class StopGroupFacility {
	
	private final Node networkNode;
	private HashSet<TransitStopFacility> stopFacilities;
	
	private HashMap<Id, HashMap<Id, TransitStopFacility>> lineStops;
	
	public StopGroupFacility(final Node node){
		this.networkNode = node;
		
		this.stopFacilities = new HashSet<TransitStopFacility>();
		this.lineStops = new HashMap<Id, HashMap<Id,TransitStopFacility>>();
	}
	
	public static Map<TransitRouteStop, StopGroupFacility> buildFacilities(TransitSchedule schedule, Network network){
		HashMap<TransitRouteStop, StopGroupFacility> result = new HashMap<TransitRouteStop, StopGroupFacility>();
		HashMap<Node, StopGroupFacility> nodeGroups = new HashMap<Node, StopGroupFacility>();
		
		//TODO
		
		for (TransitStopFacility stop : schedule.getFacilities().values()){
			Link l = network.getLinks().get(stop.getLinkId());
			StopGroupFacility group;
			if (nodeGroups.containsKey(l.getToNode())){
				group = nodeGroups.get(l.getToNode());
			}else{
				group = new StopGroupFacility(l.getToNode());
				nodeGroups.put(group.networkNode, group);
			}
			group.stopFacilities.add(stop);
		}
		
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					
					
					
				}
			}
		}
		
		return result;
	}
}
