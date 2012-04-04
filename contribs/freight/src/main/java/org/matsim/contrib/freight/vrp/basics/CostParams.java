package org.matsim.contrib.freight.vrp.basics;

public interface CostParams {
	
	public double getCostPerSecondTransport();
	
	public double getCostPerMeter();
	
	public double getCostPerSecondTooLate();
	
	public double getCostPerVehicle();

	public double getCostPerSecondWaiting();

	public double getCostPerSecondService();


}
