package playground.southafrica.freight.digicore.containers;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class DigicoreActivity implements Activity {
		
	private Id<ActivityFacility> facilityId;
	private Id<Link> linkId;
	private String type;
	private Coord coord;
	private GregorianCalendar startTime;
	private GregorianCalendar endTime;
	private Double maximumDuration = 0.0;
	
	public DigicoreActivity(String type, TimeZone timeZone, Locale locale) {
		this.type = type;
		startTime = new GregorianCalendar(timeZone, locale);
		endTime = new GregorianCalendar(timeZone, locale);
	}

	public boolean isInArea(MultiPolygon area){
		GeometryFactory gf = new GeometryFactory();
		Point p = gf.createPoint(new Coordinate(this.coord.getX(),this.coord.getY()));
		boolean result = false;
		if(area.getEnvelope().contains(p)){
			if(area.contains(p)){
				result = true;
			}else{
				result = false;
			}
		}
		return result;
	}
	
	public String getType(){
		return this.type;
	}

	public Coord getCoord() {
		return this.coord;
	}
	
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
	public GregorianCalendar getStartTimeGregorianCalendar() {
		return startTime;
	}
	
	public GregorianCalendar getEndTimeGregorianCalendar() {
		return endTime;
	}
	
	public boolean isAtSameCoord(DigicoreActivity da){
		if(this.coord.getX() == da.getCoord().getX() &&
				this.coord.getY() == da.getCoord().getY()){
			return true;
		} else{
			return false;
		}
	}
	
	/**
	 * Calculates the duration of the activity in seconds.
	 * @return duration (in sec).
	 */
	public double getDuration(){
		return this.getEndTime() - this.getStartTime();
	}

	@Override
	public void setStartTime(double seconds) {
		startTime.setTimeInMillis(Math.round(seconds * 1000.0));		
	}

	@Override
	public void setEndTime(double seconds) {
		endTime.setTimeInMillis(Math.round(seconds * 1000.0));		
	}

	@Override
	public void setType(String type) {
		this.type = type;		
	}


	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}
	
	public void setLinkId(Id<Link> linkId){
		this.linkId = linkId;
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		return this.facilityId;
	}
	
	public void setFacilityId(Id<ActivityFacility> facilityId){
		this.facilityId = facilityId;
	}

	@Override
	public double getEndTime() {
		return (double)this.endTime.getTimeInMillis() / 1000.0;
	}

	@Override
	public double getStartTime() {
		return this.startTime.getTimeInMillis() / 1000.0;
	}
	

	@Override
	public double getMaximumDuration() {
		return this.maximumDuration;
	}


	@Override
	public void setMaximumDuration(double seconds) {
		this.maximumDuration = seconds;
	}
	
	
}
