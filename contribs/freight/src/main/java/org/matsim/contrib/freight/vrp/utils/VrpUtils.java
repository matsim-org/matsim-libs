/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.utils;

import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Service;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TimeWindow;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class VrpUtils {

	public static VehicleImpl createVehicle(String id, String locationId,
			int vehicleCapacity, String vehicleType) {
		VehicleImpl vehicle = new VehicleImpl(id, locationId, VehicleImpl
				.getFactory().createType(
						vehicleType,
						vehicleCapacity,
						VehicleImpl.getFactory().createVehicleCostParams(0.0,
								1.0, 1.0)));
		return vehicle;
	}

	public static Vehicle createVehicle(String id, String locationId,
			int vehicleCapacity, String vehicleType, double fixCost,
			double costPerTime, double costPerDistance) {
		Vehicle vehicle = new VehicleImpl(id, locationId, VehicleImpl
				.getFactory().createType(
						vehicleType,
						vehicleCapacity,
						VehicleImpl.getFactory().createVehicleCostParams(
								fixCost, costPerTime, costPerDistance)));
		return vehicle;
	}

	public static TimeWindow createTimeWindow(double start, double end) {
		return new TimeWindow(start, end);
	}

	public static String createId(String id) {
		return id;
	}

	public static Coordinate createCoord(int x, int y) {
		return new Coordinate(x, y);
	}

	public static Shipment createShipment(String id, String fromId,
			String toId, int size, TimeWindow pickupTW, TimeWindow deliverTW) {
		Shipment s = new Shipment(id, fromId, toId, size);
		s.setPickupTW(pickupTW);
		s.setDeliveryTW(deliverTW);
		return s;
	}

	public static Service createService(String id, String locationId,
			int demand, double serviceTime, double earliestOperationStartTime,
			double latestOperationStartTime) {
		return new Service(id, locationId, demand, serviceTime,
				earliestOperationStartTime, latestOperationStartTime);
	}

}
