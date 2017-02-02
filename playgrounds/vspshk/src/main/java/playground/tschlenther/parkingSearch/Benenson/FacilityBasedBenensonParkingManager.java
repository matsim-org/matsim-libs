/**
 * 
 */
package playground.tschlenther.parkingSearch.Benenson;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.facilities.ActivityFacility;

/**
 * @author schlenther
 */
public class FacilityBasedBenensonParkingManager extends FacilityBasedParkingManager {

	/**
	 * @param scenario
	 */
	public FacilityBasedBenensonParkingManager(Scenario scenario) {
		super(scenario);
	}
	
	public double getNrOfAllParkingSpacesOnLink (Id<Link> linkId){
		int allSpaces = 0;
		for (Id<ActivityFacility> fac : this.facilitiesPerLink.get(linkId)){
			allSpaces += this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
		}
		return allSpaces;
	}
	
	public double getNrOfFreeParkingSpacesOnLink (Id<Link> linkId){
		int allFreeSpaces = 0;
		for (Id<ActivityFacility> fac : this.facilitiesPerLink.get(linkId)){
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			allFreeSpaces += (cap - this.occupation.get(fac).intValue());
		}
		return allFreeSpaces;
	}

}
