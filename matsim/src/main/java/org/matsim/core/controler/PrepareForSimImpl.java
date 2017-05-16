package org.matsim.core.controler;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

class PrepareForSimImpl implements PrepareForSim {

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;
	private final QSimConfigGroup qSimConfigGroup;

	@Inject
	PrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider, QSimConfigGroup qSimConfigGroup) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.qSimConfigGroup = qSimConfigGroup;
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

		// vehicles should be created (if not available) before a route is created. Amit May'17
		Map<String, VehicleType> modeVehicleTypes = getMode2VehicleType();

		for(Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Map<String,Id<Vehicle>> seenModes = new HashMap<>();
			for(PlanElement planElement : plan.getPlanElements()) {
				if( planElement instanceof Leg) {
					Leg leg =  (Leg) planElement;
					if (qSimConfigGroup.getMainModes().contains(leg.getMode())) {// only simulated modes get vehicles
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						Id<Vehicle> vehicleId = null ;
						if (route != null) {
							vehicleId = route.getVehicleId(); // may be null!
						}

						if (!seenModes.keySet().contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location

							if (vehicleId == null) {
								vehicleId = createAutomaticVehicleId(person, leg, route);
							}

							// so here we have a vehicle id, now try to find or create a physical vehicle:
							Vehicle vehicle = createVehicle(leg, vehicleId, modeVehicleTypes.get(leg.getMode()));
							seenModes.put(leg.getMode(),vehicleId);
						} else {
							if (vehicleId==null && route!=null) {
								vehicleId = seenModes.get(leg.getMode());
								route.setVehicleId( vehicleId );
							}
						}

					}
				}
			}
		}

		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(population, globalConfigGroup.getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities), scenario, net);
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

	private  Map<String, VehicleType> getMode2VehicleType(){
		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();
		switch ( this.qSimConfigGroup.getVehiclesSource() ) {
			case defaultVehicle:
				for (String mode : this.qSimConfigGroup.getMainModes()) {
					// initialize each mode with default vehicle type:
					VehicleType defaultVehicleType = VehicleUtils.getDefaultVehicleType();
					modeVehicleTypes.put(mode, defaultVehicleType);
						if( scenario.getVehicles().getVehicleTypes().get(defaultVehicleType.getId())==null) {
						scenario.getVehicles().addVehicleType(defaultVehicleType); // adding default vehicle type to vehicles container
					}
				}
				break;
			case modeVehicleTypesFromVehiclesData:
				for (String mode : qSimConfigGroup.getMainModes()) {
					VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get( Id.create(mode, VehicleType.class) ) ;
					Gbl.assertNotNull(vehicleType);
					modeVehicleTypes.put(mode, vehicleType );
				}
				break;
			case fromVehiclesData:
				// don't do anything
				break;
			default:
				throw new RuntimeException("not implemented yet.");
		}
		return modeVehicleTypes;
	}

	private Vehicle createVehicle(Leg leg, Id<Vehicle> vehicleId, VehicleType vehicleType) {
		// try to get vehicle from the vehicles container:
		Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);

		if ( vehicle==null ) {
			// if it was not found, next step depends on config:
			switch ( qSimConfigGroup.getVehiclesSource() ) {
				case defaultVehicle:
					// create vehicle but don't add it to the container. Amit Apr'17
					// I think, we should add vehicle to vehicles container. Amit May'17
					vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
					scenario.getVehicles().addVehicle(vehicle);
					break;
				case modeVehicleTypesFromVehiclesData:
					// if config says mode vehicles, then create and add it:
					vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
					scenario.getVehicles().addVehicle(vehicle);
					break;
				case fromVehiclesData:
					// otherwise complain:
					throw new IllegalStateException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
				default:
					// also complain when someone added another config option here:
					throw new RuntimeException("not implemented") ;
			}
		}
		return vehicle;
	}

	private Id<Vehicle> createAutomaticVehicleId(Person p, Leg leg, NetworkRoute route) {
		Id<Vehicle> vehicleId ;
		if (qSimConfigGroup.getUsePersonIdForMissingVehicleId()) {

			switch (qSimConfigGroup.getVehiclesSource()) {
				case defaultVehicle:
				case fromVehiclesData:
					vehicleId = Id.createVehicleId(p.getId());
					break;
				case modeVehicleTypesFromVehiclesData:
					if(!leg.getMode().equals(TransportMode.car)) {
						String vehIdString = p.getId().toString() + "_" + leg.getMode() ;
						vehicleId = Id.create(vehIdString, Vehicle.class);
					} else {
						vehicleId = Id.createVehicleId(p.getId());
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
