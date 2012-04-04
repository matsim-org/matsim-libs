package org.matsim.contrib.freight.vrp.basics;

public class CarrierCostFunction {
	
	private CostParams costParams;

	public CarrierCostFunction(CostParams costParams) {
		super();
		this.costParams = costParams;
	}
	
	public double getCosts(double transportTime_in_seconds, double transportDistance_in_meters, double waitingTime_in_seconds, 
			double serviceTime_in_seconds, double tooLate_in_seconds){
		return transportTime_in_seconds*costParams.getCostPerSecondTransport() + transportDistance_in_meters*costParams.getCostPerMeter() +
			waitingTime_in_seconds*costParams.getCostPerSecondTransport() + serviceTime_in_seconds*costParams.getCostPerSecondTransport() + tooLate_in_seconds*costParams.getCostPerSecondTooLate();
	}

}
