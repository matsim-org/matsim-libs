/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaOTFVisMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.sylvia;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.DefaultSignalModelFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaSignalModelFactory;
import playground.dgrether.utils.DgOTFVisUtils;



/**
 * @author dgrether
 *
 */
public class SylviaOTFVisMain {

	public void playCottbus(String configFileName){
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFileName);
		ScenarioImpl scenario = (ScenarioImpl) loader.loadScenario();
		this.playCottbus(scenario);
	}	
	
	public void playCottbus(ScenarioImpl scenario){
		DgOTFVisUtils.preparePopulation4Simulation(scenario);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		scenario.getConfig().otfVis().setAgentSize(40.0f);
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);

		DgSensorManager sensorManager = new DgSensorManager(scenario.getNetwork());
		sensorManager.setLaneDefinitions(scenario.getLaneDefinitions());
		events.addHandler(sensorManager);
		
		FromDataBuilder modelBuilder = new FromDataBuilder(signalsData, 
				new DgSylviaSignalModelFactory(new DefaultSignalModelFactory(), sensorManager) , events);
		SignalSystemsManager signalManager = modelBuilder.createAndInitializeSignalSystemsManager();
		
		
		SignalEngine engine = new QSimSignalEngine(signalManager);
		QSim otfVisQSim = new QSim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		OTFVisMobsimFeature qSimFeature = new OTFVisMobsimFeature(otfVisQSim);
		otfVisQSim.addFeature(qSimFeature);
		
		QSim client = otfVisQSim;
		client.run();

	}
	



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SylviaOTFVisMain sylviaMain = new SylviaOTFVisMain();
		sylviaMain.playCottbus("/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/config.xml");
	}

}
