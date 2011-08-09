package vrp.basics;

import org.matsim.api.core.v01.Id;

import vrp.api.Customer;
import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class CustomerImpl implements Customer {

	private Node locationNode;
	
	private Relation relationship;
	
	private int demand;
	
	private Id id;
	
	private double serviceTime;
	
	private TimeWindow timeWindow = new TimeWindow(0.0, Double.MAX_VALUE);

	public CustomerImpl(Id customerId, Node locationNode) {
		super();
		this.locationNode = locationNode;
		this.id = customerId;
	}

	public Node getLocation() {
		return locationNode;
	}

	public Relation getRelation() {
		return relationship;
	}

	public void setRelation(Relation relationship) {
		this.relationship = relationship;
		
	}

	public int getDemand() {
		return demand;
	}

	public void setDemand(int demand) {
		this.demand = demand;
	}

	public Id getId() {
		return id;
	}

	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public void setTheoreticalTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	public TimeWindow getTheoreticalTimeWindow() {
		return timeWindow;
	}

	public void setTheoreticalTimeWindow(double start, double end) {
		this.timeWindow = VrpUtils.createTimeWindow(start, end);
	}
	
	@Override
	public String toString() {
		return "[id="+id+"][demand="+demand+"]";
	}

	public boolean hasRelation() {
		if(relationship != null){
			return true;
		}
		return false;
	}
	
	
}
