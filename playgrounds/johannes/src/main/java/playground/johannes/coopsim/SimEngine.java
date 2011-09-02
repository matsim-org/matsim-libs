/* *********************************************************************** *
 * project: org.matsim.*
 * SimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.eval.EvalEngine;
import playground.johannes.coopsim.mental.MentalEngine;
import playground.johannes.coopsim.pysical.PhysicalEngine;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class SimEngine {
	
	private static final Logger logger = Logger.getLogger(SimEngine.class);

	private final SocialGraph graph;
	
	private final MentalEngine mentalEngine;
	
	private final PhysicalEngine physicalEngine;
	
	private final EvalEngine evalEngine;
	
	private final EventsManager eventsManager;
	
	private int sampleInterval = 500;
	
	private int logInterval = 100;
	
	private TrajectoryAnalyzerTask analyzerTask;
	
	private String outpurDirectory;
	
	public SimEngine(SocialGraph graph, MentalEngine mentalEngine, PhysicalEngine physicalEngine, EvalEngine evalEngine) {
		this.graph = graph;
		this.mentalEngine = mentalEngine;
		this.physicalEngine = physicalEngine;
		this.evalEngine = evalEngine;
		
		this.eventsManager = EventsUtils.createEventsManager();
		
		LoggerUtils.setVerbose(false);
		for(EventHandler handler : evalEngine.getEventHandler())
			eventsManager.addHandler(handler);
		LoggerUtils.setVerbose(true);
	}
	
	public void setSampleInterval(int interval) {
		this.sampleInterval = interval;
	}
	
	public void setLogInerval(int interval) {
		this.logInterval = interval;
	}
	
	public void setAnalyzerTask(TrajectoryAnalyzerTask task, String output) {
		this.analyzerTask = task;
		this.outpurDirectory = output;
	}
	
	public void run(int iterations) {
		logger.info("Drawing initial full sample...");
		drawSample(0);
		
		logger.info(String.format("Running markov chain for %1$s iterations.", iterations));
		mentalEngine.clearStatistics();
		for(int i = 1; i <= iterations; i++) {
			LoggerUtils.setVerbose(false);
			step();
			LoggerUtils.setVerbose(true);
			
			if(i % logInterval == 0) {
				double avrTransitionProba = mentalEngine.getTransitionProbaSum()/(double)logInterval;
				logger.info(String.format("[%1$s] Accepted %2$s states, Average transition probability %3$s.", i, mentalEngine.getAcceptedStates(), avrTransitionProba));
				mentalEngine.clearStatistics();
			}
			
			if(i % sampleInterval == 0) {
				logger.info("Drawing full sample...");
				drawSample(i);
			}
		}
	}
	
	public void step() {
		/*
		 * run mental layer
		 */
		Set<SocialVertex> egos = mentalEngine.nextState();
		/*
		 * get alters
		 */
		Set<SocialVertex> alters = new HashSet<SocialVertex>();
		for(SocialVertex ego : egos) {
			for(SocialVertex alter : ego.getNeighbours()) {
				if(!egos.contains(alter)) {
					alters.add(alter);
				}
			}
		}
		/*
		 * get plans for physical layer
		 */
		Set<Plan> plans = new HashSet<Plan>();
		for(SocialVertex ego : egos)
			plans.add(ego.getPerson().getPerson().getSelectedPlan());
		
		for(SocialVertex alter : alters)
			plans.add(alter.getPerson().getPerson().getSelectedPlan());
		/*
		 * run physical layer
		 */
		evalEngine.init();
		physicalEngine.run(plans, eventsManager);
		/*
		 * evaluate plans
		 */
		evalEngine.run();
		/*
		 * accept/reject state
		 */
		mentalEngine.acceptRejectState(egos);
	}
	
	public void drawSample(int iter) {
		LoggerUtils.setVerbose(false);
		
		evalEngine.init();
		
		Set<Plan> plans = new HashSet<Plan>();
		for(SocialVertex v : graph.getVertices()) {
			plans.add(v.getPerson().getPerson().getSelectedPlan());
		}
		
		TrajectoryEventsBuilder builder = new TrajectoryEventsBuilder(plans);
		builder.reset(iter);
		eventsManager.addHandler(builder);
		
		physicalEngine.run(plans, eventsManager);
		
		eventsManager.removeHandler(builder);
		
		Set<Trajectory> trajectories = new HashSet<Trajectory>(builder.getTrajectories().values());
		
		evalEngine.run();
		
		LoggerUtils.setVerbose(true);
		try {
			String iterDir = String.format("%1$s/%2$s/", outpurDirectory, iter);
			new File(iterDir).mkdirs();
			TrajectoryAnalyzer.analyze(trajectories, analyzerTask, iterDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
