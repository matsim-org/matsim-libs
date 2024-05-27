/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimDataProvider.java
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

package org.matsim.withinday.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;

/**
 * Provides Mobsim related data such as the Agents or QVehicles.
 *
 * @author cdobler
 */
@Singleton
public final class MobsimDataProvider implements MobsimInitializedListener {
	/*
	Hallo Kai,

	ja, das kann man sicherlich auch einfach durchschleifen (oder gleich in die QSim einbauen? Sind ja doch recht "typische" Methoden).

	Das Design kommt einfach daher, dass die QSim die Daten nicht oder nur in recht ungünstiger Form bereit hält:
	- Agents nur in einer Collection, nicht in einer Map
	- Vehicles nur in den jeweiligen Links aber nicht an einer zentralen Stelle

	Viele Grüße,
	Christoph


	-----Ursprüngliche Nachricht-----
	Von: Nagel, Kai, Prof. Dr. [mailto:nagel@vsp.tu-berlin.de]
	Gesendet: Dienstag, 17. März 2015 13:50
	An: Christoph Dobler
	Cc: Zilske, Michael
	Betreff: MobsimDataProvider

	Hallo Christoph,

	Bin gerade über Deinen MobsimDataProvider gestolpert.  Die Gruppe von Lin Padham verwendet den.

	Ich würde es aber für ein Problem halten, dass er mit der QSim nicht automatisch konsistent ist.  Z.B. kann man in der QSim
	nachträglich Agenten und Fahrzeuge einfügen; der MobsimDataProvider würde das aber nicht mitbekommen.

	Ich würde von der Tendenz her das "Durchschleifen", also die Information im MobsimDataProvider nicht mehr separat sammeln,
	sondern die entsprechenden Methoden aus der QSim verwenden.  Falls die nicht schnell genug sind, könnten wir sie ja optimieren.

	Oder?

	Danke & vG

	Kai
	*/

	private QSim qSim;

	@Inject MobsimDataProvider(){}

	@Override
	public final void notifyMobsimInitialized(MobsimInitializedEvent e) {
		qSim = (QSim) e.getQueueSimulation();
	}

	public final Map<Id<Person>, MobsimAgent> getAgents() {
		return this.qSim.getAgents() ;
	}

	public final MobsimAgent getAgent(Id<Person> agentId) {
		return this.getAgents().get(agentId);
	}

	public final Map<Id<Vehicle>, MobsimVehicle> getVehicles() {
		return this.qSim.getVehicles() ;
	}

	public final MobsimVehicle getVehicle(Id<Vehicle> vehicleId) {
		return this.getVehicles().get(vehicleId);
	}

	public final Collection<MobsimVehicle> getEnrouteVehiclesOnLink(Id<Link> linkId) {
		return this.qSim.getNetsimNetwork().getNetsimLink(linkId).getAllNonParkedVehicles();
	}

	public final MobsimVehicle getDriversVehicle(Id<Person> driverId) {
		MobsimAgent mobsimAgent = this.getAgents().get(driverId);
		if (mobsimAgent == null) return null;

		DriverAgent driver = (DriverAgent) mobsimAgent;
		return driver.getVehicle();
	}

	public final MobsimAgent getVehiclesDriver(Id<Vehicle> vehicleId) {
		MobsimVehicle mobsimVehicle = this.getVehicles().get(vehicleId);
		if (mobsimVehicle == null) return null;
		else return mobsimVehicle.getDriver();
	}
}
