package org.matsim.contrib.ev.withinday;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicles;

/**
 * This module manages the QSim components for within-day electric vehicle
 * charging (WEVC).
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvQSimModule extends AbstractQSimModule {
	static public final String ROAD_MODE_BINDING = "ev:road";
	static public final String WALK_MODE_BINDING = "ev:walk";

	@Override
	protected void configureQSim() {
		WithinDayEvConfigGroup config = WithinDayEvConfigGroup.get(getConfig());

		if (getConfig().controller().getMobsim().equals("dsim")) {
			addQSimComponentBinding(EvModule.EV_COMPONENT).to(DistributedWithinDayEvEngine.class);
		} else {
			addQSimComponentBinding(EvModule.EV_COMPONENT).to(WithinDayEvEngine.class);
			addMobsimScopeEventHandlerBinding().to(WithinDayEvEngine.class);
		}

		bind(Key.get(RoutingModule.class, Names.named(ROAD_MODE_BINDING)))
			.to(Key.get(RoutingModule.class, Names.named(config.getCarMode())));
		bind(Key.get(RoutingModule.class, Names.named(WALK_MODE_BINDING)))
			.to(Key.get(RoutingModule.class, Names.named(config.getWalkMode())));

		bind(ChargingSlotProvider.class).toInstance(ChargingSlotProvider.NOOP);
		bind(ChargingAlternativeProvider.class).toInstance(ChargingAlternativeProvider.NOOP);
	}

	@Provides
	@Singleton
	WithinDayEvEngine provideEvPlanningEngine(Netsim netsim, TimeInterpretation timeInterpretation, ElectricFleet electricFleet,
	                                          ChargingAlternativeProvider alternativeProvider, ChargingSlotProvider slotProvider,
	                                          EventsManager eventsManager,
	                                          ChargingScheduler chargingScheduler, WithinDayEvConfigGroup config, Vehicles vehicles,
	                                          QVehicleFactory qVehicleFactory, Scenario scenario,
	                                          WithinDayChargingStrategy.Factory chargingStrategyFactory, ChargingInfrastructure chargingInfrastructure) {
		return new WithinDayEvEngine(config, netsim, timeInterpretation, electricFleet, alternativeProvider, slotProvider,
			eventsManager, chargingScheduler, vehicles, qVehicleFactory, scenario, chargingStrategyFactory, chargingInfrastructure);
	}

	@Provides
	@Singleton
	ChargingScheduler provideChargingScheduler(Population population, TimeInterpretation timeInterpretation,
	                                           ActivityFacilities facilities, @Named(ROAD_MODE_BINDING) RoutingModule roadRoutingModule,
	                                           @Named(WALK_MODE_BINDING) RoutingModule walkRoutingModule, Network network) {
		return new ChargingScheduler(population.getFactory(), timeInterpretation, facilities, roadRoutingModule,
			walkRoutingModule, network);
	}

	@Provides
	@Singleton
	WithinDayChargingStrategy.Factory provideWithinDayChargingStrategyFactory() {
		return new WithinDayChargingStrategy.Factory();
	}
}
