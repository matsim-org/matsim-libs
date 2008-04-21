/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.mobsim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 *
 */
public class TravelTimeTest extends MatsimTestCase implements
		EventHandlerLinkLeaveI, EventHandlerLinkEnterI {


  private Map<Id, Map<Id, Double>> agentTravelTimes;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testEquilOneAgent() {
		this.agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		Config conf = loadConfig("test/scenarios/equil/config.xml");
		String popFileName = "test/scenarios/equil/plans1.xml";

		conf.plans().setInputFile(popFileName);
		conf.controler().setLastIteration(0);

		Controler controler = new Controler(conf);
		Events events = controler.getEvents();
		events.addHandler(this);
		controler.run();

		Map<Id, Double> travelTimes = this.agentTravelTimes.get(new IdImpl("1"));
		assertEquals(360.0, travelTimes.get(new IdImpl(6)));
		assertEquals(180.0, travelTimes.get(new IdImpl(15)));
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)));
		assertEquals(360.0, travelTimes.get(new IdImpl(21)));
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)));
		assertEquals(360.0, travelTimes.get(new IdImpl(23)));
	}

	public void testEquilTwoAgents() {
		this.agentTravelTimes = new HashMap<Id, Map<Id, Double>>();
		Config conf = loadConfig("test/scenarios/equil/config.xml");
		String popFileName = "test/scenarios/equil/plans2.xml";

		conf.plans().setInputFile(popFileName);
		conf.controler().setLastIteration(0);

		Controler controler = new Controler(conf);
		Events events = controler.getEvents();
		events.addHandler(this);
		controler.run();

		Map<Id, Double> travelTimes = this.agentTravelTimes.get(new IdImpl("1"));
		assertEquals(360.0, travelTimes.get(new IdImpl(6)));
		assertEquals(180.0, travelTimes.get(new IdImpl(15)));
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)));
		assertEquals(360.0, travelTimes.get(new IdImpl(21)));
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)));
		assertEquals(360.0, travelTimes.get(new IdImpl(23)));


		travelTimes = this.agentTravelTimes.get(new IdImpl("2"));
		assertEquals(360.0, travelTimes.get(new IdImpl(5)));
		assertEquals(180.0, travelTimes.get(new IdImpl(14)));
		assertEquals(13560.0, travelTimes.get(new IdImpl(20)));
		assertEquals(360.0, travelTimes.get(new IdImpl(21)));
		assertEquals(1260.0, travelTimes.get(new IdImpl(22)));
		assertEquals(360.0, travelTimes.get(new IdImpl(23)));

	}

	public void handleEvent(EventLinkEnter event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.agent.getId());
		if (travelTimes == null) {
			travelTimes = new HashMap<Id, Double>();
			this.agentTravelTimes.put(event.agent.getId(), travelTimes);
		}
		travelTimes.put(event.link.getId(), event.time);
	}

	public void handleEvent(EventLinkLeave event) {
		Map<Id, Double> travelTimes = this.agentTravelTimes.get(event.agent.getId());
		if (travelTimes != null) {
			Double d = travelTimes.get(event.link.getId());
			if (d != null) {
				Double time = event.time - d.doubleValue();
				travelTimes.put(event.link.getId(), time);
			}
		}
	}

	public void reset(int iteration) {
	}

}
