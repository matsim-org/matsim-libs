package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playgroundMeng.ptTravelTimeAnalysis.CarTravelInfo;
import playgroundMeng.ptTravelTimeAnalysis.PtTraveInfo;

public class Trip {
	ActivityImp activityEndImp;
	ActivityImp ActivityStartImp;
	double travelTime;
	Id<Person> personId;
	List<String> modes = new LinkedList<String>();
	CarTravelInfo carTravelInfo;
	PtTraveInfo ptTraveInfo;
	double ratio;
	
	public Trip() {
		// TODO Auto-generated constructor stub
	}
	public void setRatio(double ratio) {
		this.ratio = ratio;
	}
	public double getRatio() {
		return ptTraveInfo.getTravelTime() / carTravelInfo.getTravelTime();
	}
	public void setCarTravelInfo(CarTravelInfo carTravelInfo) {
		this.carTravelInfo = carTravelInfo;
	}
	public CarTravelInfo getCarTravelInfo() {
		return carTravelInfo;
	}
	public void setPtTraveInfo(PtTraveInfo ptTraveInfo) {
		this.ptTraveInfo = ptTraveInfo;
	}
	public PtTraveInfo getPtTraveInfo() {
		return ptTraveInfo;
	}

	public ActivityImp getActivityEndImp() {
		return activityEndImp;
	}

	public void setActivityEndImp(ActivityImp activityEndImp) {
		this.activityEndImp = activityEndImp;
	}



	public ActivityImp getActivityStartImp() {
		return ActivityStartImp;
	}



	public void setActivityStartImp(ActivityImp activityStartImp) {
		ActivityStartImp = activityStartImp;
		this.setTravelTime(this.getTravelTime());
	}



	public double getTravelTime() {
		return this.ActivityStartImp.getStartTime() - this.activityEndImp.getStartTime();
	}



	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}



	public Id<Person> getPersonId() {
		return personId;
	}



	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}



	@Override
	public String toString() {
		return "Trip [beginnActivityImp=" + activityEndImp + ", endActivityImp=" + ActivityStartImp +", legMode=" + modes + ", travelTime="
				+ travelTime + ", personId=" + personId + "]";
	}



	public void setModes(List<String> modes) {
		this.modes = modes;
	}

	public List<String> getModes() {
		return modes;
	}
	

}
