package playground.christoph.withinday.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

public class WithinDayLinkImpl extends LinkImpl {

	private double travelTime;
	
	protected WithinDayLinkImpl(Id id, Node from, Node to, NetworkImpl network,
			double length, double freespeed, double capacity, double lanes) {
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}
}
