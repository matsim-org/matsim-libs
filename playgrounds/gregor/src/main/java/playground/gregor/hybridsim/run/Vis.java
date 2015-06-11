/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.hybridsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.Branding;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.events.EventsReaderXMLv1ExtendedSim2DVersion;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class Vis {

	public static void main(String [] args) {

		String confFile = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/config.xml";

		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, confFile);
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		Sim2DConfig conf2d = Sim2DConfigUtils
				.loadConfig("/Users/laemmel/devel/hhw_hybrid/input/s2d_config_v0.3.xml");
		Sim2DScenario sc2d = Sim2DScenarioUtils.loadSim2DScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
		InfoBox iBox = new InfoBox(dbg, sc);
		dbg.addAdditionalDrawer(iBox);
		dbg.addAdditionalDrawer(new Branding());
		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		dbg.addAdditionalDrawer(qDbg);
		
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(qDbg);
		em.addHandler(dbg);
		new EventsReaderXMLv1ExtendedSim2DVersion(em).parse("/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/output/ITERS/it.100/100.events.xml.gz");
	}
}
