/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.dgrether.signalsystems.sylvia;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.vehicles.Vehicle;

public class DgTimeCalcEventHandler implements LaneEnterEventHandler {

	private Set<Id<Vehicle>> carsPassed;
	private Set<Id<Lane>> wannabeadaptiveLanes;

	public DgTimeCalcEventHandler() {
		this.wannabeadaptiveLanes = new HashSet<>();
		this.fillWannaBes();
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.carsPassed = new HashSet<>();
	}

	private void fillWannaBes() {
		// mock up adaptive lanes to create comparable travel times LSA-SLV
		for (int i = 2100; i < 2113; i++) { // Signalsystem 17
			this.wannabeadaptiveLanes.add(Id.create(i, Lane.class));
		}
		for (int i = 2000; i < 2013; i++) { // Signalsystem 18
			this.wannabeadaptiveLanes.add(Id.create(i, Lane.class));
		}
		for (int i = 1900; i < 1913; i++) { // Signalsystem 1
			this.wannabeadaptiveLanes.add(Id.create(i, Lane.class));
		}

	}

	@Override
	public void handleEvent(LaneEnterEvent event) {
		// if (this.ach.laneIsAdaptive(event.getLaneId()) & (!event.getLaneId().toString().endsWith(".ol")))
		// actually the nicer way

		if (this.wannabeadaptiveLanes.contains(event.getLaneId()))
			this.carsPassed.add(event.getVehicleId());

	}

	public long getPassedAgents() {
		return this.carsPassed.size();
	}

	public Set<Id<Vehicle>> getPassedCars() {
		return carsPassed;
	}


}