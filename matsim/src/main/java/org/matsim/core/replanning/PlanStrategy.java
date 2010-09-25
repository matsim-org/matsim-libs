package org.matsim.core.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

public interface PlanStrategy {

	/**
	 * Adds a strategy module to this strategy.
	 *
	 * @param module
	 */
	public void addStrategyModule(final PlanStrategyModule module);

	/**
	 * @return the number of strategy modules added to this strategy
	 */
	public int getNumberOfStrategyModules();

	/**
	 * Adds a person to this strategy to be handled. It is not required that
	 * the person is immediately handled during this method-call (e.g. when using
	 * multi-threaded strategy-modules).  This method ensures that an unscored
	 * plan is selected if the person has such a plan ("optimistic behavior").
	 *
	 * @param person
	 * @see #finish()
	 */
	public void run(final Person person);

	/**
	 * Tells this strategy to initialize its modules. Called before a bunch of
	 * person are handed to this strategy.
	 */
	public void init();

	/**
	 * Indicates that no additional persons will be handed to this module and
	 * waits until this strategy has finished handling all persons.
	 *
	 * @see #run(PersonImpl)
	 */
	public void finish();

	/** Returns a descriptive name for this strategy, based on the class names on the used
	 * {@link PlanSelector plan selector} and {@link PlanStrategyModule strategy modules}.
	 *
	 * @return An automatically generated name for this strategy.
	 */
	public String toString();

	public PlanSelector getPlanSelector();

}