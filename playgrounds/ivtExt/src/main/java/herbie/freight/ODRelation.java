package herbie.freight;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class ODRelation implements Comparable<ODRelation> {
	private Id id;
	private String originName;
	private String destinationName;
	private Coord origin;
	private Coord destination;
	private double weight;
	private Id originId;
	private Id destinationId;
	
	public ODRelation(Id id, Coord coordOrigin, Coord coordDestination, double weight, String originName, String destinationName, 
			Id originId, Id destinationId) {
		this.id = id;
		this.origin = coordOrigin;
		this.destination = coordDestination;
		this.weight = weight;
		this.originName = originName;
		this.destinationName = destinationName;
		this.originId = originId;
		this.destinationId = destinationId;
	}
	
	public Coord getOrigin() {
		return origin;
	}
	public void setOrigin(Coord origin) {
		this.origin = origin;
	}
	public Coord getDestination() {
		return destination;
	}
	public void setDestination(Coord destination) {
		this.destination = destination;
	}
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	/* 
	 * Compare keys (double scores). 
	 * If the scores are identical, additionally use the 'alternatives' id's to sort such that deterministic order is ensured
	 */
	@Override
	public int compareTo(ODRelation o) {
		// numerics
		double epsilon = 0.000001;
		
		// reverse order:
		if (Math.abs(this.weight - o.getWeight()) > epsilon) {
			if (this.weight > o.getWeight()) return -1;
			else return +1;
		}		
		else {
			return this.id.compareTo(o.getId());
		}
	}

	public String getOriginName() {
		return originName;
	}

	public void setOriginName(String originName) {
		this.originName = originName;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public Id getOriginId() {
		return originId;
	}

	public void setOriginId(Id originId) {
		this.originId = originId;
	}

	public Id getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(Id destinationId) {
		this.destinationId = destinationId;
	}
}
