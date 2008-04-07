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
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.identifiers.IdI;


/**
 * @author dgrether
 *
 */
public class TravelTimeTest extends MatsimTestCase implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI {

	private Map<IdI, Double> travelTimes = new HashMap<IdI, Double>();

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testEquilOneAgent() {
		Config conf = loadConfig("test/scenarios/equil/config.xml");
//		String netFileName = "test/scenarios/equil/network.xml";
		String popFileName = "test/scenarios/equil/plans1.xml";

		conf.plans().setInputFile(popFileName);
		conf.controler().setLastIteration(0);

		Controler controler = new Controler(conf);
		Events events = controler.getEvents();
		events.addHandler(this);
		controler.run();

		assertEquals(360.0, this.travelTimes.get(new Id(6)));
		assertEquals(180.0, this.travelTimes.get(new Id(15)));
		assertEquals(13560.0, this.travelTimes.get(new Id(20)));
		assertEquals(360.0, this.travelTimes.get(new Id(21)));
		assertEquals(1260.0, this.travelTimes.get(new Id(22)));
		assertEquals(360.0, this.travelTimes.get(new Id(23)));
	}

	public void handleEvent(EventLinkEnter event) {
		this.travelTimes.put(event.link.getId(), event.time);
	}


	public void handleEvent(EventLinkLeave event) {
		Double d = this.travelTimes.get(event.link.getId());
		if (d != null) {
			Double time = event.time - d.doubleValue();
			this.travelTimes.put(event.link.getId(), time);
		}
	}

	public void reset(int iteration) {
	}




}
