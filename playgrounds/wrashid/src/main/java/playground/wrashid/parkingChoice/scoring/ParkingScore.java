package playground.wrashid.parkingChoice.scoring;

public class ParkingScore {

	//
	double getScoreForWalking(double distance, double walkinSpeedInMetersPerSecond, double getActPerformanceEarningRate){
		return distance/walkinSpeedInMetersPerSecond*getActPerformanceEarningRate;
	}
	
}
