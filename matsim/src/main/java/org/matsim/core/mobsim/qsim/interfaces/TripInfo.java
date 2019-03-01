package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;

public interface TripInfo{
	Facility getPickupLocation() ;
	double getExpectedBoardingTime() ;
	Facility getDropoffLocation() ;
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
