package playground.jjoubert.CommercialTraffic;

import java.util.GregorianCalendar;

/**
 * A class to represent an activity of a commercial vehicle. Created 
 * for the DigiCore data set on commercial vehicle movement in South 
 * Africa. 
 * 
 * @author jjoubert
 * 
 */
public class Activity {
	
	private GregorianCalendar startTime;
	private GregorianCalendar endTime;
	private GPSPoint location;
	private int duration; // in MINUTES
	private int startHour;
	
	/**
	 * Constructs an activity with following minimum parameters
	 * <p>
	 * @param startTime		expressed as a GregorianCalendar
	 * @param endTime		expressed as a GregorianCalendar
	 * @param point			the GPS location, should be created using UTM35S coordinate 
	 * 						system for South Africa.</p>
	 * 
	 *  Once constructed, the activity's duration is calculated (in minutes), and the 
	 *  hour of the day is established. 				
	 */
	
	public Activity(GregorianCalendar startTime, GregorianCalendar endTime, GPSPoint point) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = point;
		this.startHour = startTime.get( GregorianCalendar.HOUR_OF_DAY );
		setDuration();
	}

	public GregorianCalendar getStartTime() {
		return startTime;
	}

	public void setStartTime(GregorianCalendar startTime) {
		this.startTime = startTime;
		setDuration();
	}

	public GregorianCalendar getEndTime() {
		return endTime;
	}
	
	public void setEndTime(GregorianCalendar endTime) {
		this.endTime = endTime;
		setDuration();
	}

	public int getDuration() {
		return duration;
	}
	
	private void setDuration(){
		this.duration = (int)((endTime.getTimeInMillis() - startTime.getTimeInMillis())/(1000 * 60));
	}

	public GPSPoint getLocation() {
		return location;
	}

	public int getStartHour() {
		return startHour;
	}
	
	

	
}
