package playground.mzilske.freight.carrier;


public interface CarrierDriverAgent {

	public abstract void activityEndOccurs(String activityType, double time);

	public abstract void activityStartOccurs(String activityType, double time);

	public abstract void tellDistance(double distance);

	public abstract void tellTraveltime(double time);

	public abstract void tellToll(double toll);

	public abstract double getCapacityUsage();

	public abstract double getDistance();
	
	public abstract double getTime();
	
	public abstract double getVolumes();
	
	public abstract double getPerformace();
	
	public abstract CarrierVehicle getVehicle();

	public abstract double getAdditionalCosts();

}