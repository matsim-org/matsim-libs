package org.matsim.contrib.ev.strategic;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.reservation.DistributedStrategicChargingReservationEngine;
import org.matsim.contrib.ev.strategic.reservation.StrategicChargingReservationEngine;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotProvider;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * The QSim components for startegic electric vehicle charging (SEVC).
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingQSimModule extends AbstractQSimModule {
	private final boolean isDsim;

	public StrategicChargingQSimModule() {
		this(false);
	}

	public StrategicChargingQSimModule(boolean isDsim) {
		super();
		this.isDsim = isDsim;
	}

	@Override
	protected void configureQSim() {
		bind(ChargingSlotProvider.class).to(StrategicChargingSlotProvider.class);
		bind(ChargingAlternativeProvider.class).to(StrategicChargingAlternativeProvider.class);

		addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargingPlanScoring.class);

		if (isDsim) {
			bind(DistributedStrategicChargingReservationEngine.class).in(Singleton.class);
			addQSimComponentBinding(EvModule.EV_COMPONENT).to(DistributedStrategicChargingReservationEngine.class);
		} else {
			addQSimComponentBinding(EvModule.EV_COMPONENT).to(StrategicChargingReservationEngine.class);
		}
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

	@Provides
	CriticalAlternativeProvider provideCriticalAlternativeProvider(Injector injector, Network network,
	                                                               @Named("car") TravelTime travelTime,
	                                                               ChargerProvider chargerProvider, ChargingInfrastructure infrastructure,
	                                                               StrategicChargingConfigGroup config) {
		if (isDsim) {
			return CriticalAlternativeProvider.noOp();
		}
		QSim qsim = injector.getInstance(QSim.class);
		return new CriticalAlternativeProvider(qsim, network, travelTime, chargerProvider, infrastructure, config);
	}

	@Provides
	@Singleton
	StrategicChargingReservationEngine provideStrategicChargingReservationEngine(Population population,
	                                                                             DistributedChargerReservationManager manager,
	                                                                             ChargingInfrastructureSpecification infrastructure, TimeInterpretation timeInterpretation,
	                                                                             ElectricFleet electricFleet, WithinDayEvConfigGroup config, EventsManager eventsManager) {
		return new StrategicChargingReservationEngine(
			population, manager, infrastructure, timeInterpretation, electricFleet, config.getCarMode(),
			eventsManager);
	}

//	@Provides
//	@Singleton
//	DistributedStrategicChargingReservationEngine provideDistributedStrategicChargingReservationEngine(
//			DistributedChargerReservationManager manager,
//			ChargingInfrastructureSpecification infrastructure,
//			TimeInterpretation timeInterpretation,
//			WithinDayEvConfigGroup config,
//			EventsManager eventsManager,
//			MobsimMessageCollector partitionTransfer) {
//		return new DistributedStrategicChargingReservationEngine(
//			manager, infrastructure, timeInterpretation, config.getCarMode(), eventsManager, partitionTransfer);
//	}
}
