package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.validator.*;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.depot.*;
import org.matsim.contrib.drt.optimizer.rebalancing.*;
import org.matsim.contrib.drt.routing.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import com.google.inject.*;
import com.google.inject.name.*;

public final class DrtModule extends AbstractModule {

	@Override
	public void install() {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(getConfig());

		bind(Fleet.class).toProvider(DefaultDrtFleetProvider.class).asEagerSingleton();
		bind(DrtRequestValidator.class).to(DefaultDrtRequestValidator.class);
		bind(DepotFinder.class).to(NearestStartLinkAsDepot.class);
		bind(RebalancingStrategy.class).to(SendToStartLinkStrategy.class);
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultDrtOptimizer.DRT_OPTIMIZER))
				.toInstance(timeCalculator -> new TimeAsTravelDisutility(timeCalculator));

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(DrtRoutingModule.class).asEagerSingleton();
				break;

			case stationbased:
				final Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new TransitScheduleReader(scenario2)
						.readFile(drtCfg.getTransitStopsFileUrl(getConfig().getContext()).getFile());
				bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
						.toInstance(scenario2.getTransitSchedule());
				addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(StopBasedDrtRoutingModule.class).asEagerSingleton();
				break;

			default:
				throw new IllegalStateException();
		}
	}

	public static final class DefaultDrtFleetProvider implements Provider<Fleet> {
		@Inject
		@Named(DvrpModule.DVRP_ROUTING)
		Network network;
		@Inject
		Config config;
		@Inject
		DrtConfigGroup drtCfg;

		@Override
		public Fleet get() {
			FleetImpl fleet = new FleetImpl();
			new VehicleReader(network, fleet).parse(drtCfg.getVehiclesFileUrl(config.getContext()));
			return fleet;
		}
	}
}