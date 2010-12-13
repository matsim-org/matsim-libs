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



package playground.mzilske.compo;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import playground.mzilske.pipeline.PipeTasks;
import playground.mzilske.pipeline.RunnableScenarioSourceManager;
import playground.mzilske.pipeline.ScenarioGroundFactory;
import playground.mzilske.pipeline.TaskConfiguration;
import playground.mzilske.pipeline.TaskManager;


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
		taskManagers.add(new RunnableScenarioSourceManager(new CreateNetwork(config)));
		taskManagers.add(new ScenarioGroundFactory().createTaskManagerImpl(new TaskConfiguration(config, null)));
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
		Pipeline pipeline = new Pipeline();
		pipeline.prepare(config);
		pipeline.execute();
	}
	
}


