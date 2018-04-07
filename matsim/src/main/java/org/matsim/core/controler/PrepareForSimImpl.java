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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
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

class PrepareForSimImpl implements PrepareForSim {

	private static Logger log = Logger.getLogger(PrepareForSim.class);

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;
	private final QSimConfigGroup qSimConfigGroup;
	private final FacilitiesConfigGroup facilitiesConfigGroup;

	@Inject
	PrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider, QSimConfigGroup qSimConfigGroup, FacilitiesConfigGroup facilitiesConfigGroup) {
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

		//matsim-724
		switch(this.facilitiesConfigGroup.getFacilitiesSource()){
			case none:
//				Gbl.assertIf( this.activityFacilities.getFacilities().isEmpty() );
				// I have at least one use case where people use the facilities as some kind
				// of database for stuff, but don't run the activities off them.  I have thus
				// disabled the above check.  We need to think about what we want to
				// do in such cases; might want to auto-generate our facilities as below
				// and _add_ them to the existing facilities.  kai, feb'18
				break;
			case fromFile:
			case setInScenario:
				Gbl.assertIf(! this.activityFacilities.getFacilities().isEmpty() );
				break;
			case onePerActivityLocationInPlansFile:
				FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(activityFacilities, facilitiesConfigGroup);
				facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(facilitiesConfigGroup.isAssigningLinksToFacilitiesIfMissing(), network);
				facilitiesFromPopulation.assignOpeningTimes(facilitiesConfigGroup.isAssigningOpeningTime(), scenario.getConfig().planCalcScore());
				facilitiesFromPopulation.run(population);
				break;
			default:
				throw new RuntimeException("Facilities source '"+this.facilitiesConfigGroup.getFacilitiesSource()+"' is not implemented yet.");
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
								throw new RuntimeException("Route not found.  Possible reason: leg did not have "
										+ "activities with locations at both ends (e.g. plan ends with leg).");
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
		// creating vehicles for every network mode. Amit Dec'17
		if (qSimConfigGroup.isCreatingVehiclesForAllNetworkModes()) {
			if (! qSimConfigGroup.getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.fromVehiclesData)){
				createVehiclesForEveyNetworkMode(modeVehicleTypes);
			} // don't create vehicle if vehicles are provided in vehicles file.
		} else {
			if (! qSimConfigGroup.getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.fromVehiclesData)){
			log.warn("Creating one vehicle corresponding to each network mode for every agent is disabled and " +
					"vehicleSource is not " + QSimConfigGroup.VehiclesSource.fromVehiclesData.toString() + ". " +
					"\n Simulation should run without a problem if it does not include mode choice. " +
					"Please provide vehicles file or set 'creatingVehiclesForAllNetworkModes' to true if this is not the case.");
			}
		}

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

	private void createVehiclesForEveyNetworkMode(final Map<String, VehicleType> modeVehicleTypes) {
		// yyyy maybe better just take the modes from qsim.mainMode???  kai, dec'17
		// agree. A network mode may not be main mode (e.g. ride) and in this case,
		// creating vehicles for every network mode is not required. IK, AA (Apr'18).

//		boolean isModeChoicePresent = false;
//		Collection<StrategyConfigGroup.StrategySettings> strategySettings = scenario.getConfig().strategy().getStrategySettings();
//		for (StrategyConfigGroup.StrategySettings strategySetting : strategySettings) {
//			String name = strategySetting.getStrategyName();
//			if ( name.equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.name())
//					|| name.equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name())
//					) {
//				isModeChoicePresent = true;
//			} else if (name.equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name())) {
//				isModeChoicePresent = true;
//				log.warn("Creating one vehicle corresponding to each network mode for every agent and parking it to the departure link. \n" +
//						"If this is undesirable, then write a new PrepareForSim +" +
//						"or, somehow get vehicles generation in your plan strategy.");
//			} else if ( ! Arrays.stream( DefaultPlanStrategiesModule.DefaultStrategy.values() ).anyMatch( e -> e.name().equals(strategySetting.getStrategyName()))
//					&&
//					! Arrays.stream( DefaultPlanStrategiesModule.DefaultSelector.class.getFields() ).anyMatch( e -> e.getName().equals(strategySetting.getStrategyName()))
//					){
//				log.warn("Vehicles are created internally for all re-planning strategies. However, "+strategySetting.getStrategyName()+" is not one of the recognized strategy." +
//						" \n Simulation should run without a problem if it does not include mode choice. Please provide vehicles file is this is not the case.");
//			}
//		}

//		if (isModeChoicePresent) {
//			Collection<String> networkModes = scenario.getConfig().plansCalcRoute().getNetworkModes();
			for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
//				for (String mode : networkModes) {
				for (String mode : scenario.getConfig().qsim().getMainModes()) {
					Id<Vehicle> vehicleId = createAutomaticVehicleId(personId, mode, null);
					createAndAddVehicleIfNotPresent(vehicleId, modeVehicleTypes.get(mode));
				}
			}
//		}
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
