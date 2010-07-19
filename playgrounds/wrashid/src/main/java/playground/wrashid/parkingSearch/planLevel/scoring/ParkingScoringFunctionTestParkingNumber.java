package playground.wrashid.parkingSearch.planLevel.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;
import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;

public class ParkingScoringFunctionTestParkingNumber extends ParkingScoringFunction {

	public ParkingScoringFunctionTestParkingNumber(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income,
			ActivityFacilitiesImpl parkingFacilities) {
		super(parkingPriceMapping, income, parkingFacilities);
	}

	// TODO: perhaps remove calls (if not used till 1 sep, 2010).
	public double getScore(ActivityImpl targetActivity, Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Id personId,
			double parkingArrivalDuration, double parkingDepartureDuration, Plan plan, double delta, boolean forRanking) {
		if (parkingFacilityId.toString().equalsIgnoreCase(personId.toString())){
			return 1;
		} else {
			return 0;	
		}
	}

}
