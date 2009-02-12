package playground.jjoubert.CommercialTraffic;

import java.util.GregorianCalendar;

public class Activity {
	
	private GregorianCalendar startTime;
	private GregorianCalendar endTime;
	private int startHour;
	private int duration; // in MINUTES
	private GPSPoint location;
	
	public Activity(GregorianCalendar startTime, GregorianCalendar endTime, GPSPoint point) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = point;
		this.duration = (int)((endTime.getTimeInMillis() - startTime.getTimeInMillis())/(1000 * 60));
		this.startHour = startTime.get( GregorianCalendar.HOUR_OF_DAY );
	}

	public GregorianCalendar getStartTime() {
		return startTime;
	}

	public GregorianCalendar getEndTime() {
		return endTime;
	}

	public int getDuration() {
		return duration;
	}

	public GPSPoint getLocation() {
		return location;
	}

	public int getStartHour() {
		return startHour;
	}
	
	

	
}
