package org.matsim.contrib.ev.strategic.costs;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Set these cost parameters to set the charging cost struture globally for all
 * chargers based on the configuration.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultChargingCostsParameters extends ReflectiveConfigGroup implements ChargingCostsParameters {
	static public final String SET_NAME = "costs:default";

	public DefaultChargingCostsParameters() {
		super(SET_NAME);
	}

	@Parameter
	private double costPerUse = 0.0;

	@Parameter
	private double costPerDuration_min = 0.0;

	@Parameter
	private double costPerEnergy_kWh = 0.0;

	@Parameter
	private double costPerBlockingDuration_min = 0.0;

	@Parameter
	private double blockingDuration_min = 0.0;

	public double getCostPerUse() {
		return costPerUse;
	}

	public void setCostPerUse(double costPerUse) {
		this.costPerUse = costPerUse;
	}

	public double getCostPerDuration_min() {
		return costPerDuration_min;
	}

	public void setCostPerDuration_min(double costPerDuration_min) {
		this.costPerDuration_min = costPerDuration_min;
	}

	public double getCostPerEnergy_kWh() {
		return costPerEnergy_kWh;
	}

	public void setCostPerEnergy_kWh(double costPerEnergy_kWh) {
		this.costPerEnergy_kWh = costPerEnergy_kWh;
	}

	public double getCostPerBlockingDuration_min() {
		return costPerBlockingDuration_min;
	}

	public void setCostPerBlockingDuration_min(double costPerBlockingDuration_min) {
		this.costPerBlockingDuration_min = costPerBlockingDuration_min;
	}

	public double getBlockingDuration_min() {
		return blockingDuration_min;
	}

	public void setBlockingDuration_min(double blockingDuration_min) {
		this.blockingDuration_min = blockingDuration_min;
	}
}
