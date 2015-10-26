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

package playground.jjoubert.roadpricing.senozon;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class SanralIntegrationTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * These tests are left in the playground, but since there has been huge 
	 * changes to the way in which road pricing is implemented in MATSim, there
	 * is no use in trying to maintain these tests (Oct 2014, JWJ). 
	 */
	@Test
	@Ignore
	public void testVehicleIdBasedTollCosts_distance() {
		SanralControler c = new SanralControler(utils.loadConfig(utils.getInputDirectory() + "config.xml"));
        c.getConfig().controler().setCreateGraphs(false);
        c.run();

		EventsManager em = (EventsManager) EventsUtils.createEventsManager();
		EventsCollector ec = new EventsCollector();
		em.addHandler(ec);
//		new MatsimEventsReader(em).readFile(utils.getOutputDirectory() + "ITERS/it.0/0.events.txt.gz");
		new MatsimEventsReader(em).readFile(utils.getOutputDirectory() + "ITERS/it.0/0.events.xml.gz");
		List<PersonMoneyEvent> events = new ArrayList<PersonMoneyEvent>();
		for (Event e : ec.getEvents()) {
			if (e instanceof PersonMoneyEvent) {
				events.add((PersonMoneyEvent) e);
			}
		}

		Assert.assertEquals("expected 3 money events", 3, events.size());

		double amount1 = SanralTollFactor.getTollFactor(Id.create("1", Vehicle.class), Id.createLinkId("2"), 22000);
		double amount2 = SanralTollFactor.getTollFactor(Id.create("1000000", Vehicle.class), Id.createLinkId("2"), 22000);
		double amount3 = SanralTollFactor.getTollFactor(Id.create("2000000", Vehicle.class), Id.createLinkId("2"), 22000);

		Assert.assertEquals(-2 * amount1, events.get(0).getAmount(), 1e-8);
		Assert.assertEquals(-2 * amount2, events.get(1).getAmount(), 1e-8);
		Assert.assertEquals(-2 * amount3, events.get(2).getAmount(), 1e-8);
	}
}
