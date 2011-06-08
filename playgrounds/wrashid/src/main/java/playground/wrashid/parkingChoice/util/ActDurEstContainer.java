package playground.wrashid.parkingChoice.util;

public class ActDurEstContainer {

	public Double startTimeOfFirstAct;
	public int indexOfCurrentActivity;
	public int skipAllPlanElementsTillIndex;
	
	public boolean isCurrentParkingTimeOver(){
		if (indexOfCurrentActivity>skipAllPlanElementsTillIndex){
			return true;
		}
		return false;
	}
	
	public void registerNeuActivity(){
		indexOfCurrentActivity += 2;
	}
	
}
