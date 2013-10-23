package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmActivity {
	Double startOfTimeInterval;
	Double endOfTimeInterval;
	Id personId;
	int xBin;
	int yBin;

	public EmActivity(Double startOfTimeInterval,	Double endOfTimeInterval, Id personId, int xBin, int yBin){
		this.startOfTimeInterval=startOfTimeInterval;
		this.endOfTimeInterval=endOfTimeInterval;
		this.personId=personId;
		this.xBin=xBin;
		this.yBin=yBin;
	}

	public Id getPersonId() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Double getEndTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getActivityType() {
		// TODO Auto-generated method stub
		return null;
	}
}
