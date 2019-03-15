package org.matsim.core.mobsim.qsim.interfaces;

import java.util.List;
import java.util.Map;

import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.facilities.Facility;

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

		String getMode();
		// not sure if I like that, but with current design (where the confirmation goes to the TripInfo.Provider, not to the TripInfo instance that we have) I am not sure
		// if it is possible otherwise.  kai, mar'19

		void bookTrip( MobsimPassengerAgent agent, TripInfoWithRequiredBooking tripInfo );
	}
}
