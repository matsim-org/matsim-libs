package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.core.replanning.ReplanningContext;

public interface GenericPlanStrategyModule<T extends BasicPlan> {

	/**
	 * Initializes this module before handling plans. Modules using an external
	 * routine could e.g. open a file here to write the plans out and pass them to
	 * the external routines. Multi-threaded modules could initialize and start
	 * their threads in this method.
	 * @param replanningContext TODO
	 */
	public void prepareReplanning(ReplanningContext replanningContext);
	
	/**
	 * Tells this module to handle the specified plan. It is not required that
	 * the plan must immediately be handled, e.g. modules calling external 
	 * routines could just collect the plans here and start the external routine
	 * in {@link #finishReplanning()}, or multi-threaded modules could just add the
	 * plan to a synchronized queue for the threads.
	 *
	 * @param plan
	 * @see #finishReplanning()
	 */
	public void handlePlan(T plan);
	
	/**
	 * Indicates that no additional plans will be handed to this module and waits
	 * until this module has finished handling all plans. Modules calling external
	 * routines can call those here, or multi-threaded modules can wait here until
	 * all threads are finished with their work.
	 * 
	 * @see #handlePlan(T)
	 */
	public void finishReplanning();

}
