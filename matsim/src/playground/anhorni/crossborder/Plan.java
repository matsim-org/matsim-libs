package playground.anhorni.crossborder;

import org.matsim.utils.identifiers.IdI;

public class Plan {
	
	private IdI startLink;
	private IdI endLink;
	
	private IdI id;
	private String activityType;
	private String tempHomeType;
	private int startTime;
	
	public Plan(IdI startLink, IdI endLink) {
		this.startLink=startLink;
		this.endLink=endLink;
	}
	
	public IdI getStartLink() {
		return startLink;
	}
	public void setStartLink(IdI startLink) {
		this.startLink = startLink;
	}
	public IdI getEndLink() {
		return endLink;
	}
	public void setEndLink(IdI endLink) {
		this.endLink = endLink;
	}
	public String getActivityType() {
		return activityType;
	}
	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}
	public String getTempHomeType() {
		return tempHomeType;
	}
	public void setTempHomeType(String tempHomeType) {
		this.tempHomeType = tempHomeType;
	}
	public int getStartTime() {
		return startTime;
	}
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public IdI getId() {
		return id;
	}

	public void setId(IdI id) {
		this.id = id;
	}
}
