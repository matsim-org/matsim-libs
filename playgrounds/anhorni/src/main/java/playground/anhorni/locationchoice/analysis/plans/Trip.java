package playground.anhorni.locationchoice.analysis.plans;

import org.matsim.api.core.v01.TransportMode;

public class Trip {
	private String purpose = null;
	private TransportMode mode = TransportMode.undefined;
	private double distance = 0.0;
	private double duration = 0.0;
	
	double weight = 1.0;
	
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public TransportMode getMode() {
		return mode;
	}
	public void setMode(TransportMode mode) {
		this.mode = mode;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
}
