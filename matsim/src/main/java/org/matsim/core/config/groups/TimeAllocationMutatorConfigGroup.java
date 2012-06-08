/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.core.config.Module;

public class TimeAllocationMutatorConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "TimeAllocationMutator";
	
	public static final String MUTATION_RANGE = "mutationRange" ;
	
	private Double mutationRange=1800. ;

	public TimeAllocationMutatorConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		throw new RuntimeException("getValue access disabled; use typed getter instead") ;
	}

	@Override
	public void addParam(final String key, final String value) {
		if (MUTATION_RANGE.equals(key)) {
			this.setMutationRange( Double.valueOf(value) ) ;
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MUTATION_RANGE, "Defines how many seconds a time mutation can maximally shift a time." ) ; 
		return comments;
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(MUTATION_RANGE, this.mutationRange.toString() );
		return map;
	}

	/* direct access */

	public Double getMutationRange() {
		return this.mutationRange ;
	}

	public void setMutationRange(final Double val) {
		this.mutationRange = val;
	}

}
