/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.GenericWorstPlanForRemovalSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Notes:<ul>
 * <li> This version is now a bit more restrictive than the original strategy manager in terms of public methods and in terms of final methods.
 * Given subpopulations and pluggable worst plans remover, most situations where inheritance was needed should be gone.
 * Please engage in a discussion with the core team if you think this class should be more liberal. kai, nov'13
 * </ul>
 *
 * @author nagel (for the generic version)
 * @author rieser (for the original StrategyManager)
 *
 */
public class GenericStrategyManager<T extends BasicPlan, I extends HasPlansAndId<? extends BasicPlan, I>> implements MatsimManager {
	// the "I extends ... <, I>" is correct, although it feels odd.  kai, nov'15
	
	private static final Logger log =
			Logger.getLogger(GenericStrategyManager.class);


	static class StrategyWeights<T extends BasicPlan, I> {
		final List<GenericPlanStrategy<T, I>> strategies = new ArrayList<>();
		final List<GenericPlanStrategy<T, I>> unmodifiableStrategies = Collections.unmodifiableList( strategies );
		final List<Double> weights = new ArrayList<>();
		final List<Double> unmodifiableWeights = Collections.unmodifiableList(weights);
		double totalWeights = 0.0;
		final Map<Integer, Map<GenericPlanStrategy<T, I>, Double>> changeRequests = new TreeMap<>();
	}

	private final Map<String, StrategyWeights<T, I>> weightsPerSubpopulation = new HashMap<>();

	private int maxPlansPerAgent = 0;

	private PlanSelector<T, I> removalPlanSelector = new GenericWorstPlanForRemovalSelector<>();

	private String subpopulationAttributeName = null;

	/**
	 * @param name the name of the subpopulation attribute
	 * in the person's object attributes.
	 */
	public final void setSubpopulationAttributeName(final String name) {
		this.subpopulationAttributeName = name;
	}

	/**
	 * Adds a strategy to this manager with the specified weight. This weight
	 * compared to the sum of weights of all strategies in this manager defines
	 * the probability this strategy will be used for an agent.
	 *
	 */
	public final void addStrategy(
			final GenericPlanStrategy<T, I> strategy,
			final String subpopulation,
			final double weight) {
		final StrategyWeights<T, I> weights = getStrategyWeights( subpopulation );
		if ( weights.strategies.contains( strategy ) ) {
			log.error( "Strategy "+strategy+" is already defined for subpopulation "+subpopulation  );
			log.error( "This can lead to undefined behavior. Please only specify each strategy once" );
			throw new IllegalStateException( "Strategy "+strategy+" is already defined for subpopulation "+subpopulation  );
		}
		weights.strategies.add(strategy);
		weights.weights.add(weight);
		weights.totalWeights += weight;
	}

	/**
	 * removes the specified strategy from this manager for the specified subpopulation
	 *
	 * @param strategy the strategy to be removed
	 * @param subpopulation the subpopulation for which the strategy must be removed
	 * @return true if the strategy was successfully removed from this manager,
	 * 		false if the strategy was not part of this manager and could thus not be removed.
	 */
	final boolean removeStrategy(
			final GenericPlanStrategy<T, I> strategy,
			final String subpopulation)
	{
		final StrategyWeights<T, I> weights = getStrategyWeights( subpopulation );
		int idx = weights.strategies.indexOf(strategy);
		if (idx != -1) {
			weights.strategies.remove(idx);
			double weight = weights.weights.remove(idx);
			weights.totalWeights -= weight;
			return true;
		}
		return false;
	}



	private StrategyWeights<T, I> getStrategyWeights(final String subpop) {
		StrategyWeights<T, I> weights = weightsPerSubpopulation.get(subpop);

		if ( weights == null ) {
			weights = new StrategyWeights<>();
			weightsPerSubpopulation.put(subpop, weights);
		}

		return weights;
	}

	/**
	 * changes the weight of the specified strategy
	 *
	 * @return true if the strategy is part of this manager and the weight could
	 * 		be changed successfully, false otherwise.
	 */
	final boolean changeWeightOfStrategy(
			final GenericPlanStrategy<T, I> strategy,
			final String subpopulation,
			final double newWeight) {
		final StrategyWeights<T, I> weights = getStrategyWeights(subpopulation);
		int idx = weights.strategies.indexOf(strategy);
		if (idx != -1) {
			double oldWeight = weights.weights.set(idx, newWeight);
			weights.totalWeights += (newWeight - oldWeight);
			Logger.getLogger(this.getClass()).info( strategy.toString() + ": oldWeight=" + oldWeight + " newWeight=" + newWeight );
			return true;
		}
		return false;
	}

