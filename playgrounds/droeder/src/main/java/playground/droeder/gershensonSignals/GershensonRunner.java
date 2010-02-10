/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.gershensonSignals;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.systems.SignalGroupDefinition;





/**
 * @author droeder
 *
 */
public class GershensonRunner {
	
	private Map<Id, Id> corrGroups;
	private Map<Id, List<Id>> compGroups;
	
	private static final Logger log = Logger.getLogger(GershensonRunner.class);

	
	public void runScenario (final String configFile){
		String conf = null;
		
		if (configFile == null){
			log.info("no configfile given");
			GershensonScenarioGenerator sg = new GershensonScenarioGenerator();
			sg.createScenario();
			conf = sg.CONFIGOUTPUTFILE;		
		}
		else{
			conf = configFile;
		}
		
		Controler controler = new Controler(conf);	
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		
		this.addControlerListener(controler);
		this.addQueueSimListener(controler);
		controler.run();

		
//		this.startVisualizer(config);		
		
		
	}
	
	private void addControlerListener(final Controler controler) {
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				CalculateSignalGroups csg = new CalculateSignalGroups();
				Map<Id, SignalGroupDefinition> groups = controler.getScenario().getSignalSystems().getSignalGroupDefinitions();
				corrGroups = csg.calculateCorrespondingGroups(groups, controler.getNetwork());
				compGroups = csg.calculateCompetingGroups(corrGroups, groups, controler.getNetwork());
			}
			
		}
				
		);
	}
	
	private void addQueueSimListener(final Controler controler) {
		controler.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
			//add the adaptive controller as events listener
			public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				GershensonAdaptiveTrafficLightController adaptiveController = 
					(GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				adaptiveController.setCorrGroups(corrGroups);
				adaptiveController.setCompGroups(compGroups);
				adaptiveController.init(controler.getNetwork(), controler.getPopulation());
				
				controler.getEvents().addHandler(adaptiveController);
			}
		});
		//remove the adaptive controller
		controler.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				controler.getEvents().removeHandler(adaptiveController);
			}
		});


	}
	private void startVisualizer(Config config){
		String[] args = {config.controler().getOutputDirectory() +
				"/ITERS/it." + config.controler().getLastIteration() +
				"/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		OTFVis.main(args);
	}
	
	public static void main(String[] args) {
		
		new GershensonRunner().runScenario(null);
		
	}
	

}
