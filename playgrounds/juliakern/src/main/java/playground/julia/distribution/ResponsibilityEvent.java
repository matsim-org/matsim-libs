package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class ResponsibilityEvent {

	private Id personId;
	private Double startTime;
	private Double endTime;
	private Double concentration;
	private String location;
	
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

	public Double getDuration() {
		return(this.endTime-this.startTime);
	}

	public Id getPersonId() {
		return personId;
	}

}
