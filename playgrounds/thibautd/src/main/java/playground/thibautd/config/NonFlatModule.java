/* *********************************************************************** *
 * project: org.matsim.*
 * NonFlatModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.Module;

import playground.ivt.utils.MapUtils;

/**
 * @author thibautd
 */
public class NonFlatModule extends Module {
	private final Map<String, Collection<Module>> parameterSetsPerType = new HashMap<String, Collection<Module>>();

	public NonFlatModule(final String name) {
		super(name);
	}

	/**
	 * Override if parameter sets of a certain type need a special implementation
	 */
	protected NonFlatModule createParameterSet(final String type) {
		return new NonFlatModule( type );
	}

	protected NonFlatModule createAndAddParameterSet(final String type) {
		final NonFlatModule m = createParameterSet( type );
		addParameterSet( type , m );
		return m;
	}

	public void addParameterSet(
			final String type,
			final Module set) {
		MapUtils.getCollection(
				type,
				parameterSetsPerType ).add( set );
	}

	public Collection<Module> getParameterSets(final String type) {
		final Collection<Module> sets = parameterSetsPerType.get( type );
		return sets == null ?
			Collections.<Module>emptySet() :
			Collections.unmodifiableCollection( sets );
	}

	public Map<String, Collection<Module>> getParameterSets() {
		// TODO: immutabilize (including lists)
		return parameterSetsPerType;
	}
}

