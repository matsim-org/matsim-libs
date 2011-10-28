/* *********************************************************************** *
 * project: org.matsim.*
 * PlateformBasedModeChooser.java
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
 * The core class of the logit-based matings framework.
 * Given implementations of the components of the framework, it
 * does the following on the selected plan of agents in the population:
 *
 * <ul>
 * <li> performs a first pass on all legs, to perform a mode choice
 * <li> it passes the trips for which car-pool mode has been choosen
 * to the platform
 * <li> it groups the agents mated by the platform in cliques, and
 * creates a PopulationWithCliques ready for export.
 * </ul>
 *
 * @author thibautd
 */
public class PlatformBasedModeChooser {
	private MatingPlatform platform = null;
	private ChoiceModel model = null;
	private CliquesConstructor cliquesConstructor = null;

	private Population population = null;

	private Map<Id, List<Id>> cliques = null;

	private boolean isChanged = false;

	// /////////////////////////////////////////////////////////////////////////
	// public: getters and setters
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @return the decisionMakerFactory
	 */
	public DecisionMakerFactory getDecisionMakerFactory() {
		return model.getDecisionMakerFactory();
	}

	/**
	 * @return the choiceSetFactory
	 */
	public ChoiceSetFactory getChoiceSetFactory() {
		return model.getChoiceSetFactory();
	}

	/**
	 * @return the plateform
	 */
	public MatingPlatform getPlatform() {
		return platform;
	}

	/**
	 * @param plateform the plateform to set
	 */
	public void setPlatform(final MatingPlatform platform) {
		this.platform = platform;
		isChanged = true;
	}

	/**
	 * @return the population
	 */
	public Population getPopulation() {
		return population;
	}

	/**
	 * @param population the population to use. The plans of this instance will
	 * be modified by the clique constructor.
	 */
	public void setPopulation(final Population population) {
		this.population = population;
		isChanged = true;
	}

	/**
	 * @return the model
	 */
	public ChoiceModel getChoiceModel() {
		return model;
	}

	/**
	 * @param model the model to set
	 */
	public void setChoiceModel(final ChoiceModel model) {
		this.model = model;
		isChanged = true;
	}

	/**
	 * @param constructor the constructor to set
	 */
	public void setCliquesConstructor(final CliquesConstructor constructor) {
		this.cliquesConstructor = constructor;
		isChanged = true;
	}


	// /////////////////////////////////////////////////////////////////////////
	// public: process data methods
	// /////////////////////////////////////////////////////////////////////////
	public void process() {
		if (!isChanged) return;
		checkProcessingState();

		for (Person person : population.getPersons().values()) {
			DecisionMaker decisionMaker;
			try {
				decisionMaker = getDecisionMakerFactory().createDecisionMaker(person);
			} catch (DecisionMakerFactory.UnelectableAgentException e) {
				// this agent cannot be handled: jump to the next one
				continue;
			}

			// TODO: check if other plans
			Plan plan = person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();

			int count = 0;
			for (PlanElement element : planElements) {
				if (element instanceof Leg) {
					List<Alternative> choiceSet = 
						getChoiceSetFactory().createChoiceSet(
								decisionMaker,
								plan,
								count);
					Alternative choice = model.performChoice(decisionMaker, choiceSet);

					if (choice instanceof TripRequest) {
						platform.handleRequest((TripRequest) choice);	
					}
					else {
						((Leg) element).setMode(choice.getMode());
					}
				}

				count++;
			}
		}

		List<Mating> matings = platform.getMatings();
		cliques = cliquesConstructor.processMatings(population, matings);

		isChanged = false;
	}

	public Map<Id, List<Id>> getCliques() {
		if (isChanged) {
			process();
		}
		return cliques;
	}

	// /////////////////////////////////////////////////////////////////////////
	// internal
	// /////////////////////////////////////////////////////////////////////////
	private void checkProcessingState() throws IllegalStateException {
		if ( (platform == null) ||	
				(model == null) ||
				(cliquesConstructor == null) ||
				(population == null) ) {
			throw new IllegalStateException("Calling processing methods before all components are set.");
		}
	}
}

