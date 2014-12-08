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

import org.matsim.core.config.ConfigGroup;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author nagel
 *
 */
public class ParallelEventHandlingConfigGroup extends ConfigGroup {

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
	public String getValue(final String key) {
		if ( NUMBER_OF_THREADS.equals(key) || ESTIMATED_NUMBER_OF_EVENTS.equals(key) ) {
			throw new RuntimeException("getValue access disabled; use direct getter");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
        switch (key) {
            case NUMBER_OF_THREADS:
                this.setNumberOfThreads(Integer.parseInt(value));
                break;
            case ESTIMATED_NUMBER_OF_EVENTS:
                this.setEstimatedNumberOfEvents(Long.parseLong(value));
                break;
            case SYNCHRONIZE_ON_SIMSTEPS:
                this.setSynchronizeOnSimSteps(Boolean.parseBoolean(value));
                break;
            default:
                throw new IllegalArgumentException(key);
        }
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(NUMBER_OF_THREADS, "Number of threads for parallel events handler. 0 or null means the framework decides by itself.");
		comments.put(ESTIMATED_NUMBER_OF_EVENTS, "Estimated number of events during mobsim run. An optional optimization hint for the framework.");
		return comments;
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(NUMBER_OF_THREADS, this.getNumberOfThreads() == null ? null : this.getNumberOfThreads().toString());
		map.put(ESTIMATED_NUMBER_OF_EVENTS, this.getEstimatedNumberOfEvents() == null ? null : this.getEstimatedNumberOfEvents().toString());
		return map;
	}

	/* direct access */
	public Integer getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(Integer numberOfThreads) {
		if ( !this.locked ) {
			this.numberOfThreads = numberOfThreads;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}

	public Long getEstimatedNumberOfEvents() {
		return estimatedNumberOfEvents;
	}

	public void setEstimatedNumberOfEvents(Long estimatedNumberOfEvents) {
		if ( !this.locked ) {
			this.estimatedNumberOfEvents = estimatedNumberOfEvents;
		} else {
			throw new RuntimeException("it is too late in the control flow to modify this parameter");
		}
	}
	
	public Boolean getSynchronizeOnSimSteps() {
		return this.synchronizeOnSimSteps;
	}

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
