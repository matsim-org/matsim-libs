package playground.wrashid.parkingChoice.util;

public class ActDurationEstimationContainer {

	public Double endTimeOfFirstAct;
	public int indexOfCurrentActivity;
	public int skipAllPlanElementsTillIndex;
	
	public boolean isCurrentParkingTimeOver(){
		if (indexOfCurrentActivity>skipAllPlanElementsTillIndex){
			return true;
		}
		return false;
	}
	
	public void registerNewActivity(){
		indexOfCurrentActivity += 2;
	}
	
}
