package org.matsim.contrib.ev.strategic.costs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;

/**
 * This cost calculator implementation makes use of the charger attributes to
 * obtain prices per duration, kWh, etc.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AttributeBasedChargingCostCalculator implements ChargingCostCalculator {
	static public final String USE_COST_CHARGER_ATTRIBUTE = "secv:costPerUse";
	static public final String ENERGY_COST_CHARGER_ATTRIBUTE = "secv:costPerEnergy_kWh";
	static public final String DURATION_COST_CHARGER_ATTRIBUTE = "secv:costPerDuration_min";
	static public final String BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE = "secv:costPerBlockingDuration_min";
	static public final String BLOCKING_DURATION_CHARGER_ATTRIBUTE = "secv:blockingDuration_min";

	private final ChargingInfrastructureSpecification infrastructure;

	private final IdMap<Charger, Item> cache = new IdMap<>(Charger.class);

	private record Item(double use, double duration_min, double energy_kWh, double costPerBlockingDuration_min,
			double blockingDuration_min) {
	}

	public AttributeBasedChargingCostCalculator(ChargingInfrastructureSpecification infrastructure) {
		this.infrastructure = infrastructure;
	}

	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> chargerId, double duration, double energy) {
		Item item = cache.get(chargerId);
		if (item == null) {
			ChargerSpecification charger = infrastructure.getChargerSpecifications().get(chargerId);

			double costPerUse = getCostPerUse(charger);
			double costPerDuration_min = getCostPerDuration_min(charger);
			double costPerEnergy_kWh = getCostPerEnergy_kWh(charger);
			double costPerBlockingDuration_min = getCostPerBlockingDuration_min(charger);
			double blockingDuration_min = getBlockingDuration_min(charger);

			item = new Item(costPerUse, costPerDuration_min, costPerEnergy_kWh, costPerBlockingDuration_min,
					blockingDuration_min);
			cache.put(chargerId, item);
		}

		double blockingDuration_min = Math.max(duration / 60.0 - item.blockingDuration_min, 0.0);
		return duration / 60.0 * item.duration_min + blockingDuration_min * item.costPerBlockingDuration_min
				+ EvUnits.J_to_kWh(energy) * item.energy_kWh + item.use;
	}

	/**
	 * Sets the cost structure for the charger.
	 */
	static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
			double costPerDuration_kWh) {
		setChargingCosts(charger, costPerUse, costPerEnergy_kWh, costPerDuration_kWh, 0.0, 0.0);
	}

	/**
	 * Sets the cost structure for the charger. The blocking costs are charged
	 * additionally for any duration that exceeds the blocking duration.
	 */
	static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
			double costPerDuration_min, double costPerBlockingDuration_min, double blockingDuration_min) {
		charger.getAttributes().putAttribute(USE_COST_CHARGER_ATTRIBUTE, costPerUse);
		charger.getAttributes().putAttribute(ENERGY_COST_CHARGER_ATTRIBUTE, costPerEnergy_kWh);
		charger.getAttributes().putAttribute(DURATION_COST_CHARGER_ATTRIBUTE, costPerDuration_min);
		charger.getAttributes().putAttribute(BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE, costPerBlockingDuration_min);
		charger.getAttributes().putAttribute(BLOCKING_DURATION_CHARGER_ATTRIBUTE, blockingDuration_min);
	}

	static public double getCostPerUse(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(USE_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public double getCostPerDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(DURATION_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public double getCostPerEnergy_kWh(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(ENERGY_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public double getCostPerBlockingDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public double getBlockingDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(BLOCKING_DURATION_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}
}
