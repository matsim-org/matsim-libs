package facilities;

import org.matsim.api.core.v01.Id;
import org.opengis.observation.coverage.TimeInstant;


public class FacilityRelation {
	
	public static class TimeWindow {
		private double startTime;
		
		private double endTime;

		public TimeWindow(double startTime, double endTime) {
			super();
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}
		
		public String toString(){
			return "[start="+startTime+"][end="+endTime+"]";
		}
	}
	
	private Id fromFacility;
	
	private Id toFacility;
	
	private int palletsPerDay;
	
	private TimeWindow timeWindow = null;

	public FacilityRelation(Id fromFacility, Id toFacility, int palletsPerDay) {
		super();
		this.fromFacility = fromFacility;
		this.toFacility = toFacility;
		this.palletsPerDay = palletsPerDay;
	}

	public Id getFromFacility() {
		return fromFacility;
	}

	public Id getToFacility() {
		return toFacility;
	}

	public int getSize() {
		return palletsPerDay;
	}
	
	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	public TimeWindow getTimeWindow() {
		return timeWindow;
	}

	@Override
	public String toString() {
		return "fromId=" + fromFacility + " toId=" + toFacility + " palletsPerDay=" + palletsPerDay;
	}
}