package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmActivity {
	Double startOfActivity;
	Double endOfActivity;
	Id personId;
	int xBin;
	int yBin;
	String activityType;

	public EmActivity(Double startOfActivity,	Double endOfActivity, Id personId, int xBin, int yBin, String activityType){
		this.startOfActivity=startOfActivity;
		this.endOfActivity=endOfActivity;
		this.personId=personId;
		this.xBin=xBin;
		this.yBin=yBin;
		this.activityType = activityType;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public double getStartTime() {
		return this.startOfActivity;
	}

	public Double getEndTime() {
		return this.endOfActivity;
	}

	public String getActivityType() {
		return this.activityType;
	}

	public int getXBin() {
		return this.xBin;
	}
	public int getYBin() {
		return this.yBin;
	}
}
