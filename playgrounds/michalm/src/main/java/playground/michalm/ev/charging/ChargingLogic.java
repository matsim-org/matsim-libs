/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.ev.charging;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.michalm.ev.data.*;

public interface ChargingLogic {
	void addVehicle(ElectricVehicle ev, double now);

	void removeVehicle(ElectricVehicle ev, double now);

	boolean isPlugged(ElectricVehicle ev);

	void chargeVehicles(double chargePeriod, double now);

	Charger getCharger();

	void reset();

	void initEventsHandling(EventsManager eventsManager);
}
