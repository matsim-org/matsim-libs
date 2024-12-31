package org.matsim.contrib.ev.strategic.costs;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.access.SubscriptionRegistry;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ChargingCostModule extends AbstractModule {
	@Override
	public void install() {
		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(getConfig());

		if (config != null) {
			if (config.costs == null) {
				bind(ChargingCostCalculator.class).toInstance(new NoChargingCostCalculator());
			} else if (config.costs instanceof DefaultChargingCostsParameters) {
				bind(ChargingCostCalculator.class).to(DefaultChargingCostCalculator.class);
			} else if (config.costs instanceof AttributeBasedChargingCostsParameters) {
				bind(ChargingCostCalculator.class).to(AttributeBasedChargingCostCalculator.class);
			} else if (config.costs instanceof TariffBasedChargingCostsParameters) {
				bind(ChargingCostCalculator.class).to(TariffBasedChargingCostCalculator.class);
			}
		}
	}

	@Singleton
	@Provides
	DefaultChargingCostCalculator provideDefaultChargingCostCalculator(StrategicChargingConfigGroup config) {
		return new DefaultChargingCostCalculator((DefaultChargingCostsParameters) config.costs);
	}

	@Singleton
	@Provides
	AttributeBasedChargingCostCalculator provideDefaultChargingCostCalculator(
			ChargingInfrastructureSpecification infrastructure) {
		return new AttributeBasedChargingCostCalculator(infrastructure);
	}

	@Singleton
	@Provides
	TariffBasedChargingCostCalculator provideTariffBasedChargingCostCalculator(
			ChargingInfrastructureSpecification infrastructure, Population population,
			SubscriptionRegistry subscriptions, StrategicChargingConfigGroup config) {
		TariffBasedChargingCostsParameters parameters = (TariffBasedChargingCostsParameters) config.costs;
		return new TariffBasedChargingCostCalculator(parameters, infrastructure, population, subscriptions);
	}
}
