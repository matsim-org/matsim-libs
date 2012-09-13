package org.matsim.core.api.experimental.events;

import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.events.handler.EventHandler;

public interface EventsManager extends MatsimToplevelContainer {

	@Override
	public EventsFactory getFactory();

	public void processEvent(final Event event);

	public void addHandler(final EventHandler handler);
	
	public void removeHandler(final EventHandler handler);
	
	public void resetCounter();

	public void resetHandlers(int iteration);
	
	public void clearHandlers();
	
	public void printEventsCount();
	
	public void printEventHandlers();
	
	/**
	 * Called before the first event is sent for processing. Allows to initialize internal
	 * data structures used to process events.
	 */
	public void initProcessing();

	/**
	 * Called by a {@link Steppable} Mobsim after each {@link doSimStep} call. Parallel implementations
	 * of an EventsManager can then ensure that all events of the sim step have been processed.
	 */
	public void afterSimStep(double time);
	
	/**
	 * Called after the last event is sent for processing. The method must only return when all
	 * events are completely processing (in case they are not directly processed in
	 * {@link #processEvent(Event)}). Can be used to clean up internal data structures used
	 * to process events.
	 */
	public void finishProcessing();

}