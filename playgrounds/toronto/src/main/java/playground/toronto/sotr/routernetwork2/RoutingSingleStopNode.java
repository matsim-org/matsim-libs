package playground.toronto.sotr.routernetwork2;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RoutingSingleStopNode extends AbstractRoutingNode {
	
	private TransitStopFacility stop;
	
	public RoutingSingleStopNode(TransitStopFacility stop){
		super();
		this.stop = stop;
	}
	
	@Override
	public TransitStopFacility getStopFacility(TransitLine line,
			TransitRoute route) {
		return this.stop;
	}

	@Override
	public Coord getCoord() {
		return this.stop.getCoord();
	}

}