	/**
	 * Randomly chooses for each person of the population a strategy and uses that
	 * strategy on the person, after adapting the strategies to any pending change
	 * requests for the specified iteration.
	 *
	 * @param iteration the current iteration we're handling
	 */
	public final void run(
			final Iterable<? extends HasPlansAndId<T, I>> persons,
					ObjectAttributes subpopLookup,
					final int iteration,
					final ReplanningContext replanningContext ) {
		handleChangeRequests(iteration);
		run(persons, subpopLookup, replanningContext);
	}

	/**
	 * Randomly chooses for each person of the population a strategy and uses that
	 * strategy on the person.
	 *
	 */
	final void run(
			final Iterable<? extends HasPlansAndId<T, I>> persons,
					ObjectAttributes subPopLookup,
					final ReplanningContext replanningContext) {

		// initialize all strategies
		for (GenericPlanStrategy<T, I> strategy : distinctStrategies()) {
			strategy.init(replanningContext);
		}

		// then go through the population and ...
		for (HasPlansAndId<T, I> person : persons ) {

			// ... reduce the number of plans to the allowed maximum (in evol comp lang this is "selection")
			if ((this.maxPlansPerAgent > 0) && (person.getPlans().size() > this.maxPlansPerAgent)) {
				removePlans( person, this.maxPlansPerAgent);
			}

			// ... choose the strategy to be used for this person (in evol comp lang this would be the choice of the mutation operator)
			String subpopName = null;
			if (this.subpopulationAttributeName != null) {
				subpopName = (String) subPopLookup.getAttribute(person.getId().toString(), this.subpopulationAttributeName);
			}
			GenericPlanStrategy<T, I> strategy = this.chooseStrategy(person, subpopName);

			if (strategy==null) {
				throw new RuntimeException("No strategy found! Have you defined at least one replanning strategy per subpopulation?");
			}
			
			// ... and run the strategy:
			strategy.run(person);
		}

		// finally make sure all strategies have finished there work
		for (GenericPlanStrategy<T, I> strategy : distinctStrategies()) {
			strategy.finish();
		}

	}

	private Collection<GenericPlanStrategy<T, I>> distinctStrategies() {
		// Leaving out duplicate strategies in different subpopulations
		Collection<GenericPlanStrategy<T, I>> strategies = new LinkedHashSet<>();
		for (StrategyWeights<T, I> weights : weightsPerSubpopulation.values()) {
			strategies.addAll(weights.strategies);
		}
		return strategies;
	}

	private void removePlans(final HasPlansAndId<T, I> person, final int maxNumberOfPlans) {
		while (person.getPlans().size() > maxNumberOfPlans) {
			T plan = this.removalPlanSelector.selectPlan(person);
			person.removePlan(plan);
			if (plan == person.getSelectedPlan()) {
				final T newPlanToSelect = new RandomPlanSelector<T, I>().selectPlan(person) ;
				if ( newPlanToSelect == null ) {
					throw new IllegalStateException( "could not find a plan to select for person "+person );
				}
				person.setSelectedPlan( newPlanToSelect );
			}
		}
	}

	/**
	 * modifies the loaded strategies according to pending change requests for this iteration.
	 *
	 */
	final void handleChangeRequests(final int iteration) {
		for ( int ii = 0 ; ii <= iteration ; ii++ ) {
			// (playing back history for those installations which recreate the strategy manager in every iteration)
			for ( Map.Entry<String, StrategyWeights<T, I>> wentry : weightsPerSubpopulation.entrySet() ) {
				final String subpop = wentry.getKey();
				final StrategyWeights<T, I> weights = wentry.getValue();
				Map<GenericPlanStrategy<T, I>, Double> changes = weights.changeRequests.remove(ii);
				if (changes != null) {
					for (Map.Entry<GenericPlanStrategy<T, I>, Double> entry : changes.entrySet()) {
						changeWeightOfStrategy( entry.getKey(), subpop, entry.getValue());
					}
				}
			}
		}
	}

