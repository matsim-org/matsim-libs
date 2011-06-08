package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Id;

/**
 * 
 * @author wrashid
 *
 */
public interface PreferredParkingManager {

	boolean considerForChoiceSet(PreferredParking preferredParking, Id personId, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
	boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
