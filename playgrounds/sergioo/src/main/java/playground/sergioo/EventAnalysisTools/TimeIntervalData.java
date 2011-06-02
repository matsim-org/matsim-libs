package playground.sergioo.EventAnalysisTools;

public class TimeIntervalData {
	
	private int numberExitVehicles=0;
	private double sumSpeeds=0;
	private double sumTravelTimes=0;
	private double sumQuantity=0;
	
	public double getFlow() {
		return numberExitVehicles/TimeSpaceDistribution.TIME_INTERVAL;
	}
	public double getAvgSpeeds() {
		return sumSpeeds/numberExitVehicles;
	}
	public double getAvgTravelTimes() {
		return sumTravelTimes/numberExitVehicles;
	}
	public double getConcentration() {
		return numberExitVehicles*numberExitVehicles/(sumSpeeds*TimeSpaceDistribution.TIME_INTERVAL);
	}
	public double getAvgQuantity() {
		return sumQuantity/TimeSpaceDistribution.TIME_INTERVAL;
	}
	public void addExitVehicle() {
		this.numberExitVehicles ++;
	}
	public void sumSpeeds(double sumSpeeds) {
		this.sumSpeeds += sumSpeeds;
	}
	public void sumTravelTimes(double sumTravelTimes) {
		this.sumTravelTimes += sumTravelTimes;
	}
	public void sumQuantity(double sumQuantity) {
		this.sumQuantity += sumQuantity;
	}
	
}
