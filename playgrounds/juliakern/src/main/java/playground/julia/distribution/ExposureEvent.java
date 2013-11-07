package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class ExposureEvent {
	
	// ! exposure value = time x concentration
	Id personId; 
	Double startTime; 
	Double endTime;
	Double personalExposureValue; 
	String activitytype;
	

	public ExposureEvent(Id personId, double startTime, double endTime,
			Double personalExposureValue, String activitytype) {
		this.personId = personId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.personalExposureValue = personalExposureValue;
		this.activitytype = activitytype;
	}

	public Id getPersonId() {
		return personId;
	}

	public Double getAverageExposure() {
		return personalExposureValue/(endTime-startTime);
	}

	public Double getExposure() {
		return personalExposureValue;
	}

	public Double getDuration() {
		return endTime-startTime;
	}

}
