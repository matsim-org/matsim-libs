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
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.vis.otfvis.OTFVisMobsimFactoryImpl;

import playground.droeder.DaPaths;
import playground.droeder.Analysis.AgentWaitHandler;
import playground.droeder.Analysis.AverageTTHandler;
import playground.droeder.Analysis.SignalSystems.SignalGroupStateTimeHandler;
import playground.droeder.charts.DaBarChart;
import playground.droeder.charts.DaChartWriter;
import playground.droeder.charts.DaSignalPlanChart;

/**
 *
 * runs the denver-, cottbus- or gershensonTest-Scenario with the DaAdaptiveController
 *
 * @author droeder
 *
 */
public class GershensonRunner implements AgentStuckEventHandler {

	private String output =  DaPaths.OUTPUT;

	private boolean liveVis;
	private boolean writeSignalPlans;
	private double signalPlanMin;
	private double signalPlanMax;
	private double startTime;

	private int u;
	private int n;
	private double cap;
	private double d;
	private int maxRed;

	private CalculateSignalGroups csg;

	private Map<Id, Map<Id, SignalGroupDefinition>> signalSystems = new HashMap<Id, Map<Id,SignalGroupDefinition>>();
	private Map<Id, Map<Id, List<SignalGroupDefinition>>> newCorrGroups = new HashMap<Id, Map<Id,List<SignalGroupDefinition>>>();
	private Map<Id, Map<Id, Id>> newMainOutlinks = new HashMap<Id, Map<Id,Id>>();

	private static double avTT = 0;
	private double absTT = 0;
	private double avWaitFactor = 0;
	private Map<String, Double> factors;

	private AverageTTHandler handler1;
	private CarsOnLinkLaneHandler handler2;
	private SignalGroupStateTimeHandler handler3;
	private AgentWaitHandler handler4;

	protected Map<Id, Id> mainOutLinks;


	private static final Logger log = Logger.getLogger(GershensonRunner.class);

	public GershensonRunner(int minRed, int n, double cap, double d, int maxRed, boolean writeSignalplans, boolean liveVis){
		this.u = minRed;
		this.n = n;
		this.cap = cap;
		this.d = d;
		this.maxRed = maxRed;
		this.writeSignalPlans= writeSignalplans;
		this.liveVis = liveVis;
	}


