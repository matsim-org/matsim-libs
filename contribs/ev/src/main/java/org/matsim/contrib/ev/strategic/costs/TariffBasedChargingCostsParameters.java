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
		private String name;

		@Parameter
		private Set<String> subscriptions = new HashSet<>();

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

		public String getTariffName() {
			return name;
		}

		public void setTariffName(String name) {
			this.name = name;
		}

		public Set<String> getSubscriptions() {
			return subscriptions;
		}

		public void setSubscriptions(Set<String> subscriptions) {
			this.subscriptions = subscriptions;
		}

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

	public Map<String, TariffParameters> getTariffParameters() {
		Map<String, TariffParameters> tariffs = new HashMap<>();

		for (ConfigGroup item : getParameterSets(TariffParameters.SET_NAME)) {
			TariffParameters tariff = (TariffParameters) item;
			tariffs.put(tariff.name, tariff);
		}

		return tariffs;
	}
}
