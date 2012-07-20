package playground.sergioo.workplaceCapacities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityOption;

public class MPAreaData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Attributes
	private final Id id;
	private final Coord coord;
	private Id linkId;
	private final String type;
	private final double maxArea;
	private Id zoneId;
	private final double modeShare;
	private final Map<String, ActivityOption> activityOptions = new HashMap<String, ActivityOption>();
	private final Map<Id, Double> travelTimes = new HashMap<Id, Double>();
	
	//Methods
	public MPAreaData(Id id, Coord coord, String type, double maxArea, Id zoneId, double modeShare) {
		super();
		this.id = id;
		this.coord = coord;
		this.type = type;
		this.maxArea = maxArea;
		this.zoneId = zoneId;
		this.modeShare = modeShare;
	}
	public Id getId() {
		return id;
	}
	public Coord getCoord() {
		return coord;
	}
	public Id getLinkId() {
		return linkId;
	}
	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}
	public String getType() {
		return type;
	}
	public double getMaxArea() {
		return maxArea;
	}
	public Id getZoneId() {
		return zoneId;
	}
	public double getModeShare() {
		return modeShare;
	}
	public Map<String, ActivityOption> getActivityOptions() {
		return activityOptions;
	}
	public void putActivityOption(ActivityOption activityOption) {
		activityOptions.put(activityOption.getType(), activityOption);
	}
	public void addTravelTime(Id stopId, Double travelTime) {
		travelTimes.put(stopId, travelTime);
	}
	public Double getTravelTime(Id stopId) {
		return travelTimes.get(stopId);
	}

}
