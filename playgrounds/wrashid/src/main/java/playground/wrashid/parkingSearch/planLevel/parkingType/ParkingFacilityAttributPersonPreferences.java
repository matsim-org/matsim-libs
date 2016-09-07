package playground.wrashid.parkingSearch.planLevel.parkingType;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public interface ParkingFacilityAttributPersonPreferences {

	public ParkingAttribute getParkingFacilityAttributPreferencesOfPersonForActivity(Id<Person> personId, Activity activity);
	
}
