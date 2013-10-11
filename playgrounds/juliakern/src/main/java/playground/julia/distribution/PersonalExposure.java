package playground.julia.distribution;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class PersonalExposure {
	
	Id personalId;
	List<TimeDependendExposure> timeDependendExposure;
	
	public PersonalExposure(Id personalId){
		this.personalId=personalId;
		timeDependendExposure = new LinkedList<TimeDependendExposure>();
	}

	public void addExposureIntervall(double startTime, double endTime,
			Double poll, String actType) {
		TimeDependendExposure tde = new TimeDependendExposure(startTime, endTime, poll, actType);
		timeDependendExposure.add(tde);
	}

	public Id getPersonalId() {
		return personalId;
	}

	public void setPersonalId(Id personalId) {
		this.personalId = personalId;
	}

	public TimeDependendExposure getNextTimeDependendExposure(TimeDependendExposure previous) {
		
		if(previous==null){
			previous = new TimeDependendExposure(-Double.MAX_VALUE, null, null, null);
		}
		
		TimeDependendExposure next = new TimeDependendExposure(Double.MAX_VALUE, null, null, null);
		
		for(TimeDependendExposure tde: timeDependendExposure){
			if(tde.getStartTime()<next.getStartTime()&& tde.getStartTime()>previous.getStartTime()){
				next = tde;
			}
		}
		
		if(next.getEndTime()==null)return null;
		return next;
	}

	public String getStringForInterval(TimeDependendExposure tde) {
		String intervalInformation = new String();
		//"activity type \t start time \t end time \t duration \t exposure value \t exposure x duration"
		intervalInformation += tde.getType() + "\t";
		intervalInformation += tde.getStartTime() + "\t";
		intervalInformation += tde.getEndTime() + "\t";
		intervalInformation += tde.getDuration() + "\t";
		intervalInformation += tde.getExposureValue() + "\t";
		intervalInformation += (tde.getExposureValue()*tde.getDuration());
		return intervalInformation;
	}

	public Double getAverageExposure() {
		
		Double timeSum =0.0;
		Double valueSum=0.0;
		for(TimeDependendExposure tde: timeDependendExposure){
			timeSum += tde.getDuration();
			valueSum += (tde.getDuration()*tde.getExposureValue());
		}
		
		if(timeSum<=0.0)return 0.0;
		
		return (valueSum/timeSum);
	}
}
