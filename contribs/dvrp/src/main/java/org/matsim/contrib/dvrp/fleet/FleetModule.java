/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.fleet;

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.load.DefaultDvrpLoadFromFleet;
import org.matsim.contrib.dvrp.load.DefaultDvrpLoadFromVehicle;
import org.matsim.contrib.dvrp.load.DefaultDvrpLoadFromVehicle.CapacityMapping;
import org.matsim.contrib.dvrp.load.DvrpLoadFromFleet;
import org.matsim.contrib.dvrp.load.DvrpLoadFromVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoadModule;
import org.matsim.contrib.dvrp.load.DvrpLoadParams;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Singleton;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FleetModule extends AbstractDvrpModeModule {
	private final URL fleetSpecificationUrl;
	private final boolean updateVehicleStartLinkToLastLink;
	private final VehicleType vehicleType;
	private final DvrpLoadParams loadParams;

	public FleetModule(String mode, URL fleetSpecificationUrl) {
		this(mode, fleetSpecificationUrl, false, new DvrpLoadParams());
	}

	public FleetModule(String mode, URL fleetSpecificationUrl, boolean updateVehicleStartLinkToLastLink, DvrpLoadParams loadParams) {
		this(mode, fleetSpecificationUrl, VehicleUtils.createDefaultVehicleType(), updateVehicleStartLinkToLastLink, loadParams);
	}

	public FleetModule(String mode, URL fleetSpecificationUrl, VehicleType vehicleType) {
		this(mode, fleetSpecificationUrl, vehicleType, false, new DvrpLoadParams());
	}

	public FleetModule(String mode, URL fleetSpecificationUrl, VehicleType vehicleType, boolean updateVehicleStartLinkToLastLink, DvrpLoadParams loadParams) {
		super(mode);
		this.fleetSpecificationUrl = fleetSpecificationUrl;
		this.vehicleType = vehicleType;
		this.loadParams = loadParams;
		this.updateVehicleStartLinkToLastLink = updateVehicleStartLinkToLastLink;
	}

	@Override
	public void install() {
		//
		// 3 options:
		// - vehicle specifications provided in a separate XML file (http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd)
		// - vehicle specifications derived from the "standard" matsim vehicles (only if they are read from a file,
		//     i.e. VehiclesSource.fromVehiclesData)
		// - vehicle specifications provided via a custom binding for FleetSpecification

		install(new DvrpLoadModule(getMode(), loadParams));

		bindModal(DefaultDvrpLoadFromFleet.class).toProvider(modalProvider(getter -> {
			DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
			return new DefaultDvrpLoadFromFleet(loadType, loadParams.mapFleetCapacity);
		})).in(Singleton.class);

		bindModal(DvrpLoadFromFleet.class).to(modalKey(DefaultDvrpLoadFromFleet.class));

		bindModal(DvrpLoadFromVehicle.class).toProvider(modalProvider(getter -> {
			DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
			return new DefaultDvrpLoadFromVehicle(loadType, CapacityMapping.build(loadParams));
		}));

		if (fleetSpecificationUrl != null) {
			bindModal(FleetSpecification.class).toProvider(modalProvider((getter) -> {
				FleetSpecification fleetSpecification = new FleetSpecificationImpl();
				DvrpLoadFromFleet dvrpLoadFromFleet = getter.getModal(DvrpLoadFromFleet.class);
				new FleetReader(fleetSpecification, dvrpLoadFromFleet).parse(fleetSpecificationUrl);
				return fleetSpecification;
			})).asEagerSingleton();
		} else {
			bindModal(FleetSpecification.class).toProvider(modalProvider(getter -> {
				Vehicles vehicles = getter.get(Vehicles.class);
				DvrpLoadFromVehicle dvrpLoadFromVehicle = getter.getModal(DvrpLoadFromVehicle.class);
				return DvrpVehicleSpecificationWithMatsimVehicle.createFleetSpecificationFromMatsimVehicles(getMode(), vehicles, dvrpLoadFromVehicle);
			})).asEagerSingleton();
		}

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(Fleet.class).toProvider(modalProvider(
						getter -> Fleets.createDefaultFleet(getter.getModal(FleetSpecification.class),
								getter.getModal(Network.class).getLinks()::get))).asEagerSingleton();
			}
		});

		if (updateVehicleStartLinkToLastLink) {
			bindModal(VehicleStartLinkToLastLinkUpdater.class).toProvider(modalProvider(
					getter -> new VehicleStartLinkToLastLinkUpdater(getter.getModal(FleetSpecification.class),
							getter.getModal(ExecutedScheduleCollector.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(VehicleStartLinkToLastLinkUpdater.class));
		}

		bindModal(FleetControlerListener.class).toProvider(modalProvider(
				getter -> new FleetControlerListener(getMode(), getter.get(OutputDirectoryHierarchy.class),
						getter.getModal(FleetSpecification.class), getter.getModal(DvrpLoadType.class)))).in(Singleton.class);
		addControlerListenerBinding().to(modalKey(FleetControlerListener.class));

		bindModal(VehicleType.class).toInstance(vehicleType);
	}
}
