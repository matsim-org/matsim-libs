package playground.mmoyo.zz_archive.precalculation;

import org.matsim.api.core.v01.Id;

/**Node labels in a shortest distance tree*/
public class NodeLabel {

	private Id idNode;
	private Id transitRouteId;
	private int transfersNum;
	private int interation;
	private int position;
	private double travelTime;
	private double travelDistance;
	
	public NodeLabel() {

	}

	public Id getIdNode() {
		return idNode;
	}

	public Id getTransitRouteId() {
		return transitRouteId;
	}

	public int getTransfersNum() {
		return transfersNum;
	}

	public int getInteration() {
		return interation;
	}

	public int getPosition() {
		return position;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public double getTravelDistance() {
		return travelDistance;
	}

	
	
	
	
	
}
