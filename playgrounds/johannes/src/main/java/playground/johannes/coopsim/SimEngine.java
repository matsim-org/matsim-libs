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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.collections.Tuple;

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
	
	private final TrajectoryEventsBuilder trajectoryBuilder;

	private int sampleInterval = 500;

	private int logInterval = 100;

	private TrajectoryAnalyzerTask analyzerTask;

	private String outpurDirectory;
	
	public SimEngine(SocialGraph graph, MentalEngine mentalEngine, PhysicalEngine physicalEngine, EvalEngine evalEngine) {
		this.graph = graph;
		this.mentalEngine = mentalEngine;
		this.physicalEngine = physicalEngine;
		this.evalEngine = evalEngine;
		
		Set<Person> persons = new HashSet<Person>(graph.getVertices().size());
		for(SocialVertex vertex : graph.getVertices())
			persons.add(vertex.getPerson().getPerson());
		trajectoryBuilder = new TrajectoryEventsBuilder(persons);
		
		this.eventsManager = EventsUtils.createEventsManager();

		LoggerUtils.setVerbose(false);
		eventsManager.addHandler(trajectoryBuilder);
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
		for (int i = 1; i <= iterations; i++) {
			LoggerUtils.setVerbose(false);
			step();
			LoggerUtils.setVerbose(true);

			if (i % logInterval == 0) {
				double avrTransitionProba = mentalEngine.getTransitionProbaSum() / (double) logInterval;
				logger.info(String.format("[%1$s] Accepted %2$s states, Average transition probability %3$s.", i,
						mentalEngine.getAcceptedStates(), avrTransitionProba));
				mentalEngine.clearStatistics();
			}

			if (i % sampleInterval == 0) {
				logger.info("Drawing full sample...");
				drawSample(i);
			}
		}
		
		try {
			physicalEngine.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void step() {
		/*
		 * run mental layer
		 */
		Profiler.resume("mental engine");
		List<SocialVertex> egos = mentalEngine.nextState();
		Profiler.pause("mental engine");
		/*
		 * get alters
		 */
		Profiler.resume("step preprocessing");
		Set<SocialVertex> altersLevel1 = new HashSet<SocialVertex>();
		for (SocialVertex ego : egos) {
			for (SocialVertex alter : ego.getNeighbours()) {
				if (!egos.contains(alter)) {
					altersLevel1.add(alter);
				}
			}
		}

		Set<SocialVertex> altersLevel2 = new HashSet<SocialVertex>();
		for (SocialVertex alter : altersLevel1) {
			for (SocialVertex neighbour : alter.getNeighbours()) {
				if (!egos.contains(neighbour) && !altersLevel1.contains(neighbour)) {
					altersLevel2.add(neighbour);
				}
			}
		}
		/*
		 * get plans for physical layer
		 */
		List<Plan> plans = new ArrayList<Plan>(egos.size() + altersLevel1.size() + altersLevel2.size());
		for (SocialVertex ego : egos)
			plans.add(ego.getPerson().getPerson().getSelectedPlan());

		List<Tuple<Plan, Double>> alter1Scores = new ArrayList<Tuple<Plan,Double>>(altersLevel1.size());
		for (SocialVertex alter : altersLevel1) {
			Plan plan = alter.getPerson().getPerson().getSelectedPlan();
			plans.add(plan);
			alter1Scores.add(new Tuple<Plan, Double>(plan, plan.getScore()));
		}

		List<Tuple<Plan, Double>> alter2Scores = new ArrayList<Tuple<Plan,Double>>(altersLevel2.size());
		for (SocialVertex alter : altersLevel2) {
			Plan plan = alter.getPerson().getPerson().getSelectedPlan();
			plans.add(plan);
			alter2Scores.add(new Tuple<Plan, Double>(plan, plan.getScore()));
		}
		Profiler.pause("step preprocessing");
		/*
		 * run physical layer
		 */
		Profiler.resume("physical engine");
		trajectoryBuilder.reset(0);
		physicalEngine.run(plans, eventsManager);
		Profiler.pause("physical engine");
		/*
		 * evaluate plans
		 */
		Profiler.resume("evaluation & postprocessing");
		evalEngine.evaluate(trajectoryBuilder.trajectories());
		/*
		 * accept/reject state
		 */
		boolean accept = mentalEngine.acceptRejectState(egos);
		/*
		 * reset scores of level 2 alters
		 */
		for(int i = 0; i < alter2Scores.size(); i++) {
			Tuple<Plan, Double> tuple = alter2Scores.get(i);
			tuple.getFirst().setScore(tuple.getSecond());
		}
		/*
		 * if state rejected, reset scores of level 1 alters
		 */
		if(!accept) {
			for(int i = 0; i < alter1Scores.size(); i++) {
				Tuple<Plan, Double> tuple = alter1Scores.get(i);
				tuple.getFirst().setScore(tuple.getSecond());
			}
		}
		
		Profiler.pause("evaluation & postprocessing");
	}

	public void drawSample(int iter) {
		Profiler.stop("mental engine", true);
		Profiler.stop("step preprocessing", true);
		Profiler.stop("physical engine", true);
		Profiler.stop("evaluation & postprocessing", true);
		
		LoggerUtils.setVerbose(false);

		List<Plan> plans = new ArrayList<Plan>(graph.getVertices().size());
		for (SocialVertex v : graph.getVertices()) {
			plans.add(v.getPerson().getPerson().getSelectedPlan());
		}

		trajectoryBuilder.reset(iter);
		physicalEngine.run(plans, eventsManager);

		Set<Trajectory> trajectories = trajectoryBuilder.trajectories();//new HashSet<Trajectory>(builder.getTrajectories().values());

		evalEngine.evaluate(trajectories);

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
