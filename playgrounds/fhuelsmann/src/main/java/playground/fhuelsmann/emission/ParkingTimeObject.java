package playground.fhuelsmann.emission;

import org.matsim.api.core.v01.Id;

public class ParkingTimeObject {

	String activity;
	Id personId;
	double time;
	public String getActivity() {
		return activity;
	}
	public void setActivity(String activity) {
		this.activity = activity;
	}
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public ParkingTimeObject(Id personId, double time,String activity) {
		super();
		this.personId = personId;
		this.time = time;
		this.activity=activity;
	}
}
