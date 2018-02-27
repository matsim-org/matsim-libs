package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.data.validator.DefaultDrtRequestValidator;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.routing.DrtMainModeIdentifier;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.name.Names;

public final class DrtModule extends AbstractModule {

	@Override
	public void install() {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(getConfig());
		bind(Fleet.class).toProvider(new FleetProvider(drtCfg.getVehiclesFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(DrtRequestValidator.class).to(DefaultDrtRequestValidator.class);
		bind(DepotFinder.class).to(NearestStartLinkAsDepot.class);
		bind(RebalancingStrategy.class).to(NoRebalancingStrategy.class);
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultDrtOptimizer.DRT_OPTIMIZER))
				.toInstance(timeCalculator -> new TimeAsTravelDisutility(timeCalculator));

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(DrtRoutingModule.class).asEagerSingleton();
				break;

			case stopbased:
				final Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new TransitScheduleReader(scenario2)
						.readFile(drtCfg.getTransitStopsFileUrl(getConfig().getContext()).getFile());
				bind(TransitSchedule.class).annotatedWith(Names.named(DrtConfigGroup.DRT_MODE))
						.toInstance(scenario2.getTransitSchedule());
				bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class).asEagerSingleton();
				addRoutingModuleBinding(DrtConfigGroup.DRT_MODE).to(StopBasedDrtRoutingModule.class).asEagerSingleton();
				break;

			default:
				throw new IllegalStateException();
		}
	}
}