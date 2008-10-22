package playground.anhorni.locationchoice.scoring;

//import org.apache.log4j.Logger;
import playground.anhorni.locationchoice.facilityLoad.FacilityPenalty;


public class Penalty {
	
	private double startTime = 0;
	private double endTime = 0;
	private FacilityPenalty facilityPenalty = null;
	private double score = 0.0;
	
	//private static final Logger log = Logger.getLogger(Penalty.class);
	
	public Penalty(double startTime, double endTime, FacilityPenalty facilityPenalty, double score) {
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
		this.facilityPenalty.finish();
		return this.score * this.facilityPenalty.getCapacityPenaltyFactor(startTime, endTime);
	}
}
