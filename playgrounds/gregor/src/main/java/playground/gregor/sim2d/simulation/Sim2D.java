/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2D.java
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
package playground.gregor.sim2d.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.events.XYZEventsGenerator;
import playground.gregor.sim2d.peekabot.Sim2DVis;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Sim2D {

	private final Config config;
	private final Population population;
	private final Network network;
	private static EventsManager events;
	private double stopTime;
	private final Network2D network2D;
	private double startTime;
	private final double endTime;

	protected final PriorityBlockingQueue<Agent2D> activityEndsList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	protected final PriorityBlockingQueue<Agent2D> agentsToRemoveList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());

	private Sim2DVis sim2DVis;
	private XYZEventsGenerator xyzEventsGen = null;

	public Sim2D(final Network network, final Map<MultiPolygon, List<Link>> floors, final Map<Id, LineString> ls, final Population plans, final EventsManager events, final SegmentedStaticForceField sff, final Config config) {
		this.config = config;
		this.endTime = 9 * 3600 + 30 * 60;
		this.network = network;
		Map<MultiPolygon, Network> f = new HashMap<MultiPolygon, Network>();
		for (Entry<MultiPolygon, List<Link>> e : floors.entrySet()) {
			f.put(e.getKey(), this.network);
		}
		this.network2D = new Network2D(this.network, f, sff, ls);

		this.population = plans;
		setEvents(events);
		SimulationTimer.reset(Sim2DConfig.TIME_STEP_SIZE);
	}

	public void run() {
		prepareSim();
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			cont = doSimStep(time);
			afterSimStep(time);
			if (cont) {
				SimulationTimer.incTime();
			}
		}
		if (this.sim2DVis != null) {
			this.sim2DVis.reset();
		}
	}

	private boolean doSimStep(double time) {
		handleActivityEnds(time);
		handleAgentRemoves(time);
		this.network2D.move(time);
		if (time >= this.endTime) {
			return false;
		}
		return true;
	}

	private void handleAgentRemoves(double time) {
		while (this.agentsToRemoveList.peek() != null) {
			Agent2D agent = this.agentsToRemoveList.poll();
			// TODO works only as long as there is only one floor!!
			this.network2D.removeAgent(agent);
			// this.peekABotVis.removeBot(Integer.parseInt(agent.getId().toString()));
		}
	}

	public void scheduleAgentRemove(Agent2D agent2d) {
		this.agentsToRemoveList.add(agent2d);

	}

	protected void scheduleActivityEnd(final Agent2D agent) {
		this.activityEndsList.add(agent);
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			Agent2D agent = this.activityEndsList.peek();
			if (agent.getNextDepartureTime() <= time) {
				this.activityEndsList.poll();
				agent.depart();
			} else {
				return;
			}
		}
	}

	private void prepareSim() {
		this.startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (this.startTime == Time.UNDEFINED_TIME)
			this.startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0))
			this.stopTime = Double.MAX_VALUE;

		createAgents();

		double simStartTime = 0;
		Agent2D firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(this.startTime, firstAgent.getNextDepartureTime()));
		}

		SimulationTimer.setSimStartTime(simStartTime);
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());
	}

	private void createAgents() {
		if (this.population == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}

		for (Person p : this.population.getPersons().values()) {
			Agent2D agent = new Agent2D(p, this);
			if (agent.initialize()) {

				this.network2D.addAgent(agent);

			}
		}

	}

	protected void afterSimStep(final double time) {
		if (this.sim2DVis != null) {
			this.sim2DVis.draw(time);
		}
		if (this.xyzEventsGen != null) {
			this.xyzEventsGen.generateEvents(time);
		}
	}

	// //DEBUG
	// private List<double []> updateForceInfos() {
	// List<double []> ret = new ArrayList<double[]>();
	// for (Floor floor : this.network2D.getFloors()) {
	// ret.addAll(floor.getForceInfos());
	// }
	// return ret;
	// }

	public static final EventsManager getEvents() {
		return events;
	}

	/* package */Network getNetwork() {
		return this.network;
	}

	private static final void setEvents(final EventsManager events) {
		Sim2D.events = events;
	}

	public void setXYZEventsManager(XYZEventsGenerator xyzEvents) {
		this.xyzEventsGen = xyzEvents;
		this.xyzEventsGen.setNetwork2D(this.network2D);
	}

	public void setSim2DVis(Sim2DVis sim2DVis) {
		this.sim2DVis = sim2DVis;
		this.sim2DVis.setNetwork2D(this.network2D);
	}

}
