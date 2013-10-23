package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmCarTrip {
	Double startOfTimeInterval;
	Double endOfTimeInterval;
	Id personId;
	Id linkId;
	
	public EmCarTrip(Double startOfTimeInterval, Double endOfTimeInterval, 	Id personId, Id linkId){
		this.startOfTimeInterval=startOfTimeInterval;
		this.endOfTimeInterval=endOfTimeInterval;
		this.personId=personId;
		this.linkId=linkId;
	}

	public Double getDuration() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Id getLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	public Id getPersonId() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getEndTime() {
		// TODO Auto-generated method stub
		return null;
	}

}
