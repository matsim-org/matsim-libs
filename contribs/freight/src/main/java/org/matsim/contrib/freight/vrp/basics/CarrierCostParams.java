package org.matsim.contrib.freight.vrp.basics;

public class CarrierCostParams implements CostParams {


	private static double COST_PER_SECOND = 50.0/(60.0*60.0);
	
	private static double COST_PER_METER = 1.0/1000.0;
	
	private static double COST_PER_SECONDTOOLATE = 1000.0/(60.0*60.0);
	
	private static double COST_PER_VEHICLE = 100.0;
	
	
	@Override
	public double getCostPerSecondTransport() {
		return COST_PER_SECOND;
	}

	@Override
	public double getCostPerMeter() {
		return COST_PER_METER;
	}

	@Override
	public double getCostPerSecondTooLate() {
		return COST_PER_SECONDTOOLATE;
	}
	
	public double getCostPerVehicle(){
		return COST_PER_VEHICLE;
	}

	@Override
	public double getCostPerSecondWaiting() {
		return COST_PER_SECOND;
	}

	@Override
	public double getCostPerSecondService() {
		return COST_PER_SECOND;
	}

}
