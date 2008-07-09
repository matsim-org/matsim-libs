package playground.anhorni.locationchoice.scoring;

import org.matsim.facilities.Facility;


public class Penalty {
	
	private double startTime = 0;
	private double endTime = 0;
	private Facility facility = null;
	private double score = 0.0;
	
	public Penalty(double startTime, double endTime, Facility facility, double score) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.facility = facility;
		this.score = score;
	}

	public Facility getFacility() {
		return facility;
	}
	public void setFacility_id(Facility facility) {
		this.facility = facility;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
		
	public double getPenalty() {
		this.facility.finish();
		return this.score * this.facility.getCapacityPenaltyFactor(startTime, endTime);
	}
}
