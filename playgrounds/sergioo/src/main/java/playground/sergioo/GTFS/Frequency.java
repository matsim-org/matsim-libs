package playground.sergioo.GTFS;

import java.util.Date;

public class Frequency {
	
	//Attributes
	private Date startTime;
	private Date endTime;
	private int secondsPerDeparture;
	
	//Methods
	/**
	 * @param startTime
	 * @param endTime
	 * @param secondsPerDeparture
	 */
	public Frequency(Date startTime, Date endTime, int secondsPerDeparture) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.secondsPerDeparture = secondsPerDeparture;
	}
	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}
	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}
	/**
	 * @return the secondsPerDeparture
	 */
	public int getSecondsPerDeparture() {
		return secondsPerDeparture;
	}
	
}
