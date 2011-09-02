/* *********************************************************************** *
 * project: org.matsim.*
 * EvalEngine.java
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
package playground.johannes.coopsim.eval;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.EventsToScore;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;


/**
 * @author illenberger
 * 
 */
public class EvalEngine {
	
	private final EventsToScore scorer;

	private final JointActivityScoringFactory factory;

	private final VisitorTracker tracker;

	public EvalEngine(SocialGraph graph, PlanCalcScoreConfigGroup config, double beta) {
		tracker = new VisitorTracker();
		factory = new JointActivityScoringFactory(graph, tracker, config, beta);
		
		Population pop = new PopulationImpl(null);
		for(SocialVertex v : graph.getVertices())
			pop.addPerson(v.getPerson().getPerson());
		
		scorer = new EventsToScore(pop, factory);
	}
	
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handlers = new ArrayList<EventHandler>(2);
		handlers.add(scorer);
		handlers.add(tracker);
		return handlers;
	}

	public void init() {
		scorer.reset(0);
		tracker.reset(0);
	}

	public void run() {
		scorer.finish();
	}
}
