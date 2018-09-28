package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.data.validator.DefaultDrtRequestValidator;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingModule;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.routing.DefaultAccessEgressStopFinder;
import org.matsim.contrib.drt.routing.DefaultDrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtMainModeIdentifier;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
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

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class DrtModule extends AbstractModule {
	@Inject
	private DrtConfigGroup drtCfg;

	@Override
	public void install() {
		String mode = drtCfg.getMode();
		install(FleetProvider.createModule(mode, drtCfg.getVehiclesFileUrl(getConfig().getContext())));
		bind(Fleet.class).annotatedWith(Drt.class).to(Key.get(Fleet.class, Names.named(mode))).asEagerSingleton();

		bind(DrtRequestValidator.class).to(DefaultDrtRequestValidator.class);
		bind(DepotFinder.class).to(NearestStartLinkAsDepot.class);
		bind(TravelDisutilityFactory.class).annotatedWith(Names.named(DefaultDrtOptimizer.DRT_OPTIMIZER))
				.toInstance(TimeAsTravelDisutility::new);

		if (MinCostFlowRebalancingParams.isRebalancingEnabled(drtCfg.getMinCostFlowRebalancing())) {
			install(new MinCostFlowRebalancingModule());
		} else {
			bind(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}

		bind(InsertionCostCalculator.PenaltyCalculator.class).to(drtCfg.isRequestRejection() ?
				InsertionCostCalculator.RejectSoftConstraintViolations.class :
				InsertionCostCalculator.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(TransportMode.drt).to(DrtRoutingModule.class);
				break;

			case stopbased:
				final Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new TransitScheduleReader(scenario2).readFile(
						drtCfg.getTransitStopsFileUrl(getConfig().getContext()).getFile());
				bind(TransitSchedule.class).annotatedWith(Names.named(TransportMode.drt))
						.toInstance(scenario2.getTransitSchedule());
				bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class).asEagerSingleton();
				bind(DrtRoutingModule.class);
				addRoutingModuleBinding(TransportMode.drt).to(StopBasedDrtRoutingModule.class);
				bind(AccessEgressStopFinder.class).to(DefaultAccessEgressStopFinder.class).asEagerSingleton();
				break;

			default:
				throw new IllegalStateException();
		}

		bind(DefaultDrtRouteUpdater.class).asEagerSingleton();
		bind(DrtRouteUpdater.class).to(DefaultDrtRouteUpdater.class).asEagerSingleton();
		addControlerListenerBinding().to(DrtRouteUpdater.class);
	}
}
