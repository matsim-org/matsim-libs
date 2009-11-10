/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilPopulations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.phd;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class GenerateEquilPopulations {

	public static final int NUM_AGENTS = 4000;
	
	public GenerateEquilPopulations() {
		// TODO Auto-generated constructor stub
	}

	protected void generateRandomCarOnly(ScenarioImpl scenario) {
		
		Population pop = scenario.getPopulation();
		PopulationFactory popFactory = pop.getFactory();

		NetworkLayer network = scenario.getNetwork();
		
		Activity act = null;
		Leg leg = null;
		for (int ii=0; ii < NUM_AGENTS; ii++) {
			
			Person person = popFactory.createPerson(new IdImpl(ii));
			pop.addPerson(person);
			
			Plan plan = popFactory.createPlan();
			person.addPlan(plan);
			plan.setSelected(true);
			
			act = popFactory.createActivityFromLinkId("h", network.getLink(new IdImpl(1)).getId());
			plan.addActivity(act);
			leg = popFactory.createLeg(TransportMode.undefined);
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("w", network.getLink(new IdImpl(20)).getId());
			plan.addActivity(act);
			leg = popFactory.createLeg(TransportMode.undefined);
			plan.addLeg(leg);
			act = popFactory.createActivityFromLinkId("h", network.getLink(new IdImpl(1)).getId());
			plan.addActivity(act);
		}
		
		// perform random initial demand generation wrt modes and times with planomat
		Config config = scenario.getConfig();
		// - set the population size to 1, so there is no sample of the initial random solutions the best individual would be chosen of
		config.planomat().setPopSize(1);
		// - set the number of generations to 0 (so only the random initialization, and no optimization takes place)
		config.planomat().setJgapMaxGenerations(0);
		// - set possible modes such that only "car" mode is generated
		config.planomat().setPossibleModes("car");
		
		EventsManagerImpl emptyEvents = new EventsManagerImpl();
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, config.charyparNagelScoring());
		
		Controler dummyControler = new Controler(scenario);
		dummyControler.setLeastCostPathCalculatorFactory(new DijkstraFactory());
		
		PlanomatModule testee = new PlanomatModule(
				dummyControler, 
				emptyEvents, 
				(NetworkLayer) scenario.getNetwork(), 
				scoringFunctionFactory, 
				travelCostEstimator, 
				tTravelEstimator);
		
		testee.prepareReplanning();
		for (PersonImpl person : scenario.getPopulation().getPersons().values()) {

			PlanImpl plan = person.getPlans().get(0);
			testee.handlePlan(plan);
			
		}
		testee.finishReplanning();

	}
	
}
