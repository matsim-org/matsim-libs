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

package org.matsim.core.mobsim.queuesim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class TravelTimeTest extends MatsimTestCase implements
		BasicLinkLeaveEventHandler, BasicLinkEnterEventHandler {

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

		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadScenario();
		
		EventsManagerImpl events = new EventsManagerImpl();
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
		
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadScenario();
		
		EventsManagerImpl events = new EventsManagerImpl();
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

	public void handleEvent(BasicLinkEnterEvent event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
		if (travelTimes == null) {
			travelTimes = new HashMap<Id, Double>();
			this.agentTravelTimes.put(event.getPersonId(), travelTimes);
		}
		travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.getPersonId());
		if (travelTimes != null) {
			Double d = travelTimes.get(event.getLinkId());
			if (d != null) {
				double time = event.getTime() - d.doubleValue();
				travelTimes.put(event.getLinkId(), Double.valueOf(time));
			}
		}
	}

	public void reset(int iteration) {
	}

}
