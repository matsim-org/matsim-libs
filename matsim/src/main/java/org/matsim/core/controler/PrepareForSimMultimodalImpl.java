package org.matsim.core.controler;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSimMultimodal;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class PrepareForSimMultimodalImpl implements PrepareForSim {

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;
	private final QSimConfigGroup qSimConfigGroup;

	@Inject
	PrepareForSimMultimodalImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider, QSimConfigGroup qSimConfigGroup) {
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
		ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
				new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						final PlanRouter planRouter = new PlanRouter(tripRouterProvider.get(), activityFacilities);
						return new PersonPrepareForSimMultimodal(planRouter, scenario, carNetwork );
					}
				});

		// though the vehicles should be created before creating a route, however,
		// as of now, it is not clear how to provide (store) vehicle id to the route afterwards. Amit may'17

		Map<String, VehicleType> modeVehicleTypes = getMode2VehicleType();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) { // go through with all plans ( when it was in population agent source, then going through only with selected plan was sufficient.) Amit May'17
				Map<String, Id<Vehicle>> seenModes = new HashMap<>();
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						if (qSimConfigGroup.getMainModes().contains(leg.getMode())) {// only simulated modes get vehicles
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							Id<Vehicle> vehicleId = null;
							if (route != null) {
								vehicleId = route.getVehicleId(); // may be null!
							} else {
								throw new RuntimeException("Route not found.");
							}

							if (!seenModes.keySet().contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location

								if (vehicleId == null) {
									vehicleId = createAutomaticVehicleId(person.getId(), leg.getMode(), route);
								}

								// so here we have a vehicle id, now try to find or create a physical vehicle:
								createAndAddVehicleIfNotPresent( vehicleId, modeVehicleTypes.get(leg.getMode()));
								seenModes.put(leg.getMode(), vehicleId);
							} else {
								if (vehicleId == null) {
									vehicleId = seenModes.get(leg.getMode());
									route.setVehicleId(vehicleId);
								}
							}

						}
					}
				}
			}
		}

		// create vehicles and add to scenario if using mode choice. Amit July'17
		createVehiclesInAdvance(modeVehicleTypes);

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

	private void createVehiclesInAdvance(final Map<String, VehicleType> modeVehicleTypes) {
		boolean isModeChoicePresent = false;
		Collection<StrategyConfigGroup.StrategySettings> strategySettings = scenario.getConfig().strategy().getStrategySettings();
		for (StrategyConfigGroup.StrategySettings strategySetting : strategySettings) {
			String name = strategySetting.getStrategyName();
			if ( name.equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.name())
					|| name.equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name())
					) {
				isModeChoicePresent = true;
			} else if (name.equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name())) {
				isModeChoicePresent = true;
				log.warn("Creating one vehicle corresponding to each network mode for every agent and parking it to the departure link. \n" +
						"If this is undesirable, then write a new PrepareForSim +" +
						"or, somehow get vehicles generation in your plan strategy.");
			}
		}

		if (isModeChoicePresent) {
			Collection<String> networkModes = scenario.getConfig().plansCalcRoute().getNetworkModes();
			for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
				for (String mode : networkModes) {
					Id<Vehicle> vehicleId = createAutomaticVehicleId(personId, mode, null);
					createAndAddVehicleIfNotPresent(vehicleId, modeVehicleTypes.get(mode));
				}
			}
		}
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

	private Vehicle createAndAddVehicleIfNotPresent(Id<Vehicle> vehicleId, VehicleType vehicleType) {
		// try to get vehicle from the vehicles container:
		Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);

		if ( vehicle==null ) {
			// if it was not found, next step depends on config:
			switch ( qSimConfigGroup.getVehiclesSource() ) {
				case defaultVehicle:
					vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vehicleType);
					scenario.getVehicles().addVehicle(vehicle);
					break;
				case modeVehicleTypesFromVehiclesData:
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

	private Id<Vehicle> createAutomaticVehicleId(Id<Person> personId, String mode, NetworkRoute route) {
		Id<Vehicle> vehicleId ;
		if (qSimConfigGroup.getUsePersonIdForMissingVehicleId()) {

			switch (qSimConfigGroup.getVehiclesSource()) {
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
