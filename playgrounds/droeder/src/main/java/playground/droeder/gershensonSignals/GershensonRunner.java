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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import opendap.util.dasTools;

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
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.AverageTTHandler;





/**
 * @author droeder
 *
 */
public class GershensonRunner implements AgentStuckEventHandler {
	
	private int u;
	private int n;
	private double cap;
	private double d;
	
	private Map<Id, Id> corrGroups;
	private Map<Id, List<Id>> compGroups;
	
	private Map<Integer, Double> averageTT;
	private static double avTT = 0;
	
	private static Map<Integer, Map<Integer, Double>> nAndUT = new LinkedHashMap<Integer, Map<Integer,Double>>();
	private static LinkedHashMap<Number, Number> nAndT = new LinkedHashMap<Number, Number>();
	
	
	private AverageTTHandler handler1;
	private CarsOnLinkLaneHandler handler2;
	
	// "D" run denver -- "G" run gershensonTestNetwork
	private static final String config = "D";
	
	private static final Logger log = Logger.getLogger(GershensonRunner.class);

	
	public void runScenario (final String configFile){
		String conf = null;
		
		if (configFile == "G"){
			log.info("start gershensonTest");
			GershensonScenarioGenerator gsg = new GershensonScenarioGenerator();
			gsg.createScenario();
			conf = DaPaths.GTEST + "gershensonConfigFile2.xml";		
		}else if (configFile == "D"){
			log.info("start Denver");
			DenverScenarioGenerator dsg = new DenverScenarioGenerator();
			dsg.createScenario();
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
		
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				CalculateSignalGroups csg = new CalculateSignalGroups();
				Map<Id, SignalGroupDefinition> groups = c.getScenario().getSignalSystems().getSignalGroupDefinitions();
				corrGroups = csg.calculateCorrespondingGroups(groups, c.getNetwork());
				compGroups = csg.calculateCompetingGroups(corrGroups, groups, c.getNetwork());
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				handler2 = new CarsOnLinkLaneHandler(groups, d);
				event.getControler().getEvents().addHandler(handler1);
				event.getControler().getEvents().addHandler(handler2);
				
				//enable live-visualization
				event.getControler().setMobsimFactory(new OTFVisMobsimFactoryImpl());
				
				//output of stucked vehicles
				event.getControler().getEvents().addHandler(GershensonRunner.this);	
			}
			
		}
		);
		
		c.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				handler1.reset(event.getIteration());
				handler2.reset(event.getIteration());
			}
		});
		
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				avTT = handler1.getAverageTravelTime();
			}
		});		
		
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {
				
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
				adaptiveController.setParameters(n, u, cap);
				adaptiveController.init(corrGroups, compGroups, e.getQueueSimulation().getQueueNetwork(), handler2);
				handler2.setQNetwork(e.getQueueSimulation().getQueueNetwork());
				
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
		
//		c.getQueueSimulationListener().add(new SimulationAfterSimStepListener<QSim>() {
//			public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QSim> e) {
//				QSim qs = e.getQueueSimulation();
//			}
//		});
	
	}
	
//	private void startVisualizer(Config config){
//		String[] args = {config.controler().getOutputDirectory() +
//				"/ITERS/it." + config.controler().getLastIteration() +
//				"/" + config.controler().getLastIteration() + ".otfvis.mvi"};
//		OTFVis.main(args);
//	}
	
	public void setU (int u){
		this.u = u;
	}
	public void setN (int n){
		this.n = n;
	}
	public void setCap (double cap){
		this.cap = cap;
	}
	public void setD (double d){
		this.d = d;
	}
	
	
	public static void main(String[] args) {
//		DaBarChart barChart = new DaBarChart();
//		double cap = 0;
		
		GershensonRunner runner = new GershensonRunner();
		runner.setN(300);
		runner.setU(6);
		runner.setCap(0.90);
		runner.setD(100);
		runner.runScenario(config);
		
//		for (int c = 0; c < 20; c++){
//			barChart = new DaBarChart();
//			cap = (80.00+(double)c)/100.00;
//			for (int u = 4; u < 18; u++){
//				nAndT = new LinkedHashMap<Number, Number>();
//				for (int n = 8; n < 21; n++){
//					runner = new GershensonRunner();
//					Gbl.reset();
//					runner.setU(u);
//					runner.setN(n);
//					runner.setCap(cap);
//					runner.runScenario(config);
//					nAndT.put(n, avTT);
//				}
//				barChart.addSeries("u=" + String.valueOf(u), nAndT);
//	//			nAndUT.put(n, uAndT);
//			}	
//			new DaChartWriter().writeChart(DaPaths.OUTPUT+"DENVER\\Charts_10_03_02\\" + "u4-17_n8-20_cap" + 
//					String.valueOf(cap), 1600, 1024, barChart.createChart("capacityFactor = " + String.valueOf(cap), "number of waiting cars n [ ]", "average travelTime t [s]", 1800));
//		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.error("got stuck event for agent: " + event.getPersonId() + " on Link " + event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
	}
	

}
