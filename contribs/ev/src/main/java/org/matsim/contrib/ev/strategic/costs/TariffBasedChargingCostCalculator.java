package org.matsim.contrib.ev.strategic.costs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.contrib.ev.strategic.access.SubscriptionRegistry;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters.TariffParameters;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * This cost calculator implementation calculates charging costs based on a
 * configured list of tariffs. Each charger can have one or more tariffs, which,
 * in turn, are either available to everybody or only to persons with specific
 * subscriptions. Among the tariffs available at a charger, a person will always
 * choose the cheapest available option.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class TariffBasedChargingCostCalculator implements ChargingCostCalculator {
	static public final String TARIFFS_CHARGER_ATTRIBUTE = "sevc:tariffs";

	private final Map<String, TariffParameters> parameters;
	private final IdMap<Charger, List<TariffParameters>> cache = new IdMap<>(Charger.class);

	private final Population population;
	private final ChargingInfrastructureSpecification infrastructure;
	private final SubscriptionRegistry subscriptions;

	public TariffBasedChargingCostCalculator(TariffBasedChargingCostsParameters parameters,
			ChargingInfrastructureSpecification infrastructure, Population population,
			SubscriptionRegistry subscriptions) {
		this.parameters = parameters.getTariffParameters();
		this.infrastructure = infrastructure;
		this.subscriptions = subscriptions;
		this.population = population;
	}

	@Override
	public double calculateChargingCost(Id<Person> personId, Id<Charger> chargerId, double duration, double energy) {
		Person person = population.getPersons().get(personId);

		List<TariffParameters> tariffs = cache.get(chargerId);
		if (tariffs == null) {
			tariffs = getChargerTariffs(chargerId);
			cache.put(chargerId, tariffs);
		}

		double best = Double.POSITIVE_INFINITY;
		for (TariffParameters tariff : tariffs) {
			if (tariff.subscriptions.size() == 0 || Sets
					.intersection(tariff.subscriptions, subscriptions.getPersonSubscriptions(person)).size() > 0) {
				double blockingDuration_min = Math.max(duration / 60.0 - tariff.blockingDuration_min, 0.0);

				best = Math.min(best,
						duration / 60.0 * tariff.costPerDuration_min
								+ blockingDuration_min * tariff.costPerBlockingDuration_min +
								+EvUnits.J_to_kWh(energy) * tariff.costPerEnergy_kWh + tariff.costPerUse);
			}
		}

		Preconditions.checkState(Double.isFinite(best),
				String.format("No viable tariff found for person %s at charger %s", personId, chargerId));

		return best;
	}

	private List<TariffParameters> getChargerTariffs(Id<Charger> chargerId) {
		String raw = (String) infrastructure.getChargerSpecifications().get(chargerId).getAttributes()
				.getAttribute(TARIFFS_CHARGER_ATTRIBUTE);

		if (raw == null) {
			return Collections.emptyList();
		} else {
			List<TariffParameters> tariffs = new ArrayList<>();

			for (String name : raw.split(",")) {
				TariffParameters tariff = parameters.get(name.trim());

				if (tariff == null) {
					throw new IllegalStateException(
							String.format("Tariff %s of charger %s has not been defined", name, chargerId.toString()));
				} else {
					tariffs.add(tariff);
				}
			}

			if (tariffs.size() == 0) {
				return Collections.emptyList();
			} else {
				return tariffs;
			}
		}
	}

	/**
	 * Adds a tariff to a charger.
	 */
	static public void addTariff(ChargerSpecification charger, String tariff) {
		Set<String> tariffs = StrategicChargingUtils.readList(charger, TARIFFS_CHARGER_ATTRIBUTE);
		tariffs.add(tariff);
		StrategicChargingUtils.writeList(charger, TARIFFS_CHARGER_ATTRIBUTE, tariffs);
	}

	/**
	 * Retrieve the list of tariffs for a charger.
	 */
	static public Set<String> getTariffs(ChargerSpecification charger) {
		return StrategicChargingUtils.readList(charger, TARIFFS_CHARGER_ATTRIBUTE);
	}
}
