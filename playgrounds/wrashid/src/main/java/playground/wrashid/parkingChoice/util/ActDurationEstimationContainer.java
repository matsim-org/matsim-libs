package playground.wrashid.parkingChoice.util;

public final class ActDurationEstimationContainer {

	private Double endTimeOfFirstAct;
	private int indexOfCurrentActivity;
	private int skipAllPlanElementsTillIndex;
	
	public boolean isCurrentParkingTimeOver(){
		if ( indexOfCurrentActivity > skipAllPlanElementsTillIndex ){
			return true;
		}
		return false;
	}
	
	public void registerNewActivity(){
		indexOfCurrentActivity += 2 ;
	}

	public Double getEndTimeOfFirstAct() {
		return endTimeOfFirstAct;
	}

	public void setEndTimeOfFirstAct(Double endTimeOfFirstAct) {
		this.endTimeOfFirstAct = endTimeOfFirstAct;
	}

	public int getIndexOfCurrentActivity() {
		return indexOfCurrentActivity;
	}

	public void setSkipAllPlanElementsTillIndex(int skipAllPlanElementsTillIndex) {
		this.skipAllPlanElementsTillIndex = skipAllPlanElementsTillIndex;
	}
	
}
