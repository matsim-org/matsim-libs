package org.matsim.core.api.experimental.events;


import com.google.inject.Provider;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.EventArray;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Steppable;

import java.util.List;


/**
 * This should be split into two interfaces:
 * an API (add/removeHandler) and an SPI (Service Provider Interface)
 */
public interface EventsManager  {

	public void processEvent(final Event event);

	/**
	 * Submit multiple events for processing at once.
	 */
	default void processEvents(final EventArray events) {
		for (int i = 0; i < events.size(); i++) {
			processEvent(events.get(i));
		}
	}

	public void addHandler(final EventHandler handler);

	/**
	 * Add handler to the event manager via its provider. This method is necesarry for distributed event handlers, which will have multiple instances.
	 *
	 * @return list of handlers that have been created.
	 */
	default <T extends EventHandler> List<T> addHandler(final Provider<T> provider) {
		T handler = provider.get();
		addHandler(handler);
		return List.of(handler);
	}

	public void removeHandler(final EventHandler handler);

	public void resetHandlers(int iteration);

	/**
	 * Called before the first event is sent for processing. Allows to initialize internal
	 * data structures used to process events.
	 */
	public void initProcessing();

	/**
	 * Called before the next sim steps starts.
	 */
	default public void beforeSimStep(double time) {}

	/**
	 * Called by a {@link Steppable} Mobsim after each {@link Steppable#doSimStep(double)} call. Parallel implementations
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
