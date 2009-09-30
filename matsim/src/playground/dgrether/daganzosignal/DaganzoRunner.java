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
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.run.OTFVis;

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
		Controler controler = new Controler(scenarioGenerator.configOut);
		controler.setOverwriteFiles(true);
		Config config = controler.getConfig();
		
		this.addListener(controler);
		
		controler.run();
		this.startVisualizer(config);
	}
	
	private void addListener(Controler c) {
//		handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
		handler4 = new TTInOutflowEventHandler(new IdImpl("4"));
		c.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent e) {
//				e.getControler().getEvents().addHandler(handler3);
				e.getControler().getEvents().addHandler(handler4);
			}});

		
		c.addControlerListener(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent e) {
				TTGraphWriter ttWriter = new TTGraphWriter();
//				ttWriter.addTTEventHandler(handler3);
				ttWriter.addTTEventHandler(handler4);
				ttWriter.writeTTChart(e.getControler().getIterationPath(e.getIteration()), e.getIteration());
				
//				InOutGraphWriter inoutWriter = new InOutGraphWriter();
//				inoutWriter.addInOutEventHandler(handler3);
//				inoutWriter.addInOutEventHandler(handler4);
//				inoutWriter.writeInOutChart(e.getControler().getIterationPath(event.getIteration()), event.getIteration());
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
