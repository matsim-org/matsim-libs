package playground.mmoyo.PTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NodeImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
/**
 * Node with necessary data for the routing network
 * These nodes are installed in a different layer in independent paths according to each route
 */
public class Station extends NodeImpl {
	private TransitRoute transitRoute ;
	private TransitLine transitLine;
	private TransitRouteStop transitRouteStop;
	private double[]arrDep;  
	private Link inStandardLink;
	private Node plainNode;
	private boolean isFirstStation;
	private boolean isLastStation; 
	
	public Station(final Id id, final Coord coord) {
		super(id, coord, null);
	}

	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	public void setTransitRoute(TransitRoute transitRoute) {
		this.transitRoute = transitRoute;
	}

	public TransitRouteStop getTransitRouteStop() {
		return transitRouteStop;
	}

	public void setTransitRouteStop(TransitRouteStop transitRouteStop) {
		this.transitRouteStop = transitRouteStop;
	}

	public Link getInStandardLink() {
		return inStandardLink;
	}

	public void setInStandardLink(Link inStandardLink) {
		this.inStandardLink = inStandardLink;
	}

	public Node getPlainNode() {
		return plainNode;
	}

	public void setPlainNode(Node plainNode) {
		this.plainNode = plainNode;
	}

	public TransitStopFacility getTransitStopFacility(){
		return this.transitRouteStop.getStopFacility();
	}

	public double[] getArrDep() {
		return arrDep;
	}

	public void setArrDep(double[] arrDep) {
		this.arrDep = arrDep;
	}
			
	public TransitLine getTransitLine() {
		return transitLine;
	}

	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}

	public boolean isFirstStation() {
		return isFirstStation;
	}

	public void setFirstStation(boolean isFirstStation) {
		this.isFirstStation = isFirstStation;
	}

	public boolean isLastStation() {
		return isLastStation;
	}

	public void setLastStation(boolean isLastStation) {
		this.isLastStation = isLastStation;
	}
		
}