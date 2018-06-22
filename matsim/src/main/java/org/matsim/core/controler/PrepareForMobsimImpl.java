package org.matsim.core.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class PrepareForMobsimImpl implements PrepareForMobsim {
	// I think it is ok to have this public final.  Since one may want to use it as a delegate.  kai, may'18
	// yyyyyy but how should that work with a non-public constructor? kai, jun'18
	
	// yyyy There is currently a lot of overlap between PrepareForSimImpl and PrepareForMobsimImpl.
	// This should be removed.  kai, jun'18
	
	private static Logger log = Logger.getLogger(PrepareForMobsimImpl.class);
	
	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;
	private final QSimConfigGroup qSimConfigGroup;
	private final FacilitiesConfigGroup facilitiesConfigGroup;
	
	@Inject
	PrepareForMobsimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network,
				Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider,
				QSimConfigGroup qSimConfigGroup, FacilitiesConfigGroup facilitiesConfigGroup) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.qSimConfigGroup = qSimConfigGroup;
		this.facilitiesConfigGroup = facilitiesConfigGroup;
	}
	
	
	@Override
	public void run() {
		/*
		 * Create single-mode network here and hand it over to PersonPrepareForSim. Otherwise, each instance would create its
		 * own single-mode network. However, this assumes that the main mode is car - which PersonPrepareForSim also does. Should
		 * be probably adapted in a way that other main modes are possible as well. cdobler, oct'15.
		 */
		final Network net;
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. Create car-only network which is handed over to PersonPrepareForSim.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			net = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		} else {
			net = network;
		}
		
		// make sure all routes are calculated.
		ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
				new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities), scenario, net);
					}
				});
		
		// though the vehicles should be created before creating a route, however,
		// as of now, it is not clear how to provide (store) vehicle id to the route afterwards. Amit may'17
		
		// yyyyyy from a behavioral perspective, the vehicle must be somehow linked to
		// the person (maybe via the household).  We also have the problem that it
		// is not possible to switch to a mode that was not in the initial plans ...
		// since there will be no vehicle for it.  Needs to be fixed somehow.  kai, feb'18
		
		// yy Could now set the vehicle IDs in the routes.  But can as well also do this later (currently in PopulationAgentSource).  kai, jun'18
		
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
		
		if (activityFacilities instanceof  Lockable) {
			((Lockable) activityFacilities).setLocked();
		}
		
		// (yyyy means that if someone replaces prepareForSim and does not add the above lines, the containers are not locked.  kai, nov'16)
	}
	
	public static Id<Vehicle> createAndSetAutomaticVehicleId(Id<Person> personId, String mode, NetworkRoute route, QSimConfigGroup config) {
		// yyyy cf. PopulationAgentSource.createAutomaticVehicleId (gone now)
		
		Id<Vehicle> vehicleId ;
		if (config.getUsePersonIdForMissingVehicleId()) {
			
			// yyyy my strong preference would be to do away with this "car_" exception and to just
			// use <mode>_personId across the board.  kai, may'18
			
			switch (config.getVehiclesSource()) {
				case defaultVehicle:
				case fromVehiclesData:
					vehicleId = Id.createVehicleId(personId);
					break;
				case modeVehicleTypesFromVehiclesData:
					if(! mode.equals(TransportMode.car)) {
						String vehIdString = personId.toString() + "_" + mode ;
						vehicleId = Id.create(vehIdString, Vehicle.class);
					} else {
						vehicleId = Id.createVehicleId(personId);
					}
					break;
				default:
					throw new RuntimeException("not implemented") ;
			}
			if(route!=null) route.setVehicleId(vehicleId);
		} else {
			throw new IllegalStateException("Found a network route without a vehicle id.");
		}
		return vehicleId;
	}
}
