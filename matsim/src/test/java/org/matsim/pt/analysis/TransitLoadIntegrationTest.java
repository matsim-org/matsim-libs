/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.pt.analysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / senozon
 */
public class TransitLoadIntegrationTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void testIntegration() {
		final Config cfg = this.util.loadConfig("test/scenarios/pt-tutorial/config.xml");
		cfg.controler().setLastIteration(0);
		final Scenario s = ScenarioUtils.loadScenario(cfg);
		final Controler c = new Controler(s);
		final TransitLoad transitload = new TransitLoad();

		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				c.getEvents().addHandler(transitload);
			}
		});

		cfg.controler().setWritePlansInterval(0);
        c.getConfig().controler().setCreateGraphs(false);
        c.getConfig().controler().setWriteEventsInterval(0);
		c.setDumpDataAtEnd(false);
		c.run();

		TransitLine line = s.getTransitSchedule().getTransitLines().get(Id.create("Blue Line", TransitLine.class));
		TransitRoute route = line.getRoutes().get(Id.create("1to3", TransitRoute.class));
		TransitStopFacility stopFacility = s.getTransitSchedule().getFacilities().get(Id.create("2a", TransitStopFacility.class));
		Departure departure = route.getDepartures().get(Id.create("07", Departure.class));
		int load = transitload.getLoadAtDeparture(line, route, stopFacility, departure);

		Assert.assertEquals("wrong number of passengers.", 4, load);
	}
}
