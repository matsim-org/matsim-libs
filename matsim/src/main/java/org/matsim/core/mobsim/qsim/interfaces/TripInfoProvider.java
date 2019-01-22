package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Map;

public interface TripInfoProvider{

	enum TimeInterpretation { departure, arrival } ;

	List<TripInfo> getTripInfos( Facility fromFacility, Facility toFacility, double time, TimeInterpretation timeInterpretation, Person person ) ;
	// yyyy maybe "Switches" instead of "Person"?  Similar to DB router (fast connections, regional trains only, ...)?

	interface TripInfo {
		Facility getPickupLocation() ;
		double getExpectedBoardingTime() ;
		Facility getDropoffLocation() ;
		double getExpectedTravelTime() ;
		double getMonetaryPrice() ;
		Map<String,String> getAdditionalAttributes() ;
		String getMode() ;
		double getLatestDecisionTime() ;
	}



}
