/* *********************************************************************** *
 * project: org.matsim.*
 * VisFirstIteration
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
package playground.dgrether.daganzosignal;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.dgrether.signalVis.DgOnTheFlyQueueSimQuad;


/**
 * @author dgrether
 *
 */
public class VisFirstIteration {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
		scenarioGenerator.createScenario();
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenarioGenerator.configOut);
		loader.loadScenario();
		ScenarioImpl sc = loader.getScenario();
		
		EventsManagerImpl events = new EventsManagerImpl();
		DgOnTheFlyQueueSimQuad visSim = new DgOnTheFlyQueueSimQuad(sc, events);
		visSim.setLaneDefinitions(sc.getLaneDefinitions());
		visSim.setSignalSystems(sc.getSignalSystems(), sc.getSignalSystemConfigurations());
		visSim.run();
		
	}

}
