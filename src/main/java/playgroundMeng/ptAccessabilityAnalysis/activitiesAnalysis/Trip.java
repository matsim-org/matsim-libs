package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import com.google.common.collect.TreeMultiset;

public class Trip {
	ActivityImp activityEndImp;
	ActivityImp ActivityStartImp;
	double travelTime;
	Id<Person> personId;
	
	public Trip() {
		// TODO Auto-generated constructor stub
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
		return "Trip [beginnActivityImp=" + activityEndImp + ", endActivityImp=" + ActivityStartImp + ", travelTime="
				+ travelTime + ", personId=" + personId + "]";
	}
	
	

}
