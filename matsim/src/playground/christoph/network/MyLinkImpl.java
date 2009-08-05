package playground.christoph.network;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

public class MyLinkImpl extends LinkImpl{

	protected double vehiclesCount;
	protected double travelTime;
	protected double travelCost;
	
	public MyLinkImpl(Id id, BasicNode from, BasicNode to, NetworkLayer network, double length, double freespeed, double capacity, double lanes)
	{
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}
	
	public double getVehiclesCount() {
		return vehiclesCount;
	}

	public void setVehiclesCount(double vehiclesCount) {
		this.vehiclesCount = vehiclesCount;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public double getTravelCost() {
		return travelCost;
	}

	public void setTravelCost(double travelCost) {
		this.travelCost = travelCost;
	}
}
