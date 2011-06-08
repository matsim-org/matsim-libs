package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author wrashid
 *
 */
public interface ReservedParkingManager {

	boolean considerForChoiceSet(ReservedParking reservedParking, Id personId, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
