package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class ResponsibilityEvent {

	Id personId;
	Double startTime;
	Double endTime;
	Double concentration;
	String location;
	
	public Double getExposureValue() {
		return this.getDuration()*this.concentration;
	}

	public ResponsibilityEvent(Id personId, Double startTime, Double endTime,
			Double concentration, String location) {
		this.personId = personId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.concentration = concentration;
		this.location = location;
	}

	private Double getDuration() {
		return(this.endTime-this.startTime);
	}

}
