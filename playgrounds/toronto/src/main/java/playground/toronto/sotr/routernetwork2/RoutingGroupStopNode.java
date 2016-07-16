package playground.toronto.sotr.routernetwork2;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RoutingGroupStopNode extends AbstractRoutingNode {
	
	private Coord coord;
	private HashMap<TransitLine, HashMap<TransitRoute, TransitStopFacility>> stops;
	
	public RoutingGroupStopNode(Coord coord){
		super();
		this.coord = coord;
		this.stops = new HashMap<TransitLine, HashMap<TransitRoute,TransitStopFacility>>();
	}
	
	@Override
	public TransitStopFacility getStopFacility(TransitLine line,
			TransitRoute route) {
		return (stops.get(line).get(route));
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}
	
	public static Map<TransitStopFacility, RoutingGroupStopNode> createStopGroupsFromSchedule(TransitSchedule schedule, Network network){
		return null;
	}
	
}
