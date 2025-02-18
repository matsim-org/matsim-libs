package org.matsim.contrib.ev.strategic.costs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.NotEmpty;

/**
 * When this parameter set is selected to set up the costs, charging costs are
 * calculated per tariff at the chargers. Each charger can have one or more
 * tariffs, which, in turn, are either available to everybody or only to persons
 * with specific subscriptions. Among the tariffs available at a charger, a
 * person will always choose the cheapest available option.
 * 
 * Each tariff can be added as an individual parameter set of type
 * TariffParameters.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class TariffBasedChargingCostsParameters extends ReflectiveConfigGroup implements ChargingCostsParameters {
	static public final String SET_NAME = "costs:tariff_based";

	public TariffBasedChargingCostsParameters() {
		super(SET_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (TariffParameters.SET_NAME.equals(type)) {
			return new TariffParameters();
		} else {
			throw new IllegalStateException(SET_NAME + " doesn't accept parameter sets of type " + type);
		}
	}

	/**
	 * This parameter set describes a tariff for a charger. A list of subscriptions
	 * can be defined so that only persons with the respective subscription can make
	 * use of the tariff.
	 */
	static public class TariffParameters extends ReflectiveConfigGroup {
		static public final String SET_NAME = "tariff";

		public TariffParameters() {
			super(SET_NAME);
		}

		@Parameter
		@NotEmpty
		public String name;

		@Parameter
		public Set<String> subscriptions = new HashSet<>();

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

	public Map<String, TariffParameters> getTariffParameters() {
		Map<String, TariffParameters> tariffs = new HashMap<>();

		for (ConfigGroup item : getParameterSets(TariffParameters.SET_NAME)) {
			TariffParameters tariff = (TariffParameters) item;
			tariffs.put(tariff.name, tariff);
		}

		return tariffs;
	}
}
