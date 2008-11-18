package org.matsim.locationchoice.facilityload;

//import org.apache.log4j.Logger;


public class ScoringPenalty {
	
	private double startTime = 0;
	private double endTime = 0;
	private FacilityPenalty facilityPenalty = null;
	private double score = 0.0;
	
	//private static final Logger log = Logger.getLogger(Penalty.class);
	
	public ScoringPenalty(double startTime, double endTime, FacilityPenalty facilityPenalty, double score) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.facilityPenalty = facilityPenalty;
		this.score = score;
	}


	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
		
	public double getPenalty() {
		this.facilityPenalty.finish(); // is this still needed? we have a call in EventsToFacilityLoad
		return this.score * this.facilityPenalty.getCapacityPenaltyFactor(startTime, endTime);
	}
}
