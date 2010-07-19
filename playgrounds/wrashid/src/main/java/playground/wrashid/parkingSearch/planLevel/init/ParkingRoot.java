package playground.wrashid.parkingSearch.planLevel.init;

import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacity;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyMaintainer;
import playground.wrashid.parkingSearch.planLevel.parkingActivityDuration.ParkingActivityDuration;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping1;
import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingScoringFunction;

public class ParkingRoot {

	private static ClosestParkingMatrix cpm = null;
	private static LinkParkingFacilityAssociation lpfa = null;
	private static ParkingCapacity pc = null;
	private static double parkingPriceScoreScalingFactor;
	private static double parkingActivityDurationPenaltyScalingFactor;
	private static ParkingOccupancyMaintainer parkingOccupancyMaintainer;
	private static ParkingActivityDuration parkingActivityDuration;
	private static ParkingScoringFunction parkingScoringFunction;

	public static void setParkingScoringFunction(ParkingScoringFunction parkingScoringFunction) {
		ParkingRoot.parkingScoringFunction = parkingScoringFunction;
	}

	public static ParkingScoringFunction getParkingScoringFunction() {
		return parkingScoringFunction;
	}

	public static ParkingActivityDuration getParkingActivityDuration() {
		return parkingActivityDuration;
	}

	public static double getPriceScoreScalingFactor() {
		return parkingPriceScoreScalingFactor;
	}

	public static double getParkingActivityDurationPenaltyScalingFactor() {
		return parkingActivityDurationPenaltyScalingFactor;
	}

	public static void init(ActivityFacilitiesImpl facilities, NetworkLayer network, Controler controler) {
		cpm = new ClosestParkingMatrix(facilities, network);
		lpfa = new LinkParkingFacilityAssociation(facilities, network);
		pc = new ParkingCapacity(facilities);
		parkingActivityDuration = new ParkingActivityDuration();

		String tempStringValue = controler.getConfig().findParam("parking", "parkingPriceScoreScalingFactor");
		checkIfNull(tempStringValue);
		parkingPriceScoreScalingFactor = Double.parseDouble(tempStringValue);

		tempStringValue = controler.getConfig().findParam("parking", "parkingActivityDurationPenaltyScalingFactor");
		checkIfNull(tempStringValue);
		parkingActivityDurationPenaltyScalingFactor = Double.parseDouble(tempStringValue);
		
		checkIfNull(parkingScoringFunction);
		parkingScoringFunction.setParkingFacilities(facilities);
	}

	public static void setRanking(ParkingScoringFunction ranking) {
		ParkingRoot.parkingScoringFunction = ranking;
	}

	public static ClosestParkingMatrix getClosestParkingMatrix() {
		checkIfNull(cpm);
		return cpm;
	}

	public static LinkParkingFacilityAssociation getLinkParkingFacilityAssociation() {
		checkIfNull(lpfa);
		return lpfa;
	}

	public static ParkingCapacity getParkingCapacity() {
		checkIfNull(pc);
		return pc;
	}

	private static void checkIfNull(Object obj) {
		if (obj == null) {
			throw new Error("Please initialize the variables first.");
		}

	}

	public static void setParkingOccupancyMaintainer(ParkingOccupancyMaintainer _parkingOccupancyMaintainer) {
		parkingOccupancyMaintainer = _parkingOccupancyMaintainer;
	}

	public static ParkingOccupancyMaintainer getParkingOccupancyMaintainer() {
		return parkingOccupancyMaintainer;
	}

}
