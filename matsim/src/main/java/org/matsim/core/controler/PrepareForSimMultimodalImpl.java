package org.matsim.core.controler;


import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.core.population.algorithms.PersonPrepareForSimMultimodal;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;

public final class PrepareForSimMultimodalImpl implements PrepareForSim {

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;

	@Inject
	PrepareForSimMultimodalImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
	}


	@Override
	public void run() {
		{
			log.warn("===") ;
			for ( Link link : network.getLinks().values() ) {
				if ( !link.getAllowedModes().contains( TransportMode.car ) ) {
					log.warn("link that does not allow car: " + link.toString() ) ;
				}
			}
			log.warn("---") ;
		}
		
		/*
		 * Create single-mode network here and hand it over to PersonPrepareForSim. Otherwise, each instance would create its
		 * own single-mode network. However, this assumes that the main mode is car - which PersonPrepareForSim also does. Should
		 * be probably adapted in a way that other main modes are possible as well. cdobler, oct'15.
		 */
		final Network carNetwork ; // for postal address
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. Create car-only network which is handed over to PersonPrepareForSim.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			carNetwork  = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<>();
			modes.add(TransportMode.car);
			filter.filter(carNetwork , modes);
		} else {
			carNetwork  = network;
		}

		{
			log.warn("---") ;
			int ii = 0 ;
			for ( Link link : carNetwork.getLinks().values() ) {
				if ( ii < 10 ) {
					ii++ ;
					log.warn( link.getId() + "; " + link.getAllowedModes() );
				}
			}
			log.warn("===") ;
		}
	
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(population, globalConfigGroup.getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						final PlanRouter planRouter = new PlanRouter(tripRouterProvider.get(), activityFacilities);
						return new PersonPrepareForSimMultimodal(planRouter, scenario, carNetwork );
					}
				});

		if (scenario instanceof Lockable) {
			((Lockable)scenario).setLocked();
			// see comment in ScenarioImpl. kai, sep'14
		}

		if (population instanceof Lockable) {
			((Lockable) population).setLocked();
		}
		
		if ( network instanceof Lockable ) {
			((Lockable) network).setLocked();
		}
		
		// (yyyy means that if someone replaces prepareForSim and does not add the above lines, the containers are not locked.  kai, nov'16)

	}
}
