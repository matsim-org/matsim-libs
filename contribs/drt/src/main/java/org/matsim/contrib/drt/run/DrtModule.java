package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizerProvider;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public final class DrtModule extends AbstractModule {



	@Override
	public void install() {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(getConfig());

		bind(Fleet.class).toProvider(DefaultDrtFleetProvider.class).asEagerSingleton();
		install(new DvrpModule(DrtControlerCreator.createModuleForQSimPlugin(DefaultDrtOptimizerProvider.class),
				DrtOptimizer.class));
		switch (drtCfg.getOperationalScheme()) {
		case door2door:

			addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(DrtRoutingModule.class).asEagerSingleton();
			break;

		case stationbased: {
			final Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new TransitScheduleReader(scenario2)
					.readFile(drtCfg.getTransitStopsFileUrl(getConfig().getContext()).getFile());
			bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
					.toInstance(scenario2.getTransitSchedule());
			addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(StopBasedDrtRoutingModule.class).asEagerSingleton();
		}
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