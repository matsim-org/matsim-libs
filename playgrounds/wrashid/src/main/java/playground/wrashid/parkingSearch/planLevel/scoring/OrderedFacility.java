package playground.wrashid.parkingSearch.planLevel.scoring;

import org.matsim.core.facilities.ActivityFacilityImpl;


/**
 * The facility is ordered accoring to the value (which can be a score, if ordering according to score is required)
 * @author wrashid
 *
 */
public class OrderedFacility implements Comparable<OrderedFacility> {

	ActivityFacilityImpl facility;
	double value;
	
	public OrderedFacility(ActivityFacilityImpl facility, double value) {
		super();
		this.facility = facility;
		this.value = value;
	}

	public ActivityFacilityImpl getFacility() {
		return facility;
	}

	public int compareTo(OrderedFacility otherFacility) {
		if (getValue() > otherFacility.getValue()) {
			return 1;
		} else if (getValue() < otherFacility.getValue()) {
			return -1;
		} else {
			return 0;
		}
	}

	public double getValue() {
		return value;
	}
	
	
}
