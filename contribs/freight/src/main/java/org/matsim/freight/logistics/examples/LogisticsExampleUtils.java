/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Static helper methods that are shared by several logistics examples (in the sub-packages of this package). Material
 * that is used by only one group of examples belongs into a helper class of that group, e.g. {@code
 * TwoEchelonGridUtils} in {@code initialPlans}.
 */
public final class LogisticsExampleUtils {

	private LogisticsExampleUtils() {
	} // so it cannot be instantiated

	/**
	 * Creates a new vehicle type "large50": capacity 50, fixed costs 150, costs per meter 0.01, costs per second 0.01,
	 * maximum velocity 10 m/s, network mode car.
	 *
	 * <p>Every call creates a new instance, so that examples (and tests that run several examples in the same JVM) do
	 * not share one mutable vehicle type.
	 */
	public static VehicleType createVehTypeLarge50() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("large50", VehicleType.class), TransportMode.car);
		vehicleType.getCapacity().setOther(50);
		vehicleType.getCostInformation().setCostsPerMeter(0.01);
		vehicleType.getCostInformation().setCostsPerSecond(0.01);
		vehicleType.getCostInformation().setFixedCost(150.);
		vehicleType.setMaximumVelocity(10);
		vehicleType.setNetworkMode(TransportMode.car);

		return vehicleType;
	}

	/**
	 * Creates a new vehicle type "small05": capacity 5, fixed costs 25, costs per meter 0.001, costs per second 0.005,
	 * maximum velocity 10 m/s, network mode car.
	 *
	 * <p>Every call creates a new instance, see {@link #createVehTypeLarge50()}.
	 */
	public static VehicleType createVehTypeSmall05() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("small05", VehicleType.class), TransportMode.car);
		vehicleType.getCapacity().setOther(5);
		vehicleType.getCostInformation().setCostsPerMeter(0.001);
		vehicleType.getCostInformation().setCostsPerSecond(0.005);
		vehicleType.getCostInformation().setFixedCost(25.);
		vehicleType.setMaximumVelocity(10);
		vehicleType.setNetworkMode(TransportMode.car);

		return vehicleType;
	}

	/**
	 * Sets the free speed of all links to 30 km/h (8.33 m/s) and their capacity to 1000. On the 9x9 grid network of
	 * the matsim-examples, the free-flow travel time per link is then 2 min, which makes the results easier to compute
	 * by hand.
	 */
	public static void setFreespeed30KmhAndCapacity1000OnAllLinks(Network network) {
		for (Link link : network.getLinks().values()) {
			link.setFreespeed(30 / 3.6);
			link.setCapacity(1000);
		}
	}

	/**
	 * Collects the resources of all logistic chain elements of all logistic chains of the given plans, in this order.
	 * A resource that is used by several elements is contained several times.
	 */
	public static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		List<LSPResource> resourcesList = new ArrayList<>();
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticChain chain : lspPlan.getLogisticChains()) {
				for (LogisticChainElement chainElement : chain.getLogisticChainElements()) {
					resourcesList.add(chainElement.getResource());
				}
			}
		}
		return resourcesList;
	}
}
