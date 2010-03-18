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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.AverageTTHandler;
import playground.droeder.charts.DaBarChart;
import playground.droeder.charts.DaChartWriter;





/**
 * @author droeder
 *
 */
public class GershensonRunner implements AgentStuckEventHandler {
	
	private int u;
	private int n;
	private double cap;
	private double d;
	private int maxGreen;
	
	private Map<Id, Id> corrGroups;
	private Map<Id, List<Id>> compGroups;
	private Map<Id, Id> mainOutLinks;
	
	private Map<Integer, Double> averageTT;
	private static double avTT = 0;
	
	private static Map<Integer, Map<Integer, Double>> nAndUT = new LinkedHashMap<Integer, Map<Integer,Double>>();
	private static LinkedHashMap<Number, Number> nAndT = new LinkedHashMap<Number, Number>();
	
	
	private AverageTTHandler handler1;
	private CarsOnLinkLaneHandler handler2;
	
	// "D" run denver -- "G" run gershensonTestNetwork
	private static final String config = "G";
	// "G" for GershensonController -- "I" for InterimController
	private static final String controller = "G";
	
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
			if(controller == "G"){
				dsg.controllerClass =  GershensonAdaptiveTrafficLightController.class.getCanonicalName();
			}else if(controller =="I"){
				dsg.controllerClass  = AdaptiveInterimSignalController.class.getCanonicalName();
			}
			dsg.createScenario();
			conf = DaPaths.OUTPUT + "Denver\\denverConfig.xml";
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
				Map<Id, SignalGroupDefinition> groups = c.getScenario().getSignalSystems().getSignalGroupDefinitions();
				CalculateSignalGroups csg = new CalculateSignalGroups(groups, c.getNetwork());
				corrGroups = csg.calculateCorrespondingGroups();
				compGroups = csg.calculateCompetingGroups(corrGroups);
				mainOutLinks = csg.calculateMainOutlinks();
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				handler2 = new CarsOnLinkLaneHandler(groups, d, c.getNetwork());
				event.getControler().getEvents().addHandler(handler1);
				event.getControler().getEvents().addHandler(handler2);
				
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
//				AdaptiveSignalSystemControlerImpl adaptiveController;
				QSim qs = e.getQueueSimulation();
				if (controller == "G"){
					GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
					adaptiveController.setParameters(n, u, cap, maxGreen);
					adaptiveController.init(corrGroups, compGroups, mainOutLinks, e.getQueueSimulation().getQueueNetwork(), handler2);
					c.getEvents().addHandler(adaptiveController);				
				}else{
					AdaptiveInterimSignalController adaptiveController = (AdaptiveInterimSignalController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
					adaptiveController.setParameters(n, u, cap, 0);
					adaptiveController.init(corrGroups, compGroups, mainOutLinks,e.getQueueSimulation().getQueueNetwork(), handler2);
					c.getEvents().addHandler(adaptiveController);
				}
				
				handler2.setQNetwork(e.getQueueSimulation().getQueueNetwork());
				
				
			}
		});
		//remove the adaptive controller
		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				if (controller == "G"){
					GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
					c.getEvents().removeHandler(adaptiveController);
				}else{
					AdaptiveInterimSignalController adaptiveController = (AdaptiveInterimSignalController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
					c.getEvents().removeHandler(adaptiveController);

				}
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
	public void setMaxGreen(int maxGreen){
		this.maxGreen = maxGreen;
	}
	
	
	public static void main(String[] args) {
		DaBarChart barChart = new DaBarChart();
		double cap ;
		double temp;
		
		GershensonRunner runner = new GershensonRunner();
		runner.setN(30);
		runner.setU(15);
		runner.setCap(0.90);
		runner.setD(150);
		runner.setMaxGreen(60);
		runner.runScenario(config);
		
//		for (int d = 80; d<151; d = d+10){
//			for (int c = 0; c < 16; c++){
//				temp = 0;
//				barChart = new DaBarChart();
//				cap = (80.00+(double)c)/100.00;
//				for (int u = 5; u < 30; u = u + 2){
//					nAndT = new LinkedHashMap<Number, Number>();
//					for (int n = 10; n < 401; n = n+10){
//						runner = new GershensonRunner();
//						Gbl.reset();
//						runner.setU(u);
//						runner.setN(n);
//						runner.setCap(cap);
//						runner.setD(d);
//						runner.setMaxGreen(0);
//						runner.runScenario(config);
//						nAndT.put(n, avTT);
//						if(avTT>temp){
//							temp = avTT;
//						}
//					}
//					barChart.addSeries("u=" + String.valueOf(u), nAndT);
//		//			nAndUT.put(n, uAndT);
//				}	
//				new DaChartWriter().writeChart(DaPaths.OUTPUT+"DENVER\\Charts_10_03_17.2\\" + "n10-400_u5-30_cap" + 
//						String.valueOf(cap) + "_d" + String.valueOf(d), 2560, 1600, barChart.createChart("capacityFactor = " + String.valueOf(cap) + " d = " + String.valueOf(d), "waitingCars * redTime [1*s]", "average travelTime t [s]", temp));
//			}
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
