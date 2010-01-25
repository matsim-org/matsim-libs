/* *********************************************************************** *
 * project: org.matsim.*
 * DenverStarter
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
package playground.dgrether.signalVis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQSim;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DenverStarter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile = DgPaths.STUDIESDG + "denver/dgConfig.xml";
		
		ScenarioLoaderImpl scl = new ScenarioLoaderImpl(configFile);
		Scenario sc = scl.loadScenario();
		sc.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		EventsManagerImpl e = new EventsManagerImpl();
		
		OTFVisQSim sim = new OTFVisQSim(sc, e);
		sim.run();
		
		
	}

}
