
/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareForSimImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

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

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

		// yyyy from a behavioral perspective, the vehicle must be somehow linked to
		// the person (maybe via the household).    kai, feb'18
		// each agent receives a vehicle for each main mode now. janek, aug'19
		createAndAddVehiclesForEveryNetworkMode();

		// make sure all routes are calculated.
		// the above creation of vehicles per agent has to be run before executing the initial routing here. janek, aug'19
		// At least xy2links is needed here, i.e. earlier than PrepareForMobsimImpl.  It could, however, presumably be separated out
		// (i.e. we introduce a separate PersonPrepareForMobsim).  kai, jul'18
		ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
				() -> new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities), scenario, carOnlyNetwork)
		);
		
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

	// only warn once that legacy vehicle id is used
	private static boolean hasWarned = false;

	private void createAndAddVehiclesForEveryNetworkMode() {

		final Map<String, VehicleType> modeVehicleTypes = getVehicleTypesForAllNetworkAndMainModes();

		for (Map.Entry<String, VehicleType> modeType : modeVehicleTypes.entrySet()) {
			for (Person person : scenario.getPopulation().getPersons().values()) {

				Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, modeType.getKey());

				if (qSimConfigGroup.getUsePersonIdForMissingVehicleId() && TransportMode.car.equals(modeType.getKey())) {
					if (!hasWarned) {
						log.warn("'usePersonIdForMissingVehicleId' is deprecated. It will be removed soon.");
						hasWarned = true;
					}

					vehicleId = Id.createVehicleId(person.getId());
				}
				createAndAddVehicleIfNecessary(vehicleId, modeType.getValue());
				VehicleUtils.insertVehicleIdIntoAttributes(person, modeType.getKey(), vehicleId);
			}
		}
	}

	private Map<String, VehicleType> getVehicleTypesForAllNetworkAndMainModes() {

		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

		if (qSimConfigGroup.getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.fromVehiclesData)) {
			// in this case the user has to do everything on their own and we can short circuit here.
			return modeVehicleTypes;
		}

		Set<String> modes = new HashSet<>(qSimConfigGroup.getMainModes());
		modes.addAll(scenario.getConfig().plansCalcRoute().getNetworkModes());

		for (String mode : modes) {
			VehicleType type = null;
			switch (qSimConfigGroup.getVehiclesSource()) {
				case defaultVehicle:
					type = VehicleUtils.getDefaultVehicleType();
					if (!scenario.getVehicles().getVehicleTypes().containsKey(type.getId()))
						scenario.getVehicles().addVehicleType(type);
					break;
				case modeVehicleTypesFromVehiclesData:
					type = scenario.getVehicles().getVehicleTypes().get(Id.create(mode, VehicleType.class));
					break;
				default:
					throw new RuntimeException(qSimConfigGroup.getVehiclesSource().toString() + " is not implemented yet.");
			}
			Gbl.assertNotNull(type);
			modeVehicleTypes.put(mode, type);
		}
		return modeVehicleTypes;
	}

	private void createAndAddVehicleIfNecessary(Id<Vehicle> vehicleId, VehicleType vehicleType) {

		if (!scenario.getVehicles().getVehicles().containsKey(vehicleId)) {

			switch (qSimConfigGroup.getVehiclesSource()) {
				case defaultVehicle:
				case modeVehicleTypesFromVehiclesData:
					Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(vehicleId, vehicleType);
					scenario.getVehicles().addVehicle(vehicle);
					break;
				default:
					throw new RuntimeException("Expecting a vehicle id which is missing in the vehicles database: " + vehicleId);
			}
		}
	}
}
