package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public interface ReservedParkingManager {

	boolean considerForChoiceSet(ReservedParking reservedParking, Person person, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
