package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class ServiceRequest {
	
	public Id from;
	public Id to;
	public int size;
	public double startPickup;
	public double endPickup;
	public double startDelivery;
	public double endDelivery;
	public ServiceRequest(Id from, Id to, int size, double startPickup,
			double endPickup, double startDelivery, double endDelivery) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.startPickup = startPickup;
		this.endPickup = endPickup;
		this.startDelivery = startDelivery;
		this.endDelivery = endDelivery;
	}
	

}
