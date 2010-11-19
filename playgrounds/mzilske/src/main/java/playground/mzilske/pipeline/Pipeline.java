/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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



package playground.mzilske.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;


public final class Pipeline {
	
	private static final Logger log = Logger.getLogger(Pipeline.class);

	private List<TaskManager> taskManagers = new ArrayList<TaskManager>();
	
	public Pipeline() {
		
	}

	public void prepare(Config config) {	
		buildTasks(config);
		connectTasks();	
	}

	private void buildTasks(Config config) {
		taskManagers.add(new ScenarioLoaderTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new EventsManagerTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new LogOutputEventHandlerTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new IteratorTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new MobsimTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new TravelTimeCalculatorTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new TravelCostCalculatorTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new RouterTaskManagerFactory().createTaskManagerImpl(config));
		if (config.controler().isLinkToLinkRoutingEnabled()) {
			taskManagers.add(new RouterInvertedNetTaskManagerFactory().createTaskManagerImpl(config));
		}
		taskManagers.add(new VehicleWatcherFactory().createTaskManagerImpl(config));
		taskManagers.add(new ScoringTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new PersonReplanningTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new IterationTerminatorTaskManagerFactory().createTaskManagerImpl(config));
		taskManagers.add(new ScenarioGroundFactory().createTaskManagerImpl(config));
	}

	private void connectTasks() {
		PipeTasks pipeTasks = new PipeTasks();
		for (TaskManager taskManager : taskManagers) {
			taskManager.connect(pipeTasks);
		}
		pipeTasks.assertIsComplete();
	}


	public void execute() {
		for (TaskManager taskManager : taskManagers) {
			taskManager.execute();
		}
	}

	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile("../matsim/examples/equil/config.xml");
		Pipeline pipeline = new Pipeline();
		pipeline.prepare(config);
		pipeline.execute();
	}
	
}


