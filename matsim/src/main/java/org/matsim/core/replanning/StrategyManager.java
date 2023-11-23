/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Manages and applies strategies to agents for re-planning.
 *
 * @author mrieser
 * @author kai
 */
@Singleton
public class StrategyManager implements MatsimManager {
	// I understand the way in which this is integrated into the framwork as follows:
	// * The PlansReplanning interface is the one that is bound as ReplanningListener, and it calls StrategyManager.
	// * On the other hand, StrategyManager is also independently bound.
	// * This has the consequence that users (possibly via the config) may configure StrategyManager, but it is effectively ignored if the bound
	// implementation of ReplanningListener does not use it.
	// * This is the design that I found.  Not sure if I should change it, but I need something for Carrier and later for LSP.
	// * A possible way out would be to unbind StrategyManager (for someone who implements a ReplanningListener that does not use
	// StrategyManager).  This is not immediately possible, but the internet gives possibilities.  Could be added as a utility method, which would
	// (I think) make sense together with the MATSim overall design which overrides defaults instead of asking the user to plug everything together.

	// Not sure if that would work: There is other code that tries to obtain StrategyManager via injection, and all of this would then fail.
	// Not sure if one could unbind all of these.

	// kai, jun'22

	private static final Logger log = LogManager.getLogger(StrategyManager.class);

	private final GenericStrategyManagerImpl<Plan, Person> delegate;

	@Inject
	StrategyManager(ReplanningConfigGroup replanningConfigGroup,
									ControllerConfigGroup controllerConfigGroup, StrategyChooser<Plan, Person> strategyChooser,
									Map<ReplanningConfigGroup.StrategySettings, PlanStrategy> planStrategies ) {

		this(strategyChooser);
		setMaxPlansPerAgent(replanningConfigGroup.getMaxAgentPlanMemorySize());

		int globalInnovationDisableAfter = (int) ((controllerConfigGroup.getLastIteration() - controllerConfigGroup.getFirstIteration())
				* replanningConfigGroup.getFractionOfIterationsToDisableInnovation() + controllerConfigGroup.getFirstIteration());
		log.info("global innovation switch off after iteration: " + globalInnovationDisableAfter);

		for (Map.Entry<ReplanningConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
			PlanStrategy strategy = entry.getValue();
			ReplanningConfigGroup.StrategySettings settings = entry.getKey();
			addStrategy(strategy, settings.getSubpopulation(), settings.getWeight());

			// now check if this modules should be disabled after some iterations
			int maxIter = settings.getDisableAfter();
			if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
				if (!ReplanningUtils.isOnlySelector(strategy)) {
					maxIter = globalInnovationDisableAfter ;
				}
			}

			if (maxIter >= 0) {
				if (maxIter >= controllerConfigGroup.getFirstIteration()) {
					addChangeRequest(maxIter + 1, strategy, settings.getSubpopulation(), 0.0);
				} else {
					/* The services starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					changeWeightOfStrategy(strategy, settings.getSubpopulation(), 0.0);
				}
			}
		}
	}

	public StrategyManager() {
		this.delegate = new GenericStrategyManagerImpl<>();
	}

	StrategyManager(StrategyChooser<Plan, Person> strategyChooser) {
		this.delegate = new GenericStrategyManagerImpl<>(strategyChooser);
	}

	/**
	 * Adds a strategy to this manager with the specified weight. This weight
	 * compared to the sum of weights of all strategies in this manager defines
	 * the probability this strategy will be used for an agent.
	 *
	 */
	public final void addStrategy( final PlanStrategy strategy, final String subpopulation, final double weight) {
		delegate.addStrategy(strategy, subpopulation, weight);
	}

	/**
	 * removes the specified strategy from this manager for the specified subpopulation
	 *
	 * @param strategy the strategy to be removed
	 * @param subpopulation the subpopulation for which the strategy must be removed
	 * @return true if the strategy was successfully removed from this manager,
	 * 		false if the strategy was not part of this manager and could thus not be removed.
	 */
	public final boolean removeStrategy( final PlanStrategy strategy, final String subpopulation) {
		return delegate.removeStrategy(strategy, subpopulation) ;
	}

	/**
	 * changes the weight of the specified strategy
	 *
	 * @return true if the strategy is part of this manager and the weight could
	 * 		be changed successfully, false otherwise.
	 */
	public final boolean changeWeightOfStrategy( final GenericPlanStrategy<Plan, Person> strategy, final String subpopulation, final double newWeight) {
		return delegate.changeWeightOfStrategy(strategy, subpopulation, newWeight) ;
	}

	/**
	 * Randomly chooses for each person of the population a strategy and uses that
	 * strategy on the person, after adapting the strategies to any pending change
	 * requests for the specified iteration.
	 *
	 * @param iteration the current iteration we're handling
	 */
	public final void run(final Population population, final int iteration, final ReplanningContext replanningContext) {
		delegate.run( population.getPersons().values(), iteration, replanningContext );
	}

	/**
	 * chooses a (weight-influenced) random strategy
	 *
	 * @param person The person for which the strategy should be chosen
	 * @return the chosen strategy
	 */
	public GenericPlanStrategy<Plan, Person> chooseStrategy(final Person person, final String subpopulation, ReplanningContext replanningContext) {
		return delegate.chooseStrategy(person, subpopulation, replanningContext );
	}

	/**
	 * Sets the maximal number of plans an agent can memorize. Setting
	 * maxPlansPerAgent to zero means unlimited memory (only limited by RAM).
	 * Agents can have up to maxPlansPerAgent plans plus one additional one with the
	 * currently modified plan they're trying out.
	 */
	public final void setMaxPlansPerAgent(final int maxPlansPerAgent) {
		delegate.setMaxPlansPerAgent(maxPlansPerAgent);
	}

	public final int getMaxPlansPerAgent() {
		return delegate.getMaxPlansPerAgent();
	}

	/**
	 * Schedules a command for a later iteration. The
	 * change will take place before the strategies are applied.
	 *
	 */
	public final void addChangeRequest(
			final int iteration,
			final PlanStrategy strategy,
			final String subpopulation,
			final double newWeight) {
		delegate.addChangeRequest(iteration, strategy, subpopulation, newWeight);
	}

	/**
	 * Sets a plan selector to be used for choosing plans for removal, if they
	 * have more plans than the specified maximum. This defaults to
	 * {@link WorstPlanForRemovalSelector}.
	 * <p></p>
	 * Thoughts about using the logit-type selectors with negative logit model scale parameter:<ul>
	 * <li> Look at one agent.
	 * <li> Assume she has the choice between <i>n</i> different plans.
	 * <li> (Continuous) fraction <i>f(i)</i> of plan <i>i</i> develops as (master equation)
	 * <blockquote><i>
	 * df(i)/dt = - p(i) * f(i) + 1/n
	 * </i></blockquote>
	 * where <i>p(i)</i> is from the choice model.
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
	@Inject
	public final void setPlanSelectorForRemoval(final PlanSelector<Plan, Person> planSelector) {
		delegate.setPlanSelectorForRemoval(planSelector);
	}

	public final List<GenericPlanStrategy<Plan, Person>> getStrategies(final String subpopulation) {
		return delegate.getStrategies(subpopulation) ;
	}

	/**
	 * @return the weights of the strategies for the given subpopulation, in the same order as the strategies returned by {@link #getStrategies(java.lang.String)}
	 */
	public final List<Double> getWeights(final String subpopulation) {
		return delegate.getWeights(subpopulation);
	}
}
