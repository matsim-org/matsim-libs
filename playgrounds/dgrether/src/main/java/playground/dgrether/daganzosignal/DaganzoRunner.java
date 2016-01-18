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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.dgrether.analysis.charts.DgTravelTimeCalculatorChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.TTGraphWriter;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;
import playground.dgrether.signalsystems.analysis.DgGreenSplitPerIterationGraph;
import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;


/**
 * @author dgrether
 *
 */
public class DaganzoRunner {

	private TTInOutflowEventHandler handler3, handler4;

  private DgSignalGreenSplitHandler signalGreenSplitHandler;
  private DgGreenSplitPerIterationGraph greenSplitPerIterationGraph;

	public DaganzoRunner(){}

	public void runScenario(final String configFile){
		String conf = null;
		if (configFile == null) {
			conf = DaganzoScenarioGenerator.DAGANZOBASEDIR + "dgconfig.xml";
//			DaganzoScenarioGenerator scgen = new DaganzoScenarioGenerator();
//			conf = scgen.configOut;
		}
		else {
			conf = configFile;
		}
		
		Config config = ConfigUtils.loadConfig(conf);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
//		if (scenario.getConfig().signalSystems().getSignalSystemConfigFile() == null){
//			DaganzoScenarioGenerator scgenerator = new DaganzoScenarioGenerator();
//			scgenerator.createSignalSystemsConfig(scenario);
//		}
		
		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		this.addControlerListener(controler);
//		this.addQSimListener(services);
		controler.run();
	}


	private void addControlerListener(MatsimServices c) {
		//add some EventHandler to the EventsManager after the services is started
		handler3 = new TTInOutflowEventHandler(Id.create("3", Link.class), Id.create("5", Link.class));
		handler4 = new TTInOutflowEventHandler(Id.create("4", Link.class));

		signalGreenSplitHandler = new DgSignalGreenSplitHandler();
		signalGreenSplitHandler.addSignalSystem(Id.create("1", SignalSystem.class));
		
		greenSplitPerIterationGraph = new DgGreenSplitPerIterationGraph(c.getConfig().controler(), Id.create("1", SignalSystem.class));
		
		c.addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent e) {
				e.getServices().getEvents().addHandler(handler3);
				e.getServices().getEvents().addHandler(handler4);
				e.getServices().getEvents().addHandler(signalGreenSplitHandler);
			}
		});
		

		//write some output after each iteration
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent e) {
				handler3.iterationsEnds(e.getIteration());
				handler4.iterationsEnds(e.getIteration());
				greenSplitPerIterationGraph.addIterationData(signalGreenSplitHandler, e.getIteration());

					TTGraphWriter ttWriter = new TTGraphWriter();
					ttWriter.addTTEventHandler(handler3);
					ttWriter.addTTEventHandler(handler4);
					ttWriter.writeTTChart(e.getServices().getControlerIO().getIterationPath(e.getIteration()), e.getIteration());

					//				InOutGraphWriter inoutWriter = new InOutGraphWriter();
					//				inoutWriter.addInOutEventHandler(handler3);
					//				inoutWriter.addInOutEventHandler(handler4);
					//				inoutWriter.writeInOutChart(e.getServices().getControlerIO().getIterationPath(e.getIteration()), e.getIteration());

					DgTravelTimeCalculatorChart ttcalcChart = new DgTravelTimeCalculatorChart((TravelTimeCalculator)e.getServices().getLinkTravelTimes());
					ttcalcChart.setStartTime(10.0);
					ttcalcChart.setEndTime(3600.0 * 2.5);
					List<Id<Link>> list = new ArrayList<>();
					list.add(Id.create("2", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("4", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("3", Link.class));
					list.add(Id.create("5", Link.class));
					ttcalcChart.addLinkId(list);
					DgChartWriter.writeChart(e.getServices().getControlerIO().getIterationFilename(e.getIteration(), "ttcalculator"),
							ttcalcChart.createChart());

				if ( e.getIteration() % 10 == 0 ) {
					DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getServices().getConfig().controler());
					chart.addCountEventHandler(handler3);
					chart.addCountEventHandler(handler4);
					DgChartWriter.writeChart(e.getServices().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
				}
			}
		});
  	//write some output at shutdown
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent e) {
				DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getServices().getConfig().controler());
				chart.addCountEventHandler(handler3, "count on link 3 & 5");
				chart.addCountEventHandler(handler4);
				DgChartWriter.writeChart(e.getServices().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());

				DgChartWriter.writeChart(e.getServices().getControlerIO().getOutputFilename("greensplit"), greenSplitPerIterationGraph.createChart());
			}
		});
	}

//private void addQSimListener(final Controler services) {
//services.getQueueSimulationListener().add(new SimulationInitializedListener<QSim>() {
//	//add the adaptive controller as events listener
//	public void notifySimulationInitialized(SimulationInitializedEvent<QSim> e) {
//		QSim qs = e.getQueueSimulation();
//		AdaptiveController adaptiveController = (AdaptiveController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(Id.create("1"));
//		services.getEvents().addHandler(adaptiveController);
//	}
//});
////remove the adaptive controller
//services.getQueueSimulationListener().add(new SimulationBeforeCleanupListener<QSim>() {
//	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent<QSim> e) {
//		QSim qs = e.getQueueSimulation();
//		AdaptiveController adaptiveController = (AdaptiveController) qs.getQSimSignalEngine().getSignalSystemControlerBySystemId().get(Id.create("1"));
//		services.getEvents().removeHandler(adaptiveController);
//	}
//});
//}

	
	

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
