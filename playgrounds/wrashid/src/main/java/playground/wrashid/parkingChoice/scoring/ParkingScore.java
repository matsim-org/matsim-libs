package playground.wrashid.parkingChoice.scoring;

public class ParkingScore {

	//TODO: load parameters from org.matsim.core.config.groups.PlansCalcRouteConfigGroup
	double getScoreForWalking(double distance, double walkinSpeedInMetersPerSecond, double getActPerformanceEarningRate){
		return distance/walkinSpeedInMetersPerSecond*getActPerformanceEarningRate;
	}
	
}
