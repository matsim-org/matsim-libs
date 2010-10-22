/* *********************************************************************** *
 * project: org.matsim.*
 * PhantomManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.simulation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.gregor.sim2d.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class PhantomManager {

	private final Scenario2DImpl scenario;
	private final Queue<Event> phantomQueue;
	private final Mobsim sim;

	private final Map<Id, Coordinate> phantomPositions = new LinkedHashMap<Id, Coordinate>();

	public PhantomManager(Scenario2DImpl scenario, Mobsim sim) {
		this.scenario = scenario;
		this.phantomQueue = new ConcurrentLinkedQueue<Event>(scenario.getPhantomPopulation());
		this.sim = sim;
	}

	/**
	 * @param time
	 */
	public void update(double time) {
		// this.phantomPositions.clear();
		while (this.phantomQueue.size() > 0 && this.phantomQueue.peek().getTime() <= time) {
			Event e = this.phantomQueue.poll();

			if (e instanceof AgentDepartureEventImpl) {
				Id id = new IdImpl("-" + ((PersonEvent) e).getPersonId());
				AgentDepartureEventImpl e2 = new AgentDepartureEventImpl(e.getTime(), id, ((AgentDepartureEventImpl) e).getLinkId(), ((AgentDepartureEventImpl) e).getLegMode());
				this.sim.getEventsManager().processEvent(e2);
			} else if (e instanceof AgentArrivalEventImpl) {
				Id id = new IdImpl("-" + ((PersonEvent) e).getPersonId());
				AgentArrivalEventImpl e2 = new AgentArrivalEventImpl(e.getTime(), id, ((AgentArrivalEventImpl) e).getLinkId(), ((AgentArrivalEventImpl) e).getLegMode());
				this.sim.getEventsManager().processEvent(e2);
				this.phantomPositions.remove(e2.getPersonId());
			} else if (e instanceof XYZAzimuthEventImpl) {
				Id id = new IdImpl("-" + ((PersonEvent) e).getPersonId());
				XYZAzimuthEventImpl e2 = new XYZAzimuthEventImpl(id, ((XYZAzimuthEventImpl) e).getCoordinate(), ((XYZAzimuthEventImpl) e).getAzimuth(), e.getTime());
				this.sim.getEventsManager().processEvent(e2);
				this.phantomPositions.put(e2.getPersonId(), e2.getCoordinate());
			}
		}

	}

	/**
	 * @param position
	 * @return
	 */
	public Collection<Coordinate> getPhatomsList() {
		return this.phantomPositions.values();
	}

}