	/**
	 * chooses a (weight-influenced) random strategy
	 *
	 * @return the chosen strategy
	 */
	/* deliberately package */ GenericPlanStrategy<T, I> chooseStrategy(HasPlansAndId<T, I> person, final String subpopulation) {
		// yyyyyy I can see that this would need to be replaceable, but need to find some other way than inheritance.  kai, mar'18
		
		final StrategyWeights<T, I> weights = getStrategyWeights(subpopulation);

		double rnd = MatsimRandom.getRandom().nextDouble() * weights.totalWeights;

		double sum = 0.0;
		for (int i = 0, max = weights.weights.size(); i < max; i++) {
			sum += weights.weights.get(i);
			if (rnd <= sum) {
				return weights.strategies.get(i);
			}
		}
		return null;
	}

	/**
	 * Sets the maximal number of plans an agent can memorize. Setting
	 * maxPlansPerAgent to zero means unlimited memory (only limited by RAM).
	 * Agents can have up to maxPlansPerAgent plans plus one additional one with the
	 * currently modified plan they're trying out.
	 *
	 */
	public final void setMaxPlansPerAgent(final int maxPlansPerAgent) {
		this.maxPlansPerAgent = maxPlansPerAgent;
	}

	/**
	 * Schedules a {@link #changeWeightOfStrategy(GenericPlanStrategy, String, double)} command for a later iteration. The
	 * change will take place before the strategies are applied.
	 *
	 */
	public final void addChangeRequest(
			final int iteration,
			final GenericPlanStrategy<T, I> strategy,
			final String subpopulation,
			final double newWeight) {
		final StrategyWeights<T, I> weights = getStrategyWeights( subpopulation );
		Integer iter = iteration;
		Map<GenericPlanStrategy<T, I>, Double> iterationRequests = weights.changeRequests.get(iter);
		if (iterationRequests == null) {
			iterationRequests = new HashMap<>(3);
			weights.changeRequests.put(iter, iterationRequests);
		}
		iterationRequests.put(strategy, newWeight);
		Logger.getLogger(this.getClass()).info( "added change request: "
				+ " iteration=" + iter + " newWeight=" + newWeight + " strategy=" + strategy.toString() );
	}

	/**
	 * Sets a plan selector to be used for choosing plans for removal, if they
	 * have more plans than the specified maximum. This defaults to
	 * {@link WorstPlanForRemovalSelector}.
	 * <p></p>
	 * Thoughts about using the logit-type selectors with negative logit model scale parameter:<ul>
	 * <li> Look at one agent.
	 * <li> Assume she has the choice between <i>n</i> different plans (although fewer of them are in the MATSim choice set).
	 * <li> (Continuous) fraction <i>f(i)</i> of plan <i>i</i> develops as (master equation)
	 * <blockquote><i>
	 * df(i)/dt = - p(i) * f(i) + 1/n
	 * </i></blockquote>
	 * where <i>p(i)</i> is from the removal selector, and <i>1/n</i> makes the assumption that each possible plan is (re-)inserted with equal probability
	 * by the innovative modules.
	 * <li> Steady state solution (<i>df/dt=0</i>) <i> f(i) = 1/n * 1/p(i) </i>.
	 * <li> If <i> p(i) = e<sup>-b*U(i)</sup></i>, then <i> f(i) = e<sup>b*U(i)</sup> / n </i>.  Or in words:
	 * <i><b> If you use a logit model with a minus in front of the beta for plans removal, the resulting steady state distribution is
	 * the same logit model with normal beta.</b></i>
	 *
	 * </ul>
	 * The implication seems to be: divide the user-configured beta by two, use one half for choice and the other for plans removal.
	 * <p></p>
	 * The path size version still needs to be tested (both for choice and for plans removal).
	 *
	 *
	 * @see #setMaxPlansPerAgent(int)
	 */
	public final void setPlanSelectorForRemoval(final PlanSelector<T, I> planSelector) {
		Logger.getLogger(this.getClass()).info("setting PlanSelectorForRemoval to " + planSelector.getClass() ) ;
		this.removalPlanSelector = planSelector;
	}

	final int getMaxPlansPerAgent() {
		return this.maxPlansPerAgent ;
	}

	public final List<GenericPlanStrategy<T, I>> getStrategies(String subpopulation) {
		return getStrategyWeights( subpopulation ).unmodifiableStrategies;
	}

	public final List<Double> getWeights(String subpopulation) {
		return getStrategyWeights( subpopulation ).unmodifiableWeights;
	}

}
