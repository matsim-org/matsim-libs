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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;





/**
 * @author droeder
 *
 */
public class GershensonRunner implements AgentStuckEventHandler {
	
	private Map<Id, Id> corrGroups;
	private Map<Id, List<Id>> compGroups;
	
	private Map<Integer, Double> averageTT;
	
	private AverageTTHandler handler1;
	
	// "D" run denver -- "G" run gershensonTestNetwork
	private static final String config = "D";
	
	private static final Logger log = Logger.getLogger(GershensonRunner.class);

	
	public void runScenario (final String configFile){
		String conf = null;
		
		if (configFile == "G"){
			log.info("start gershensonTest");
			GershensonScenarioGenerator gsg = new GershensonScenarioGenerator();
			gsg.createScenario();
			conf = gsg.CONFIGOUTPUTFILE;		
		}else if (configFile == "D"){
			log.info("start Denver");
			DenverScenarioGenerator dsg = new DenverScenarioGenerator();
			conf = dsg.CONFIGOUTPUTFILE;
		}else {
			conf = configFile;
		}
		
		Controler controler = new Controler(conf);	
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		
		this.addListener(controler);
		this.addQueueSimListener(controler);
		controler.run();

		
//		this.startVisualizer(config);		
		
		
	}
	
	private void addListener(final Controler c) {
		
		
		averageTT = new HashMap<Integer, Double>();
		
		
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				CalculateSignalGroups csg = new CalculateSignalGroups();
				Map<Id, SignalGroupDefinition> groups = c.getScenario().getSignalSystems().getSignalGroupDefinitions();
				corrGroups = csg.calculateCorrespondingGroups(groups, c.getNetwork());
				compGroups = csg.calculateCompetingGroups(corrGroups, groups, c.getNetwork());
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				event.getControler().getEvents().addHandler(handler1);
				
				//enable live-visualization
//				event.getControler().setMobsimFactory(new OTFVisMobsimFactoryImpl());
				
				//output of stucked vehicles
				event.getControler().getEvents().addHandler(GershensonRunner.this);	
			}
			
		}
		);
		
		c.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
//				handler1.reset(c.getIterationNumber());
			}
		});
		
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				handler1.setAverageIterationTime(event.getIteration());
			}
		});		
		
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {
				handler1.writeChart(event.getControler().getControlerIO().getOutputFilename("averageTimePerIteration"));
				handler1.writeChart2(event.getControler().getControlerIO().getOutputFilename("averageTimePerIteration2"));
			}
		});
		
	}
	
	private void addQueueSimListener(final Controler c) {
		c.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
			//add the adaptive controller as events listener
			public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				GershensonAdaptiveTrafficLightController adaptiveController = 
					(GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				adaptiveController.setCorrGroups(corrGroups);
				adaptiveController.setCompGroups(compGroups);
				adaptiveController.init(c.getScenario().getSignalSystems().getSignalGroupDefinitions(), e.getQueueSimulation().getQueueNetwork());
				
				c.getEvents().addHandler(adaptiveController);
			}
		});
		//remove the adaptive controller
		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				c.getEvents().removeHandler(adaptiveController);
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
		
		new GershensonRunner().runScenario(config);
		
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.error("got stuck event for agent: " + event.getPersonId() + " on Link " + event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
	}
	

}
