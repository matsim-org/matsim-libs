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
	public double costPerUse = 0.0;

	@Parameter
	public double costPerDuration_min = 0.0;

	@Parameter
	public double costPerEnergy_kWh = 0.0;

	@Parameter
	public double costPerBlockingDuration_min = 0.0;

	@Parameter
	public double blockingDuration_min = 0.0;
}
