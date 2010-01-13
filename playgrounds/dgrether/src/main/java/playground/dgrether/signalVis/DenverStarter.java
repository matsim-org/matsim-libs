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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

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
		EventsManagerImpl e = new EventsManagerImpl();
		
		DgOnTheFlyQueueSimQuad sim = new DgOnTheFlyQueueSimQuad(sc, e);
		sim.setLaneDefinitions(((ScenarioImpl) sc).getLaneDefinitions());
		sim.setSignalSystems(((ScenarioImpl) sc).getSignalSystems(), ((ScenarioImpl) sc).getSignalSystemConfigurations());
		sim.run();
		
		
	}

}
