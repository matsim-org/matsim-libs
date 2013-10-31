package playground.toronto.sotr.routernetwork2;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RoutingNoStopNode extends AbstractRoutingNode{
	
	private Coord coord;
	
	public RoutingNoStopNode(Coord coord){
		super();
		this.coord = coord;
	}
	
	@Override
	public TransitStopFacility getStopFacility(TransitLine line,
			TransitRoute route) {
		return null;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

}
