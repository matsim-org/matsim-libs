/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelEventHandlingConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public final class EventsManagerConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "eventsManager";

	private final static String NUMBER_OF_THREADS = "numberOfThreads";
	private Integer numberOfThreads = null;
	public final static String NUMBER_OF_THREADS_COMMENT = "Number of threads for parallel events handler. _null_ means the framework decides by itself. 0 is currently not possible.";

	private final static String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
	private Long estimatedNumberOfEvents = null;

	private final static String SYNCHRONIZE_ON_SIMSTEPS = "synchronizeOnSimSteps";
	private Boolean synchronizeOnSimSteps = true;

	private final static String ONE_THREAD_PER_HANDLER = "oneThreadPerHandler";
	private Boolean oneThreadPerHandler = false;

	private final static String EVENTS_QUEUE_SIZE = "eventsQueueSize";
	private final static String EVENTS_QUEUE_SIZE_COMMENT = "Size of the events Queue. Increase for very large scenarios";
	private int eventsQueueSize = 65536 * 2 ;



	private boolean locked = false;

	public EventsManagerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(NUMBER_OF_THREADS, NUMBER_OF_THREADS_COMMENT);
		comments.put(ESTIMATED_NUMBER_OF_EVENTS, "Estimated number of events during mobsim run. An optional optimization hint for the framework.");
		comments.put(SYNCHRONIZE_ON_SIMSTEPS, "If enabled, it is ensured that all events that are created during a time step of the mobility simulation are processed "
				+ "before the next time step is simulated. E.g. neccessary when within-day replanning is used.");
		comments.put(ONE_THREAD_PER_HANDLER, "If enabled, each event handler is assigned to its own thread. Note that enabling this feature disabled the " + NUMBER_OF_THREADS + " option! "
				+ "This feature is still experimental!");
		comments.put(EVENTS_QUEUE_SIZE,EVENTS_QUEUE_SIZE_COMMENT);
		return comments;
	}

	/**
	 * {@value #NUMBER_OF_THREADS_COMMENT}
	 */
	@StringGetter( NUMBER_OF_THREADS )
	public Integer getNumberOfThreads() {
		return numberOfThreads;
	}

	/**
	 * {@value #NUMBER_OF_THREADS_COMMENT}
	 *
	 * @param numberOfThreads
	 */
	@StringSetter( NUMBER_OF_THREADS )
	public void setNumberOfThreads(Integer numberOfThreads) {
		if ( !this.locked ) {
			this.numberOfThreads = numberOfThreads;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}

	@StringGetter( ESTIMATED_NUMBER_OF_EVENTS )
	public Long getEstimatedNumberOfEvents() {
		return estimatedNumberOfEvents;
	}

	@StringSetter( ESTIMATED_NUMBER_OF_EVENTS )
	public void setEstimatedNumberOfEvents(Long estimatedNumberOfEvents) {
		if ( !this.locked ) {
			this.estimatedNumberOfEvents = estimatedNumberOfEvents;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}

	@StringSetter(EVENTS_QUEUE_SIZE)
	public void setEventsQueueSize(int eventsQueueSize) {
		this.eventsQueueSize = eventsQueueSize;
	}

	@StringGetter(EVENTS_QUEUE_SIZE)
	public int getEventsQueueSize() {
		return eventsQueueSize;
	}

	@StringGetter( SYNCHRONIZE_ON_SIMSTEPS )
	public Boolean getSynchronizeOnSimSteps() {
		return this.synchronizeOnSimSteps;
	}

	@StringSetter( SYNCHRONIZE_ON_SIMSTEPS )
	public void setSynchronizeOnSimSteps(Boolean synchronizeOnSimSteps) {
		if ( !this.locked ) {
			this.synchronizeOnSimSteps = synchronizeOnSimSteps;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}

	@StringGetter( ONE_THREAD_PER_HANDLER )
	public Boolean getOneThreadPerHandler() {
		return this.oneThreadPerHandler;
	}

	@StringSetter( ONE_THREAD_PER_HANDLER )
	public void setOneThreadPerHandler(Boolean oneThreadPerHandler) {
		if ( !this.locked ) {
			this.oneThreadPerHandler = oneThreadPerHandler;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}

	public void makeLocked() {
		this.locked = true;
	}
}
