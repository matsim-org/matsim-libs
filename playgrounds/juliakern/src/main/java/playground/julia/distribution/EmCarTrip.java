package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmCarTrip {
	Double startTime;
	Double endTime;
	Id personId;
	Id linkId;
	
	public EmCarTrip(Double startOfTimeInterval, Double endOfTimeInterval, 	Id personId, Id linkId){
		this.startTime=startOfTimeInterval;
		this.endTime=endOfTimeInterval;
		this.personId=personId;
		this.linkId=linkId;
	}

	public Double getStartTime() {
		return this.startTime;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public Double getEndTime() {
		return this.endTime;
	}

}
