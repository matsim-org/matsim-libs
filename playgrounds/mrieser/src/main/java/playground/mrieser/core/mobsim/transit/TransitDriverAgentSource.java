/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;

public class TransitDriverAgentSource implements AgentSource {

	private final static Logger log = Logger.getLogger(TransitDriverAgentSource.class);

	private final TransitSchedule schedule;
	private final Vehicles transitVehicles;
	private boolean useUmlaeufe = false;
	private final ReconstructingUmlaufBuilder umlaufBuilder;
	private final TransitStopAgentTracker agentTracker;
	private final String transitVehicleLegType;

	public TransitDriverAgentSource(final TransitSchedule schedule, final Vehicles transitVehicles, final Network network, final TransitStopAgentTracker agentTracker, final String transitVehicleLegType) {
		this.schedule = schedule;
		this.transitVehicles = transitVehicles;
		this.agentTracker = agentTracker;
		this.transitVehicleLegType = transitVehicleLegType;
		this.umlaufBuilder = new ReconstructingUmlaufBuilder(network, this.schedule.getTransitLines().values(),
				this.transitVehicles, new PlanCalcScoreConfigGroup()); // TODO [MR] use config, not create new
	}

	@Override
	public List<PlanAgent> getAgents() {
		return createVehiclesAndDriversWithUmlaeufe();
	}

	private List<PlanAgent> createVehiclesAndDriversWithUmlaeufe() {
		List<PlanAgent> drivers = new ArrayList<PlanAgent>();
		Collection<Umlauf> umlaeufe = this.umlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			Vehicle basicVehicle = this.transitVehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				PlanAgent driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private PlanAgent createAndScheduleVehicleAndDriver(Umlauf umlauf, Vehicle vehicle) {
		TransitDriverPlanAgent driver = new TransitDriverPlanAgent(umlauf, vehicle, this.agentTracker, this.transitVehicleLegType);
		return driver;
	}
}
