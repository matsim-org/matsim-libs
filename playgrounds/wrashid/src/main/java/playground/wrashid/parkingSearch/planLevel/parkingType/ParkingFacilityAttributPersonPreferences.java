package playground.wrashid.parkingSearch.planLevel.parkingType;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;

public interface ParkingFacilityAttributPersonPreferences {

	public ParkingAttribute getParkingFacilityAttributPreferencesOfPersonForActivity(Id personId, ActivityImpl activity);
	
}
