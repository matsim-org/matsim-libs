/* *********************************************************************** *
 * project: org.matsim.*
 * VisController.java
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
package playground.gregor.grips.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.gregor.JOGLSetup;

public class VisController {
	public static void main (String [] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		Config c = config;
		String simOutput = c.controler().getOutputDirectory();
		c.plans().setInputFile(simOutput + "/output_plans.xml.gz");
		c.otfVis().setMapOverlayMode(true);
		if (c.getQSimConfigGroup() == null) {
			c.addQSimConfigGroup(new QSimConfigGroup());
		}
		Scenario sc = ScenarioUtils.createScenario(c);
		ScenarioUtils.loadScenario(sc);
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(sc,events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(c,sc,events,qSim);
		JOGLSetup.configureJOGL();
		OTFClientLive.run(config,server);
		qSim.run();
	}
}
