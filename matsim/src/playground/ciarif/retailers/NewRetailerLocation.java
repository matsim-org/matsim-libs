package playground.ciarif.retailers;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.locationchoice.facilityload.EventsToFacilityLoad;
import org.matsim.world.Location;

public class NewRetailerLocation {

	private Collection<Location> freeLocations;
	private Collection<Id> reteilersToBeRelocated;
	
		public NewRetailerLocation (Collection<Location> freeLocations, Collection<Id> retailersToBeRelocated) {
		
		this.freeLocations = freeLocations;
		this.reteilersToBeRelocated = reteilersToBeRelocated;
		
	}
	/*TODO Here it should happens the following:
	 * Information from the facility to be relocated is taken 
	 * The old facility is deleted
	 * A new facility with the same attributes, but with different location is constructed
	 * The location assigned is deleted from the freeLocations list, the location freed is added to the freeLocations list*/ 
	
}
