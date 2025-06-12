
/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultVehicleHandler.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Default implementation of a {{@link #VehicleHandler()}. It always allows
 * vehicles to arrive at a link.
 * <p>
 * The design of this class follows the design pattern of the {@link DefaultLinkSpeedCalculator}
 *
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class DefaultVehicleHandler implements VehicleHandler {
	public static final Logger log = LogManager.getLogger(DefaultVehicleHandler.class);

	private final Collection<VehicleHandler> handlers = new ArrayList<>();

	@Inject
	DefaultVehicleHandler() {
	} // so it has to be instantiated by injection from outside package.  paul, jan'25


	@Override
	public void handleVehicleDeparture(QVehicle vehicle, Link link) {
		for (VehicleHandler handler : handlers) {
			handler.handleVehicleDeparture(vehicle, link);
		}
	}

	@Override
	public VehicleArrival handleVehicleArrival(QVehicle vehicle, Link link) {
		VehicleArrival result = null;

		// go through all handlers and store the result
		for (VehicleHandler handler : handlers) {
			VehicleArrival tmp = handler.handleVehicleArrival(vehicle, link);
			if (Objects.nonNull(tmp)) {
				if (Objects.nonNull(result)) {
					throw new RuntimeException("Two vehicle handlers feel responsible for vehicle; don't know what to do.");
				}
				result = tmp;
			}
		}

		// If no handler has a result, return ALLOWED as fallback
		if (Objects.isNull(result)) {
			result = VehicleArrival.ALLOWED;
		}
		return result;
	}

	@Override
	public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {
		for (VehicleHandler handler : handlers) {
			handler.handleInitialVehicleArrival(vehicle, link);
		}
	}

	/**
	 * This is not meant to be used directly.  But rather through {@link AbstractQSimModule} addVehicleHandlerBinding().
	 */
	public final DefaultVehicleHandler addVehicleHandler(VehicleHandler vehicleHandler) {
		this.handlers.add(vehicleHandler);
		return this;
	}
}
