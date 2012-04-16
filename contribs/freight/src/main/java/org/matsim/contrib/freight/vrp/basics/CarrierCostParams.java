package org.matsim.contrib.freight.vrp.basics;

public class CarrierCostParams implements CostParams {


	public final double transportCost_per_second;
	
	public final double transportCost_per_meter;
	
	public final double waitingCost_per_second;
	
	public final double serviceCost_per_second;
	
	public final double penality_per_secondTooLate;
	
	public final double cost_per_vehicle;

	public CarrierCostParams(double transportCostPerSecond,
			double transportCostPerMeter, double waitingCostPerSecond,
			double serviceCostPerSecond, double penalityPerSecondTooLate,
			double costPerVehicle) {
		super();
		transportCost_per_second = transportCostPerSecond;
		transportCost_per_meter = transportCostPerMeter;
		waitingCost_per_second = waitingCostPerSecond;
		serviceCost_per_second = serviceCostPerSecond;
		penality_per_secondTooLate = penalityPerSecondTooLate;
		cost_per_vehicle = costPerVehicle;
	}
	
	
}
