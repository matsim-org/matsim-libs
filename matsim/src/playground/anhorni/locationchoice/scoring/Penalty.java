package playground.anhorni.locationchoice.scoring;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facility;


public class Penalty {
	
	private double startTime = 0;
	private double endTime = 0;
	private Facility facility = null;
	private double score = 0.0;
	
	private static final Logger log = Logger.getLogger(Penalty.class);
	
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
		log.info(this.facility.getCapacityPenaltyFactor(startTime, endTime));
		this.facility.finish();
		return this.score * this.facility.getCapacityPenaltyFactor(startTime, endTime);
	}
}
