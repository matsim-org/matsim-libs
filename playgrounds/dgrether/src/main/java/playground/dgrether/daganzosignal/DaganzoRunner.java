/* *********************************************************************** *
 * project: org.matsim.*
 * DaganzoRunner
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;

import playground.dgrether.analysis.charts.DgTravelTimeCalculatorChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.InOutGraphWriter;
import playground.dgrether.linkanalysis.TTGraphWriter;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;
import playground.dgrether.signalsystems.DgGreenSplitPerIterationGraph;
import playground.dgrether.signalsystems.DgSignalGreenSplitHandler;


/**
 * @author dgrether
 *
 */
public class DaganzoRunner {

	private static final Logger log = Logger.getLogger(DaganzoRunner.class);

	private TTInOutflowEventHandler handler3, handler4;

  private DgSignalGreenSplitHandler signalGreenSplitHandler;
  private DgGreenSplitPerIterationGraph greenSplitPerIterationGraph;

	public DaganzoRunner(){}

	public void runScenario(final String configFile){
		String conf = null;
		if (configFile == null) {
			DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
			conf = scenarioGenerator.configOut;
		}
		else {
			conf = configFile;
		}
//		String c = DgPaths.STUDIESDG + "daganzo/daganzoConfig2Agents.xml";
		Controler controler = new Controler(conf);
//		controler.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		this.addListener(controler);
		this.addQueueSimListener(controler);
		controler.run();
//		this.startVisualizer(config);
	}

	private void addQueueSimListener(final Controler controler) {
		controler.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
			//add the adaptive controller as events listener
			public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				AdaptiveController adaptiveController = (AdaptiveController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				controler.getEvents().addHandler(adaptiveController);
			}
		});
		//remove the adaptive controller
		controler.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
			public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
				QSim qs = e.getQueueSimulation();
				AdaptiveController adaptiveController = (AdaptiveController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				controler.getEvents().removeHandler(adaptiveController);
			}
		});


	}

	private void addListener(Controler c) {
		//add some EventHandler to the EventsManager after the controler is started
		handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
		handler4 = new TTInOutflowEventHandler(new IdImpl("4"));

		signalGreenSplitHandler = new DgSignalGreenSplitHandler();
		signalGreenSplitHandler.addSignalSystem(new IdImpl("1"));
		
		greenSplitPerIterationGraph = new DgGreenSplitPerIterationGraph(c.getConfig().controler(), new IdImpl("1"));
		
		c.addControlerListener(new StartupListener() {

			public void notifyStartup(StartupEvent e) {
				e.getControler().getEvents().addHandler(handler3);
				e.getControler().getEvents().addHandler(handler4);
				e.getControler().getEvents().addHandler(signalGreenSplitHandler);

				
			}
		});
		

		//write some output after each iteration
		c.addControlerListener(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent e) {
				TTGraphWriter ttWriter = new TTGraphWriter();
				ttWriter.addTTEventHandler(handler3);
				ttWriter.addTTEventHandler(handler4);
				ttWriter.writeTTChart(e.getControler().getControlerIO().getIterationPath(e.getIteration()), e.getIteration());

				InOutGraphWriter inoutWriter = new InOutGraphWriter();
				inoutWriter.addInOutEventHandler(handler3);
				inoutWriter.addInOutEventHandler(handler4);
				inoutWriter.writeInOutChart(e.getControler().getControlerIO().getIterationPath(e.getIteration()), e.getIteration());
			
				DgTravelTimeCalculatorChart ttcalcChart = new DgTravelTimeCalculatorChart((TravelTimeCalculator)e.getControler().getTravelTimeCalculator());
				ttcalcChart.setStartTime(900.0);
				ttcalcChart.setEndTime(3600.0 * 2.5);
				List<Id> list = new ArrayList<Id>();
				list.add(new IdImpl("4"));
				ttcalcChart.addLinkId(list);
				list = new ArrayList<Id>();
				list.add(new IdImpl("3"));
				list.add(new IdImpl("5"));
				ttcalcChart.addLinkId(list);
				DgChartWriter.writeChart(e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "ttcalculator"), 
				    ttcalcChart.createChart());
				
				greenSplitPerIterationGraph.addIterationData(signalGreenSplitHandler, e.getIteration());
			}
		});
  	//write some output at shutdown
		c.addControlerListener(new ShutdownListener() {
			public void notifyShutdown(ShutdownEvent event) {
				DgCountPerIterationGraph chart = new DgCountPerIterationGraph(event.getControler().getConfig().controler());
				chart.addCountEventHandler(handler3);
				chart.addCountEventHandler(handler4);
				DgChartWriter.writeChart(event.getControler().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
			
				DgChartWriter.writeChart(event.getControler().getControlerIO().getOutputFilename("greensplit"), greenSplitPerIterationGraph.createChart());
			}
		});
	}

		private void startVisualizer(Config config) {
		String[] args = {config.controler().getOutputDirectory() +
				"/ITERS/it." + config.controler().getLastIteration() +
				"/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		OTFVis.main(args);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0){
			new DaganzoRunner().runScenario(null);
		}
		else {
			new DaganzoRunner().runScenario(args[0]);
		}
	}

}
