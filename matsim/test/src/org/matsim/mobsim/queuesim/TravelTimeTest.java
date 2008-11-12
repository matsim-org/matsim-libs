/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.queuesim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class TravelTimeTest extends MatsimTestCase implements
		LinkLeaveEventHandler, LinkEnterEventHandler {

  private Map<Id, Map<Id, Double>> agentTravelTimes;

  @Override
  protected void tearDown() throws Exception {
  	this.agentTravelTimes = null;
  	super.tearDown();
  }

	public void testEquilOneAgent() {
		this.agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		Config conf = loadConfig("test/scenarios/equil/config.xml");
		String popFileName = "test/scenarios/equil/plans1.xml";

		conf.plans().setInputFile(popFileName);

		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);

		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();

		Map<Id, Double> travelTimes = this.agentTravelTimes.get(new IdImpl("1"));
		assertEquals(360.0, travelTimes.get(new IdImpl(6)).intValue(), EPSILON);
		assertEquals(180.0, travelTimes.get(new IdImpl(15)).intValue(), EPSILON);
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), EPSILON);
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), EPSILON);
	}

	public void testEquilTwoAgents() {
		this.agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		Config conf = loadConfig("test/scenarios/equil/config.xml");
		String popFileName = "test/scenarios/equil/plans2.xml";

		conf.plans().setInputFile(popFileName);

		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);

		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();

		Map<Id, Double> travelTimes = this.agentTravelTimes.get(new IdImpl("1"));
		assertEquals(360.0, travelTimes.get(new IdImpl(6)).intValue(), EPSILON);
		assertEquals(180.0, travelTimes.get(new IdImpl(15)).intValue(), EPSILON);
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), EPSILON);
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), EPSILON);


		travelTimes = this.agentTravelTimes.get(new IdImpl("2"));
		assertEquals(360.0, travelTimes.get(new IdImpl(5)).intValue(), EPSILON);
		assertEquals(180.0, travelTimes.get(new IdImpl(14)).intValue(), EPSILON);
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(21)).intValue(), EPSILON);
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)).intValue(), EPSILON);
		assertEquals(360.0, travelTimes.get(new IdImpl(23)).intValue(), EPSILON);
	}

	public void handleEvent(LinkEnterEvent event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.agent.getId());
		if (travelTimes == null) {
			travelTimes = new HashMap<Id, Double>();
			this.agentTravelTimes.put(event.agent.getId(), travelTimes);
		}
		travelTimes.put(event.link.getId(), Double.valueOf(event.time));
	}

	public void handleEvent(LinkLeaveEvent event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.agent.getId());
		if (travelTimes != null) {
			Double d = travelTimes.get(event.link.getId());
			if (d != null) {
				double time = event.time - d.doubleValue();
				travelTimes.put(event.link.getId(), Double.valueOf(time));
			}
		}
	}

	public void reset(int iteration) {
	}

}
