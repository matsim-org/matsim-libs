package playground.wrashid.parkingChoice.api;

import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;

/**
 * 
 * @author wrashid
 *
 */
public interface PreferredParkingManager {

	boolean considerForChoiceSet(PreferredParking preferredParking, Id personId, double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
	boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds, ActInfo targetActInfo);
	
}
