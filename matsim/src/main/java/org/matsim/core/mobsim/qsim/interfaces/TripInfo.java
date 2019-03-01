package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;

public interface TripInfo{
	Facility getPickupLocation() ;
	Facility getDropoffLocation() ;
	// If these need an ID, they need to be ActivityFacilities.  Otherwise, they are ad-hoc facilities, which is probably also ok. kai, mar'19

	double getExpectedBoardingTime() ;
	double getExpectedTravelTime() ;
	double getMonetaryPrice() ;
	Map<String,String> getAdditionalAttributes() ;
	String getMode() ;
	double getLatestDecisionTime() ;

	enum TimeInterpretation { departure, arrival }

	interface Provider{
		List<TripInfo> getTripInfos( TripInfoRequest request ) ;
	}

}
