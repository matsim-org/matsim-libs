package org.matsim.contrib.ev.strategic.costs;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * These cost parameters should be set in order to retrieve the charging cost
 * structure from the charger attributes.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AttributeBasedChargingCostsParameters extends ReflectiveConfigGroup implements ChargingCostsParameters {
	static public final String SET_NAME = "costs:attribute_based";

	public AttributeBasedChargingCostsParameters() {
		super(SET_NAME);
	}
}
