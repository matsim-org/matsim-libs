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
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class PrepareForSimImpl implements PrepareForSim, PrepareForMobsim {
	// I think it is ok to have this public final.  Since one may want to use it as a delegate.  kai, may'18
	// but how should that work with a non-public constructor? kai, jun'18
	// Well, I guess it can be injected as well?!
	// bind( PrepareForSimImpl.class ) ;
	// bind( PrepareForSim.class ).to( MyPrepareForSimImpl.class ) ;
	
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
	PrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network,
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
		final Network carOnlyNetwork;
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. Create car-only network which is handed over to PersonPrepareForSim.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			carOnlyNetwork = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<>();
			modes.add(TransportMode.car);
			filter.filter(carOnlyNetwork, modes);
		} else {
			carOnlyNetwork = network;
		}

		//matsim-724
		switch(this.facilitiesConfigGroup.getFacilitiesSource()){
			case none:
//				Gbl.assertIf( this.activityFacilities.getFacilities().isEmpty() );
				// I have at least one use case where people use the facilities as some kind
				// of database for stuff, but don't run the activities off them.  I have thus
				// disabled the above check.  yy We need to think about what we want to
				// do in such cases; might want to auto-generate our facilities as below
				// and _add_ them to the existing facilities.  kai, feb'18
				break;
			case fromFile:
			case setInScenario:
				Gbl.assertIf(! this.activityFacilities.getFacilities().isEmpty() );
				break;
			case onePerActivityLinkInPlansFile:
			case onePerActivityLocationInPlansFile:
//				FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(activityFacilities, facilitiesConfigGroup);
				FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenario);

//				facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(true, network);
				// (yy not sure if the false setting makes sense at all. kai, jul'18)

//				facilitiesFromPopulation.assignOpeningTimes(facilitiesConfigGroup.isAssigningOpeningTime(), scenario.getConfig().planCalcScore());
				facilitiesFromPopulation.run(population);
				// Note that location choice, when switched on, should now either use the facilities generated here,
				// or come with explicit pre-existing facilities.  kai, jul'18
				break;
			default:
				throw new RuntimeException("Facilities source '"+this.facilitiesConfigGroup.getFacilitiesSource()+"' is not implemented.");
		}

		// get links for facilities
		// using car only network to get the links for facilities. Amit July'18
		XY2LinksForFacilities.run(carOnlyNetwork, this.activityFacilities);

		// make sure all routes are calculated.
		// At least xy2links is needed here, i.e. earlier than PrepareForMobsimImpl.  It could, however, presumably be separated out
		// (i.e. we introduce a separate PersonPrepareForMobsim).  kai, jul'18
		ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
				new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities), scenario, carOnlyNetwork);
					}
				}
		);

		// yyyy from a behavioral perspective, the vehicle must be somehow linked to
		// the person (maybe via the household).    kai, feb'18
		
		switch( qSimConfigGroup.getVehiclesSource() ) {
			case defaultVehicle:
			case modeVehicleTypesFromVehiclesData:
				createAndAddVehiclesForEveryNetworkMode( getMode2VehicleType() );
				break;
			case fromVehiclesData:
				// don't do anything
				break;
			default:
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
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

	private void createAndAddVehiclesForEveryNetworkMode(final Map<String, VehicleType> modeVehicleTypes) {
		for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
			for (String mode : scenario.getConfig().qsim().getMainModes()) {
				Id<Vehicle> vehicleId = obtainAutomaticVehicleId(personId, mode, this.qSimConfigGroup);
				createAndAddVehicleIfNotPresent(vehicleId, modeVehicleTypes.get(mode));
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

	private void createAndAddVehicleIfNotPresent(Id<Vehicle> vehicleId, VehicleType vehicleType) {
		// try to get vehicle from the vehicles container:
		Vehicles vehicles = scenario.getVehicles();
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
		VehiclesFactory factory = vehicles.getFactory();

		if ( vehicle==null ) {
			// if it was not found, next step depends on config:
			switch ( qSimConfigGroup.getVehiclesSource() ) {
				case defaultVehicle:
					vehicle = factory.createVehicle(vehicleId, vehicleType);
					vehicles.addVehicle(vehicle);
					break;
				case modeVehicleTypesFromVehiclesData:
					vehicle = factory.createVehicle(vehicleId, vehicleType);
					vehicles.addVehicle(vehicle);
					break;
				case fromVehiclesData:
					// otherwise complain:
					throw new IllegalStateException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
				default:
					// also complain when someone added another config option here:
					throw new RuntimeException("not implemented");
			}
		}
	}

	public static Id<Vehicle> obtainAutomaticVehicleId( Id<Person> personId, String mode, QSimConfigGroup config ) {
		
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
						vehicleId = Id.createVehicleId( personId.toString() + "_" + mode );
					} else {
						vehicleId = Id.createVehicleId(personId);
					}
					break;
				default:
					throw new RuntimeException("not implemented") ;
			}
		} else {
			throw new IllegalStateException("Found a network route without a vehicle id.");
			// yyyyyy condition not really logical here. kai, jul'18
		}
		return vehicleId;
	}
}
