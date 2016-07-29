package playground.wrashid.parkingSearch.planLevel.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.facilities.ActivityFacilitiesImpl;

import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;

public class ParkingScoringFunctionTestNumberOfParkings extends ParkingScoringFunction {

	public ParkingScoringFunctionTestNumberOfParkings(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income,
			ActivityFacilitiesImpl parkingFacilities) {
		super(parkingPriceMapping, income, parkingFacilities);
	}

	public double getScore(Activity targetActivity, Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Id personId,
			double parkingArrivalDuration, double parkingDepartureDuration, Plan plan, double delta, boolean forRanking) {
		return -1.0;
	}

}
