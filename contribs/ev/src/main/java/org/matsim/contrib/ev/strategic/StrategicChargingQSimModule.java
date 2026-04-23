package org.matsim.contrib.ev.strategic;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.reservation.StrategicChargingReservationEngine;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * The QSim components for startegic electric vehicle charging (SEVC).
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingQSimModule extends AbstractQSimModule {

	@Override
	protected void configureQSim() {
		bind(ChargingSlotProvider.class).to(StrategicChargingSlotProvider.class);
		bind(ChargingAlternativeProvider.class).to(StrategicChargingAlternativeProvider.class);
		bind(CriticalAlternativeProvider.class);

		addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargingPlanScoring.class);

		bind(StrategicChargingReservationEngine.class).in(Singleton.class);
		addQSimComponentBinding(EvModule.EV_COMPONENT).to(StrategicChargingReservationEngine.class);
	}

	@Provides
	@Singleton
	StrategicChargingSlotProvider provideStrategicOfflineSlotProvider(ChargingInfrastructure infrastructure,
	                                                                  TimeInterpretation timeInterpretation, Scenario scenario, WithinDayEvConfigGroup config) {
		return new StrategicChargingSlotProvider(infrastructure,
			new ChargingSlotFinder(scenario, config.getCarMode()));
	}

	@Provides
	StrategicChargingAlternativeProvider providePublicOnlineSlotProvider(ChargingInfrastructure infrastructure,
	                                                                     ChargerProvider chargerProvider, Scenario scenario, StrategicChargingConfigGroup chargingConfig,
	                                                                     ChargerAccess access,
	                                                                     DistributedChargerReservationManager reservationManager, TimeInterpretation timeInterpretation,
	                                                                     CriticalAlternativeProvider criticalProvider) {
		return new StrategicChargingAlternativeProvider(scenario, chargerProvider, infrastructure, access,
			chargingConfig.getOnlineSearchStrategy(),
			chargingConfig.isUseProactiveOnlineSearch(), timeInterpretation, reservationManager, criticalProvider,
			chargingConfig.getMaximumAlternatives());
	}
}
