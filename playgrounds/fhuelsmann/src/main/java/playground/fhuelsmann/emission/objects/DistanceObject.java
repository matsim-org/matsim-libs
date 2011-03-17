

package playground.fhuelsmann.emission.objects;
import org.matsim.api.core.v01.Id;

public class DistanceObject {



	double sumDistance;
	public double getSumDistance() {
		return sumDistance;
	}

	public void setSumDistance(double sumDistance) {
		this.sumDistance = sumDistance;
	}

	String activity;
	public Id getLinkId() {
		return LinkId;
	}

	public void setLinkId(Id linkId) {
		LinkId = linkId;
	}

	double distance;
	Id LinkId;
	private Id personId;
	
	
	
	public DistanceObject(String activity, double distance, Id personId, Id LinkId) {
		this.activity= activity;
		this.distance= distance;
		this.LinkId= LinkId;
		this.personId = personId;
	}

	public Id getPersonId() {
		return personId;
	}

	public void setPersonId(Id personId) {
		this.personId = personId;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}
	
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

}
