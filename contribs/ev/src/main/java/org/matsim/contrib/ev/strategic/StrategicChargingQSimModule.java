package org.matsim.contrib.ev.strategic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.ChargerReservationManager;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The QSim components for startegic electric vehicle charging (SEVC).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingQSimModule extends AbstractQSimModule {
	public StrategicChargingQSimModule() {
		super();
	}

	@Override
	protected void configureQSim() {
		bind(ChargingSlotProvider.class).to(StrategicChargingSlotProvider.class);
		bind(ChargingAlternativeProvider.class).to(StrategicChargingAlternativeProvider.class);

		addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargingPlanScoring.class);
	}

	@Provides
	@Singleton
	StrategicChargingSlotProvider provideStrategicOfflineSlotProvider(ChargingInfrastructure infrastructure,
			TimeInterpretation timeInterpretation, Scenario scenario, WithinDayEvConfigGroup config) {
		return new StrategicChargingSlotProvider(infrastructure,
				new ChargingSlotFinder(scenario, config.carMode));
	}

	@Provides
	StrategicChargingAlternativeProvider providePublicOnlineSlotProvider(ChargingInfrastructure infrastructure,
			ChargerProvider chargerProvider, Scenario scenario, StrategicChargingConfigGroup chargingConfig,
			ChargerAccess access,
			ChargerReservationManager reservationManager, TimeInterpretation timeInterpretation,
			CriticalAlternativeProvider criticalProvider) {
		return new StrategicChargingAlternativeProvider(scenario, chargerProvider, infrastructure, access,
				chargingConfig.onlineSearchStrategy,
				chargingConfig.useProactiveOnlineSearch, timeInterpretation, reservationManager, criticalProvider,
				chargingConfig.maximumAlternatives);
	}

	@Provides
	CriticalAlternativeProvider provideCriticalAlternativeProvider(QSim qsim, Network network,
			@Named("car") TravelTime travelTime,
			ChargerProvider chargerProvider, ChargingInfrastructure infrastructure,
			StrategicChargingConfigGroup config) {
		return new CriticalAlternativeProvider(qsim, network, travelTime, chargerProvider, infrastructure, config);
	}
}
