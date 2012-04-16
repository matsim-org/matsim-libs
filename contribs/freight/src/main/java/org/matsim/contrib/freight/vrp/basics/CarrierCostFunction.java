package org.matsim.contrib.freight.vrp.basics;

public class CarrierCostFunction {
	
	public final CarrierCostParams costParams;

	private double costs = 0.0;
	
	private double transportTime = 0.0;
	
	public double getTransportTime() {
		return transportTime;
	}

	public CarrierCostFunction(CarrierCostParams costParams) {
		super();
		this.costParams = costParams;
	}
	
	public void reset(){
		costs = 0.0;
		transportTime = 0.0;
	}
	
	public void addCosts(double value){
		costs += value;
	}
	
	public void addTransportTime(double value){
		transportTime += value;
	}
	
	public void addActiveVehicle(){
		costs += costParams.cost_per_vehicle;
	}
	
	public double getCosts() {	
		return costs;
	}

}
