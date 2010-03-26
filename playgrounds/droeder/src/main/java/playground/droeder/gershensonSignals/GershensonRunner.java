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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
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
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.AverageTTHandler;
import playground.droeder.Analysis.SignalSystems.SignalGroupStateTimeHandler;

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
	
	
//	private Map<Id, Id> corrGroups;
//	private Map<Id, List<Id>> compGroups;
	private Map<Id, Id> mainOutLinks;
	
	private CalculateSignalGroups csg;
	
	private Map<Id, Map<Id, SignalGroupDefinition>> signalSystems = new HashMap<Id, Map<Id,SignalGroupDefinition>>();
	private Map<Id, Map<Id, List<SignalGroupDefinition>>> newCorrGroups = new HashMap<Id, Map<Id,List<SignalGroupDefinition>>>();
	private Map<Id, Map<Id, Id>> newMainOutlinks = new HashMap<Id, Map<Id,Id>>();
	
	private Map<Integer, Double> averageTT;
	private static double avTT = 0;
	
//	private static Map<Integer, Map<Integer, Double>> nAndUT = new LinkedHashMap<Integer, Map<Integer,Double>>();
//	private static LinkedHashMap<Number, Number> nAndT = new LinkedHashMap<Number, Number>();
	
	
	private AverageTTHandler handler1;
	private CarsOnLinkLaneHandler handler2;
	private SignalGroupStateTimeHandler handler3;
	
	// "D" run denver -- "G" run gershensonTestNetwork --- "C" run cottbus
	private static final String config = "C";
	
	private static final Logger log = Logger.getLogger(GershensonRunner.class);

	
	public void runScenario (final String configFile){
		String conf = null;
		
		if (configFile == "G"){
			log.info("start gershensonTest");
			GershensonScenarioGenerator gsg = new GershensonScenarioGenerator();
			gsg.createScenario();
			conf = DaPaths.DASTUDIES + "gershenson\\gershensonConfigFile2.xml";		
		}else if (configFile == "D"){
			log.info("start Denver");
			DenverScenarioGenerator dsg = new DenverScenarioGenerator();
			dsg.createScenario();
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
		}else if (configFile == "opti"){
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
		}
		else if (configFile == "C"){
			conf = DaPaths.DASTUDIES + "cottbus\\cottbusConfig.xml";
		}
		else{
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
				
				for (Entry<Id, SignalGroupDefinition> e : groups.entrySet()){
					if(signalSystems.containsKey(e.getValue().getSignalSystemDefinitionId())){
						signalSystems.get(e.getValue().getSignalSystemDefinitionId()).put(e.getValue().getId(), e.getValue());
					}else {
						signalSystems.put(e.getValue().getSignalSystemDefinitionId(), new HashMap<Id, SignalGroupDefinition>());
						signalSystems.get(e.getValue().getSignalSystemDefinitionId()).put(e.getValue().getId(), e.getValue());
					}
				}
				
				for (Entry<Id, Map<Id, SignalGroupDefinition>> e : signalSystems.entrySet()){
					csg = new CalculateSignalGroups(e.getValue(), c.getNetwork());
					newCorrGroups.put(e.getKey(), csg.calcCorrGroups());
					newMainOutlinks.put(e.getKey(), csg.calculateMainOutlinks());
				}
				
				
				csg = new CalculateSignalGroups(groups, c.getNetwork());
//				corrGroups = csg.calculateCorrespondingGroups();
//				compGroups = csg.calculateCompetingGroups(corrGroups);
				mainOutLinks = csg.calculateMainOutlinks();
				
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				handler2 = new CarsOnLinkLaneHandler(groups, d, c.getNetwork());
				handler3 = new SignalGroupStateTimeHandler();
				
				event.getControler().getEvents().addHandler(handler1);
				event.getControler().getEvents().addHandler(handler2);
				event.getControler().getEvents().addHandler(handler3);
				
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
				handler3.reset(event.getIteration());
			}
		});
		
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				avTT = handler1.getAverageTravelTime();
				handler3.writeToTxt(DaPaths.OUTPUT+ "denver\\ITERS\\it." + event.getIteration() + "\\greenTimes.txt");
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
				
				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptivController adaptiveController = (DaAdaptivController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					adaptiveController.setParameters(n, u, cap, maxGreen);
					adaptiveController.init(newCorrGroups.get(ee.getKey()), newMainOutlinks.get(ee.getKey()), e.getQueueSimulation().getQNetwork(), handler2);
					c.getEvents().addHandler(adaptiveController);	
				}
				
//				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
//				adaptiveController.setParameters(n, u, cap, maxGreen);
//				adaptiveController.init(corrGroups, compGroups, mainOutLinks, e.getQueueSimulation().getQNetwork(), handler2);
//				c.getEvents().addHandler(adaptiveController);				
				
				handler2.setQNetwork(e.getQueueSimulation().getQNetwork());

				qs.getEventsManager().addHandler(handler3);
				
			}
		});
		//remove the adaptive controller
		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptivController adaptiveController = (DaAdaptivController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					c.getEvents().removeHandler(adaptiveController);	
				}
//				GershensonAdaptiveTrafficLightController adaptiveController = (GershensonAdaptiveTrafficLightController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
//				c.getEvents().removeHandler(adaptiveController);

			}
		});
		
//		c.getQueueSimulationListener().add(new SimulationAfterSimStepListener<QSim>() {
//			public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QSim> e) {
//				QSim qs = e.getQueueSimulation();
//			}
//		});
	
	}
	
	
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
	public double getAvTT(){
		return this.avTT;
	}
	
	
	public static void main(String[] args) {
//		DaBarChart barChart = new DaBarChart();
//		double cap ;
//		double temp;
		
		GershensonRunner runner = new GershensonRunner();
		runner.setN(142);
		runner.setU(21);
		runner.setCap(0.63);
		runner.setD(45);
		runner.setMaxGreen(30);
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
