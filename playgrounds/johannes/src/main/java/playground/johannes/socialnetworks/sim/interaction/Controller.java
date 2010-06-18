/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.johannes.socialnetworks.graph.social.SocialGraph;

/**
 * @author illenberger
 *
 */
public class Controller {

	private Population population;
	
	private List<Person> persons;
	
	private Random random;
	
	private EndTimeMutator mutator;
	
	private PseudoSim sim;
	
	private Network network;
	
	private TravelTime travelTime;
	
	private EventsManagerImpl eventManager;
	
	private EventsToScore scorer;
	
	private double oldScore;
	
	public Controller(Population population, Network network, SocialGraph graph) {
		random = new Random();
		
		this.population = population;
		this.network = network;
	
		persons = new ArrayList<Person>(population.getPersons().values());
		
		eventManager = new EventsManagerImpl();
		VisitorTracker tracker = new VisitorTracker();
		eventManager.addHandler(tracker);
		
		scorer = new EventsToScore(population, new JointActivityScoringFunctionFactory(tracker, graph));
		eventManager.addHandler(scorer);
		
		travelTime = new TravelTimeCalculator(network, 3600, 86400, new TravelTimeCalculatorConfigGroup());
		
		sim = new PseudoSim();
		
		mutator = new EndTimeMutator();
	}
	
	public void run(int iterations) {
		sim.run(population, network, travelTime, eventManager);
		scorer.finish();
		oldScore = scorer.getAveragePlanPerformance();
		
		for(int i = 0; i < iterations; i++) {
			step();
		}
	}
	
	public void step() {
		/*
		 * randomly select one plan
		 */
		Person person = persons.get(random.nextInt(persons.size()));
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		PlanImpl copy = new PlanImpl(plan.getPerson());
		copy.copyPlan(plan);
		copy.setSelected(true);
		plan.setSelected(false);
		/*
		 * mutate
		 */
		mutator.mutatePlan(copy, "leisure");
		/*
		 * simulate
		 */
		sim.run(population, network, travelTime, eventManager);
		/*
		 * evaluate
		 */
		scorer.finish();
		double newscore = scorer.getAveragePlanPerformance();
		double delta = oldScore - newscore;
		/*
		 * select
		 */
		double p = 1/(1 + Math.exp(delta));
		if(random.nextDouble() < p) {
			/*
			 * accept
			 */
			person.getPlans().remove(plan);
			copy.setSelected(true);
			
			oldScore = newscore;
		} else {
			/*
			 * reject
			 */
			person.getPlans().remove(copy);
			plan.setSelected(true);
		}
	}
}
