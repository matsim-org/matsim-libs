/* *********************************************************************** *
 * project: org.matsim.*
 * MixedLaneTest
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
package org.matsim.contrib.signals;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;


/**
 * 
 * @author dgrether
 *
 */
public class SignalsMixedLaneTest {

	private static final Logger log = Logger.getLogger(SignalsMixedLaneTest.class);

	private SignalsMixedLaneTestFixture fixture;

	@Before
	public void initFixture() {
		fixture = new SignalsMixedLaneTestFixture();
	}
	


	@Test
	public void testMixedLanesAndSignals() {
		fixture.create2PersonPopulation();
		EventsManager events = EventsUtils.createEventsManager();
//		((EventsImpl)events).addHandler(new LogOutputEventHandler());

		MixedLanesEventsHandler handler = new MixedLanesEventsHandler(this.fixture);
		events.addHandler(handler);

		QSim qsim = (QSim) new QSimFactory().createMobsim(this.fixture.sc, events);
		qsim.run();

		Assert.assertTrue(handler.hasCollectedLink2Event);
		Assert.assertTrue(handler.hasCollectedLink3Event );
	}

	private static class MixedLanesEventsHandler implements LinkEnterEventHandler {

		boolean hasCollectedLink3Event = false;
		boolean hasCollectedLink2Event = false;
		private SignalsMixedLaneTestFixture fixture;

		public MixedLanesEventsHandler(SignalsMixedLaneTestFixture fixture) {
			this.fixture = fixture;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().equals(this.fixture.id2)){
				Assert.assertEquals(this.fixture.pid1, event.getPersonId());
				hasCollectedLink2Event = true;
			}
			else if (event.getLinkId().equals(this.fixture.id3)){
				Assert.assertEquals(this.fixture.pid2, event.getPersonId());
				hasCollectedLink3Event = true;
			}
		}

		@Override
		public void reset(int iteration) {
		}

	}

}
