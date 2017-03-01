/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;

import playground.michalm.ev.data.*;
import playground.michalm.taxi.vrpagent.ETaxiAtChargerActivity;

public class EvrpVehicle extends VehicleImpl {
	public class Ev extends ElectricVehicleImpl {
		private ETaxiAtChargerActivity atChargerActivity;

		public Ev(Id<Vehicle> id, Battery battery) {
			super(Id.createVehicleId(id), battery);
		}

		public EvrpVehicle getEvrpVehicle() {
			return EvrpVehicle.this;
		}

		public ETaxiAtChargerActivity getAtChargerActivity() {
			return atChargerActivity;
		}

		public void setAtChargerActivity(ETaxiAtChargerActivity atChargerActivity) {
			this.atChargerActivity = atChargerActivity;
		}
	}

	private final Ev ev;

	public EvrpVehicle(Id<Vehicle> id, Link startLink, double capacity, double t0, double t1, double batteryCapacity,
			double initialSoc) {
		super(id, startLink, capacity, t0, t1);
		ev = new Ev(id, new BatteryImpl(batteryCapacity, initialSoc));
	}

	public Ev getEv() {
		return ev;
	}
}