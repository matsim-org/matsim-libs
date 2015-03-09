package playground.sergioo.workplaceCapacities2012;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.vividsolutions.jts.geom.Polygon;

public class MPAreaData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Attributes
	private final Id<ActivityFacility> id;
	private final Coord coord;
	private Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
	private final String type;
	private final double maxArea;
	private Id<ActivityFacility> zoneId;
	private final double modeShare;
	private final Map<String, ActivityOption> activityOptions = new HashMap<String, ActivityOption>();
	private final Map<Id<TransitStopFacility>, Double> travelTimes = new HashMap<Id<TransitStopFacility>, Double>();
	private Polygon polygon;
	
	//Methods
	public MPAreaData(Id<ActivityFacility> id, Coord coord, String type, double maxArea, Id<ActivityFacility> zoneId, double modeShare) {
		super();
		this.id = id;
		this.coord = coord;
		this.type = type;
		this.maxArea = maxArea;
		this.zoneId = zoneId;
		this.modeShare = modeShare;
	}
	public Id<ActivityFacility> getId() {
		return id;
	}
	public Coord getCoord() {
		return coord;
	}
	public Set<Id<Link>> getLinkIds() {
		return linkIds;
	}
	public void addLinkId(Id<Link> linkId) {
		linkIds.add(linkId);
	}
	public String getType() {
		return type;
	}
	public double getMaxArea() {
		return maxArea;
	}
	public Id<ActivityFacility> getZoneId() {
		return zoneId;
	}
	public double getModeShare() {
		return modeShare;
	}
	public Map<String, ActivityOption> getActivityOptions() {
		return activityOptions;
	}
	public void setPolygon(Polygon polygon) {
		this.polygon = polygon;
	}
	public void putActivityOption(ActivityOption activityOption) {
		activityOptions.put(activityOption.getType(), activityOption);
	}
	public void addTravelTime(Id<TransitStopFacility> stopId, Double travelTime) {
		travelTimes.put(stopId, travelTime);
	}
	public Double getTravelTime(Id<TransitStopFacility> stopId) {
		return travelTimes.get(stopId);
	}
	public Polygon getPolygon() {
		return polygon;
	}

}
