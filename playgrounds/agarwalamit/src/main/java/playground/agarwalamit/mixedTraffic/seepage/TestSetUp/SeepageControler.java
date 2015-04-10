/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.seepage.TestSetUp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */
public class SeepageControler {
	 public static final String outputDir = "/Users/amit/Documents/repos/shared-svn/projects/mixedTraffic/seepage/xt_1Link/seepage/";
	 static final List<String> mainModes = Arrays.asList(TransportMode.car,TransportMode.bike);
	 static final Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	 static final String seepMode = "bike";
	 static final String isSeepModeStorageFree = "false";
	 
	private void run (){
		new CreateInputs().run();
		
		EventsManager manager = EventsUtils.createEventsManager();
		EventWriterXML eventWriterXML = new EventWriterXML(outputDir+"/events.xml");
		manager.addHandler(eventWriterXML);
		
		QSim qSim = createQSim(manager);
		qSim.run();
		eventWriterXML.closeFile();
		
//		new QPositionDataWriterForR().run();
//		new TravelTimeAnalyzer(outputDir).run();
	}
	
	public static void main(String[] args) {
		new SeepageControler().run();
//		CreateInputs inputs = new CreateInputs();
//		inputs.run();
//		Scenario sc = inputs.getScenario();
//		
//		Controler myController = new Controler(sc.getConfig());	
//		myController.setOverwriteFiles(true) ;
//		myController.setCreateGraphs(true);
//		myController.setMobsimFactory(new PatnaQSimFactory()); 
////		myController.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		myController.setDumpDataAtEnd(true);
//		myController.run();
//		myController.setMobsimFactory(new SeepageMobsimfactory());
	}
	
	private QSim createQSim (EventsManager manager){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		SeepageNetworkFactory netsimNetworkFactory = new SeepageNetworkFactory(queueWithBufferType);
		
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1,netsimNetworkFactory);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		for(String travelMode:mainModes){
			VehicleType mode = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode,VehicleType.class));
			mode.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(travelMode));
			mode.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(travelMode));
			modeVehicleTypes.put(travelMode, mode);
		}
		
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
}
