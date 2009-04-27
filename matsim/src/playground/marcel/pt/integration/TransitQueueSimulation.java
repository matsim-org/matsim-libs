/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.integration;

import java.util.HashMap;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.network.NetworkLayer;

import playground.marcel.pt.implementations.TransitDriver;
import playground.marcel.pt.interfaces.Vehicle;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.tryout.VehicleImpl;
import playground.marcel.pt.utils.FacilityVisitors;

public class TransitQueueSimulation extends QueueSimulation {

	private TransitSchedule schedule = null;
	private final FacilityVisitors fv;
	private final HashMap<Person, PersonAgent> agents = new HashMap<Person, PersonAgent>(100);

	public TransitQueueSimulation(final NetworkLayer network, final Population population, final Events events) {
		super(network, population, events);

		this.setAgentFactory(new TransitAgentFactory(this, this.agents));

		this.fv = new FacilityVisitors();
		events.addHandler(this.fv);
	}

	public void setTransitSchedule(final TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public PersonAgent getAgent(final Person p) {
		return this.agents.get(p);
	}

	@Override
	protected void createAgents() {
		super.createAgents();

		if (this.schedule != null) {

			for (TransitLine line : this.schedule.getTransitLines().values()) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (Departure departure : route.getDepartures().values()) {
						TransitDriver driver = new TransitDriver(route, departure, this);
						driver.setFacilityVisitorObserver(this.fv);
						Vehicle bus = new VehicleImpl(20, getEvents());
						driver.setVehicle(bus);
						this.scheduleActivityEnd(driver);
						Simulation.incLiving();
					}
				}
			}
		}

	}

}
