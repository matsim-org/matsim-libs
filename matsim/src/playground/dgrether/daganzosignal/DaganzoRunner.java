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

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeCleanupListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.run.OTFVis;

import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.InOutGraphWriter;
import playground.dgrether.linkanalysis.TTGraphWriter;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;


/**
 * @author dgrether
 *
 */
public class DaganzoRunner {
	
	private static final Logger log = Logger.getLogger(DaganzoRunner.class);
	
	private TTInOutflowEventHandler handler3, handler4;
	
	public DaganzoRunner(){}

	public void runScenario(){
		DaganzoScenarioGenerator scenarioGenerator = new DaganzoScenarioGenerator();
		String c = scenarioGenerator.configOut;
//		String c = DgPaths.STUDIESDG + "daganzo/daganzoConfig2Agents.xml"; 
		Controler controler = new Controler(c);
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		
		this.addListener(controler);
		this.addQueueSimListener(controler);
		controler.run();
//		this.startVisualizer(config);
	}
	
	private void addQueueSimListener(final Controler controler) {
		controler.getQueueSimulationListener().add(new QueueSimulationInitializedListener() {
			public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
				QueueSimulation qs = e.getQueueSimulation();
				AdaptiveController adaptiveController = (AdaptiveController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				controler.getEvents().addHandler(adaptiveController);
			}
		});
		
		controler.getQueueSimulationListener().add(new QueueSimulationBeforeCleanupListener() {
			public void notifySimulationBeforeCleanup(QueueSimulationBeforeCleanupEvent e) {
				QueueSimulation qs = e.getQueueSimulation();
				AdaptiveController adaptiveController = (AdaptiveController) qs.getQueueSimSignalEngine().getSignalSystemControlerBySystemId().get(new IdImpl("1"));
				controler.getEvents().removeHandler(adaptiveController);
			}
		});
		
		
	}

	private void addListener(Controler c) {
		handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
		handler4 = new TTInOutflowEventHandler(new IdImpl("4"));
		c.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent e) {
				e.getControler().getEvents().addHandler(handler3);
				e.getControler().getEvents().addHandler(handler4);
			}});

		
		c.addControlerListener(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent e) {
				TTGraphWriter ttWriter = new TTGraphWriter();
				ttWriter.addTTEventHandler(handler3);
				ttWriter.addTTEventHandler(handler4);
				ttWriter.writeTTChart(e.getControler().getIterationPath(e.getIteration()), e.getIteration());
				
				InOutGraphWriter inoutWriter = new InOutGraphWriter();
				inoutWriter.addInOutEventHandler(handler3);
				inoutWriter.addInOutEventHandler(handler4);
				inoutWriter.writeInOutChart(e.getControler().getIterationPath(e.getIteration()), e.getIteration());
			}
		});
		
		c.addControlerListener(new ShutdownListener() {
			public void notifyShutdown(ShutdownEvent event) {
				DgCountPerIterationGraph chart = new DgCountPerIterationGraph();
				chart.addCountEventHandler(handler3);
				chart.addCountEventHandler(handler4);
				DgChartWriter.writeChart(event.getControler().getNameForOutputFilename("countPerIteration"), chart.createChart());
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
		new DaganzoRunner().runScenario();
	}

}
