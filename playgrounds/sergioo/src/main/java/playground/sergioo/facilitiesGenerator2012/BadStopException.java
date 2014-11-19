package playground.sergioo.facilitiesGenerator2012;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class BadStopException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Attributes
	private Id<TransitStopFacility> stopId;

	//Methods
	public BadStopException(Id<TransitStopFacility> stopId) {
		this.stopId = stopId;
	}
	@Override
	public String getMessage() {
		return "Private bus is not enough for stop "+stopId;
	}
	
}
