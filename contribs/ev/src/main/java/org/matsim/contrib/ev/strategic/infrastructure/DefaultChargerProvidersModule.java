package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * This module configures the standard ChargerProviders that come with the
 * package.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultChargerProvidersModule extends AbstractChargerProviderModule {
	@Override
	protected void configureChargingStrategies() {
		bind(ChargerProvider.class).to(CompositeChargerProvider.class);

		bindChargerProvider().to(PersonChargerProvider.class);
		bindChargerProvider().to(FacilityChargerProvider.class);
		bindChargerProvider().to(PublicChargerProvider.class);
	}

	@Provides
	@Singleton
	CompositeChargerProvider provideChargerProvider(Set<ChargerProvider> delegates) {
		return new CompositeChargerProvider(delegates);
	}

	@Provides
	public PersonChargerProvider provideHomeChargerProvider(ChargingInfrastructureSpecification infrastructure,
			StrategicChargingConfigGroup config, Scenario scenario, ChargerAccess access) {
		return PersonChargerProvider.build(infrastructure, config.chargerSearchRadius, scenario, access);
	}

	@Provides
	public FacilityChargerProvider provideWorkChargerProvider(ChargingInfrastructureSpecification infrastructure,
			StrategicChargingConfigGroup config, Scenario scenario, ChargerAccess access) {
		return FacilityChargerProvider.build(infrastructure, config.chargerSearchRadius, scenario, access);
	}

	@Provides
	public PublicChargerProvider providePublicChargerProvider(Scenario scenario,
			ChargingInfrastructureSpecification infrastructure, StrategicChargingConfigGroup config,
			ChargerAccess access) {
		return PublicChargerProvider.create(scenario, infrastructure, access, config.chargerSearchRadius);
	}
}