	public void runScenario (final String configFile){
		String conf = null;
		if (configFile == "G"){
			log.info("start gershensonTest");
			GershensonScenarioGenerator gsg = new GershensonScenarioGenerator();
			gsg.createScenario();
			conf = DaPaths.DASTUDIES + "gershenson\\gershensonConfigFile2.xml";
			output = output + "gershenson/";
		}else if (configFile == "D"){
			log.info("start Denver");
			DenverScenarioGenerator dsg = new DenverScenarioGenerator();
			dsg.createScenario();
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
			output = output + "denver/";
		}else if (configFile == "denver"){
			conf = DaPaths.DASTUDIES + "denver\\denverConfig.xml";
			output = output + "denver/";
		}
		else if (configFile == "C"){
			CottbusScenarioGenerator csg = new CottbusScenarioGenerator();
			csg.createScenario();
			conf = DaPaths.DASTUDIES + "cottbus\\cottbusConfig.xml";
			output = output + "cottbus/";
		}else if (configFile == "cottbus"){
			conf = DaPaths.DASTUDIES + "cottbus\\cottbusConfig.xml";
			output = output + "cottbus/";
		}else{
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
				mainOutLinks = csg.calculateMainOutlinks();
				handler1 = new AverageTTHandler(c.getPopulation().getPersons().size());
				handler2 = new CarsOnLinkLaneHandler(groups, d, c.getNetwork());
				handler3 = new SignalGroupStateTimeHandler();
				handler4 = new AgentWaitHandler(c.getNetwork());

				event.getControler().getEvents().addHandler(handler1);
				event.getControler().getEvents().addHandler(handler2);
				event.getControler().getEvents().addHandler(handler3);
				event.getControler().getEvents().addHandler(handler4);

				//enable live-visualization
				if (liveVis == true){
					event.getControler().setMobsimFactory(new OTFVisMobsimFactoryImpl());
				}

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
				handler4.reset(event.getIteration());
			}
		});

		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				avTT = handler1.getAverageTravelTime();
				absTT = handler1.getAbsoluteTT();
				avWaitFactor = handler4.getAverageWaitingFactor(c.getPopulation());
				factors = handler4.getFactors(c.getPopulation());

				DaSignalPlanChart planChart;
				if(writeSignalPlans == true){
					for(Entry<Id, TreeMap<Id, TreeMap<Double, SignalGroupState>>> e : handler3.getSystemGroupTimeStateMap().entrySet()){
						planChart = new DaSignalPlanChart();
						planChart.writeDataToTxt(output + "signalPlans/signalPlanData" + e.getKey() + ".txt", e.getValue());
						planChart.addData(e.getValue(), startTime, signalPlanMax);
						new DaChartWriter().writeChart(output + "signalPlans/signalPlan1Id" + e.getKey() + ".png", 512, 368, planChart.createSignalPlanChart("", "ID", "Zeit [s]", signalPlanMin, signalPlanMin+240));
						new DaChartWriter().writeChart(output + "signalPlans/signalPlan2Id" + e.getKey() + ".png", 512, 368, planChart.createSignalPlanChart("", "ID", "Zeit [s]", signalPlanMin+240, signalPlanMin+480));
						new DaChartWriter().writeChart(output + "signalPlans/signalPlan3Id" + e.getKey() + ".png", 512, 368, planChart.createSignalPlanChart("", "ID", "Zeit [s]", signalPlanMin+480, signalPlanMin+720));
					}

				}
			}
		});

		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {

			}
		});

	}

	// adaptiveController
	private void addQueueSimListener(final Controler c) {
		c.getQueueSimulationListener().add(new SimulationInitializedListener() {
			//add the adaptive controller as events listener
			@Override
			public void notifySimulationInitialized(SimulationInitializedEvent e) {
				QSim qs = (QSim) e.getQueueSimulation();

				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptiveController adaptiveController = (DaAdaptiveController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					adaptiveController.setParameters(n, u, cap, maxRed);
					adaptiveController.init(newCorrGroups.get(ee.getKey()), newMainOutlinks.get(ee.getKey()), ((QSim) e.getQueueSimulation()).getNetsimNetwork(), handler2);
					c.getEvents().addHandler(adaptiveController);
				}

				handler2.setQNetwork(((QSim) e.getQueueSimulation()).getNetsimNetwork());

				qs.getEventsManager().addHandler(handler3);

			}
		});
		//remove the adaptive controller
		c.getQueueSimulationListener().add(new SimulationBeforeCleanupListener() {
			@Override
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent e) {
				QSim qs = (QSim) e.getQueueSimulation();
				for(Entry<Id, Map<Id, SignalGroupDefinition>> ee: signalSystems.entrySet()){
					DaAdaptiveController adaptiveController = (DaAdaptiveController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(ee.getKey());
					if(writeSignalPlans == true){
						writeDemandOnRefLinkChart(adaptiveController, ee.getKey());
					}
						c.getEvents().removeHandler(adaptiveController);
				}
			}
		});
	}

	public void writeDemandOnRefLinkChart(DaAdaptiveController contr, Id signalSystem){
		DaBarChart chart = new DaBarChart();
		for (Entry<Id, SortedMap<Double, Double>> ee : contr.getDemandOnRefLink().entrySet()){
			chart.addSeries(ee.getKey().toString(), (Map)ee.getValue().subMap(signalPlanMin, signalPlanMax));
		}
		new DaChartWriter().writeChart(output + "signalPlans/demandOnLinkSystem" + signalSystem.toString() + ".png", 512, 368,
				chart.createChart("demandOnRefLink for SignalSystem " + signalSystem.toString(), "time [s]", "demand [cars]", 30));

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
	public void setMaxRed(int maxRed){
		this.maxRed = maxRed;
	}
	public double getAvTT(){
		return this.avTT;
	}

	public double getAbsTT(){
		return absTT;
	}

	public double getAvWaitFactor(){
		return this.avWaitFactor;
	}

	public Map<String, Double> getFactors(){
		return factors;
	}

	public void setSignalPlanBounds(double startTime, double min, double max){
		this.signalPlanMax = max;
		this.signalPlanMin = min;
		this.startTime = startTime;
	}


	public static void main(String[] args) {
		GershensonRunner runner;

//		Denver opti
		double cap  = 0.54;
		double d = 53;
		int minRed = 25;
		int carTime = 379;
		int maxRed = 31;
		runner = new GershensonRunner(minRed, carTime, cap, d, maxRed, false, true);
		runner.setSignalPlanBounds(21600, 22000, 22720);
		runner.runScenario("denver");

// 		//cottbus opti
//		double cap  = 0.82;
//		double d = 79;
//		int minRed = 20;
//		int carTime = 405;
//		int maxRed = 102;
//		runner = new GershensonRunner(minRed, carTime, cap, d, maxRed, false, false);
//		runner.setSignalPlanBounds(21600, 22000, 22720);
//		runner.runScenario("cottbus");

	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.error("got stuck event for agent: " + event.getPersonId() + " on Link " + event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
	}


}
