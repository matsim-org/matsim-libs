package org.matsim.contrib.drt.run;

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
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
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Inject;

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
		bind(DvrpModes.key(Fleet.class, mode)).toProvider(new FleetProvider(drtCfg.getVehiclesFile()))
				.asEagerSingleton();
		bind(Fleet.class).annotatedWith(Drt.class).to(DvrpModes.key(Fleet.class, mode));

		bind(DvrpModes.key(PassengerRequestValidator.class, mode)).to(DefaultPassengerRequestValidator.class);
		bind(DepotFinder.class).to(NearestStartLinkAsDepot.class);
		bind(TravelDisutilityFactory.class).annotatedWith(Drt.class).toInstance(TimeAsTravelDisutility::new);

		if (MinCostFlowRebalancingParams.isRebalancingEnabled(drtCfg.getMinCostFlowRebalancing())) {
			install(new MinCostFlowRebalancingModule());
		} else {
			bind(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}

		bind(InsertionCostCalculator.PenaltyCalculator.class).to(drtCfg.isRequestRejection() ?
				InsertionCostCalculator.RejectSoftConstraintViolations.class :
				InsertionCostCalculator.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		bind(MainModeIdentifier.class).to(DrtMainModeIdentifier.class).asEagerSingleton();

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(drtCfg.getMode()).to(DrtRoutingModule.class);//not singleton
				break;

			case stopbased:
				bind(TransitSchedule.class).annotatedWith(Drt.class)
						.toInstance(readTransitSchedule(drtCfg.getTransitStopsFileUrl(getConfig().getContext())));

				bind(DrtRoutingModule.class);//not singleton
				addRoutingModuleBinding(drtCfg.getMode()).to(StopBasedDrtRoutingModule.class);//not singleton
				bind(AccessEgressStopFinder.class).to(DefaultAccessEgressStopFinder.class).asEagerSingleton();
				break;

			default:
				throw new IllegalStateException();
		}

		bind(DrtRouteUpdater.class).to(DefaultDrtRouteUpdater.class).asEagerSingleton();
		addControlerListenerBinding().to(DrtRouteUpdater.class);
	}

	static TransitSchedule readTransitSchedule(URL url) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url);
		return scenario.getTransitSchedule();
	}
}
