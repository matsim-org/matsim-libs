package playground.jjoubert.CommercialTraffic;

public class Activity {
	
	private long startTime;
	private long duration;
	private GPSPoint location;
	
	public Activity(long startTime, long duration, GPSPoint point) {
		this.startTime = startTime;
		this.duration = duration;
		this.location = point;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getDuration() {
		return duration;
	}

	public GPSPoint getLocation() {
		return location;
	}
	
}
