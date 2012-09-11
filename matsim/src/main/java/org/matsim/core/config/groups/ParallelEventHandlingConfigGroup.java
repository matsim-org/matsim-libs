/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;

/**
 * @author nagel
 *
 */
public class ParallelEventHandlingConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "parallelEventHandling";

	final String NUMBER_OF_THREADS = "numberOfThreads";
	private Integer numberOfThreads = null ;

	final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
	private Long estimatedNumberOfEvents = null ;

	private boolean locked = false ;

	public ParallelEventHandlingConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if ( NUMBER_OF_THREADS.equals(key) || ESTIMATED_NUMBER_OF_EVENTS.equals(key) ) {
			throw new RuntimeException( "getValue access disabled; use direct getter ") ;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if ( NUMBER_OF_THREADS.equals(key) ) {
			this.setNumberOfThreads(Integer.parseInt(value)) ; 
		} else if ( ESTIMATED_NUMBER_OF_EVENTS.equals(key) ) {
			this.setEstimatedNumberOfEvents( Long.parseLong(value) ) ; 
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(NUMBER_OF_THREADS, "number of threads for parallel events handler.  0 or null means parallel events handler is disabled");
		comments.put(ESTIMATED_NUMBER_OF_EVENTS, "estimated number of events during mobsim run, useful for configuration");
		return comments;
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
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

	public void makeLocked() {
		this.locked = true;
	}

}
