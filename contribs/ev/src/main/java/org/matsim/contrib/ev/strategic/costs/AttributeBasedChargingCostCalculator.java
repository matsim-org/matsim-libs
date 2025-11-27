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
	static public final String DYNAMIC_ENERGY_COST_CHARGER_ATTRIBUTE = "sevc:dynamicCostPerEnergy_kWh";
	static public final String RESERVATION_COST_CHARGER_ATTRIBUTE = "secv:costPerReservation";

	private final ChargingInfrastructureSpecification infrastructure;

	private final IdMap<Charger, Item> cache = new IdMap<>(Charger.class);

	private record Item(double use, double duration_min, double energy_kWh, double costPerBlockingDuration_min,
			double blockingDuration_min, DynamicEnergyCosts dynamicEnergyCosts, double costPerReservation) {
	}

	public AttributeBasedChargingCostCalculator(ChargingInfrastructureSpecification infrastructure) {
		this.infrastructure = infrastructure;
	}

	private Item getItem(Id<Charger> chargerId) {
		Item item = cache.get(chargerId);

		if (item == null) {
			ChargerSpecification charger = infrastructure.getChargerSpecifications().get(chargerId);

			double costPerUse = getCostPerUse(charger);
			double costPerDuration_min = getCostPerDuration_min(charger);
			double costPerEnergy_kWh = getCostPerEnergy_kWh(charger);
			double costPerBlockingDuration_min = getCostPerBlockingDuration_min(charger);
			double blockingDuration_min = getBlockingDuration_min(charger);
			double costPerReservation = getCostPerReservation(charger);

			DynamicEnergyCosts dynamicEnergyCosts_kWh = getDynamicEnergyCost_kWh(charger);

			item = new Item(costPerUse, costPerDuration_min, costPerEnergy_kWh, costPerBlockingDuration_min,
					blockingDuration_min, dynamicEnergyCosts_kWh, costPerReservation);

			cache.put(chargerId, item);
		}

		return item;
	}

	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> chargerId, double startTime, double duration,
			double energy) {
		Item item = getItem(chargerId);

		double blockingDuration_min = Math.max(duration / 60.0 - item.blockingDuration_min, 0.0);
		double energy_kWh = EvUnits.J_to_kWh(energy);

		return duration / 60.0 * item.duration_min // duration
				+ (item.dynamicEnergyCosts == null ? 0.0
						: item.dynamicEnergyCosts.calculate(startTime, duration, energy_kWh)) // dynamic costs
				+ blockingDuration_min * item.costPerBlockingDuration_min // blocking duration
				+ energy_kWh * item.energy_kWh // energy
				+ item.use; // per use
	}

	@Override
	public double calculateReservationCost(Id<Person> personId, Id<Charger> chargerId, double duration) {
		Item item = getItem(chargerId);
		return item.costPerReservation;
	}

	/**
	 * Sets the cost structurg be for the charger.
	 */
	static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
			double costPerDuration_kWh) {
		setChargingCosts(charger, costPerUse, costPerEnergy_kWh, costPerDuration_kWh, 0.0, 0.0, 0.0);
	}

	/**
	 * Sets the cost structure for the charger. The blocking costs are charged
	 * additionally for any duration that exceeds the blocking duration.
	 */
	static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
			double costPerDuration_min, double costPerBlockingDuration_min, double blockingDuration_min,
			double costPerReservation) {
		charger.getAttributes().putAttribute(USE_COST_CHARGER_ATTRIBUTE, costPerUse);
		charger.getAttributes().putAttribute(ENERGY_COST_CHARGER_ATTRIBUTE, costPerEnergy_kWh);
		charger.getAttributes().putAttribute(DURATION_COST_CHARGER_ATTRIBUTE, costPerDuration_min);
		charger.getAttributes().putAttribute(BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE, costPerBlockingDuration_min);
		charger.getAttributes().putAttribute(BLOCKING_DURATION_CHARGER_ATTRIBUTE, blockingDuration_min);
		charger.getAttributes().putAttribute(RESERVATION_COST_CHARGER_ATTRIBUTE, costPerReservation);
	}

	static public void setCostPerUse(ChargerSpecification charger, double costPerUse) {
		charger.getAttributes().putAttribute(USE_COST_CHARGER_ATTRIBUTE, costPerUse);
	}

	static public double getCostPerUse(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(USE_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public void setCostPerDuration_min(ChargerSpecification charger, double costPerDuration_min) {
		charger.getAttributes().putAttribute(DURATION_COST_CHARGER_ATTRIBUTE, costPerDuration_min);
	}

	static public double getCostPerDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(DURATION_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public void setCostPerEnergy_kWh(ChargerSpecification charger, double costPerEnergy_kWh) {
		charger.getAttributes().putAttribute(ENERGY_COST_CHARGER_ATTRIBUTE, costPerEnergy_kWh);
	}

	static public double getCostPerEnergy_kWh(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(ENERGY_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public void setCostPerBlockingDuration_min(ChargerSpecification charger,
			double costPerBlockingDuration_min) {
		charger.getAttributes().putAttribute(BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE, costPerBlockingDuration_min);
	}

	static public double getCostPerBlockingDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(BLOCKING_DURATION_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public void setBlockingDuration_min(ChargerSpecification charger, double blockingDuration_min) {
		charger.getAttributes().putAttribute(BLOCKING_DURATION_CHARGER_ATTRIBUTE, blockingDuration_min);
	}

	static public double getBlockingDuration_min(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(BLOCKING_DURATION_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}

	static public void setDynamicEnergyCost_kWh(ChargerSpecification charger, DynamicEnergyCosts dynamicCosts_kWh) {
		charger.getAttributes().putAttribute(DYNAMIC_ENERGY_COST_CHARGER_ATTRIBUTE,
				DynamicEnergyCosts.write(dynamicCosts_kWh));
	}

	static public DynamicEnergyCosts getDynamicEnergyCost_kWh(ChargerSpecification charger) {
		String value = (String) charger.getAttributes().getAttribute(DYNAMIC_ENERGY_COST_CHARGER_ATTRIBUTE);
		return value == null ? null : DynamicEnergyCosts.parse(value);
	}

	static public double getCostPerReservation(ChargerSpecification charger) {
		Double value = (Double) charger.getAttributes().getAttribute(RESERVATION_COST_CHARGER_ATTRIBUTE);
		return value == null ? 0.0 : value;
	}
}
