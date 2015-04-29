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
import java.util.TreeMap;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public class ParallelEventHandlingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "parallelEventHandling";

	private final static String NUMBER_OF_THREADS = "numberOfThreads";
	private Integer numberOfThreads = null;

	private final static String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
	private Long estimatedNumberOfEvents = null;

	private final static String SYNCHRONIZE_ON_SIMSTEPS = "synchronizeOnSimSteps"; 
	private Boolean synchronizeOnSimSteps = true;
	
	private boolean locked = false;

	public ParallelEventHandlingConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(NUMBER_OF_THREADS, "Number of threads for parallel events handler. 0 or null means the framework decides by itself.");
		comments.put(ESTIMATED_NUMBER_OF_EVENTS, "Estimated number of events during mobsim run. An optional optimization hint for the framework.");
		return comments;
	}

	@StringGetter( NUMBER_OF_THREADS )
	public Integer getNumberOfThreads() {
		return numberOfThreads;
	}

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

	public void makeLocked() {
		this.locked = true;
	}

}
