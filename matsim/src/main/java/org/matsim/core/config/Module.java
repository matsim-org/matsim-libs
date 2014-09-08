/* *********************************************************************** *
 * project: org.matsim.*
 * Module.java
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

package org.matsim.core.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Implements a generic config-group that stores all parameters in a simple Map.
 *
 * @author mrieser
 * @author balmermi
 */
public class Module {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String name;
	private final TreeMap<String,String> params;
	private final Map<String, Collection<Module>> parameterSetsPerType = new HashMap<String, Collection<Module>>();

	private final static Logger log = Logger.getLogger(Module.class);

	public Module(final String name) {
		this.name = name;
		this.params = new TreeMap<String,String>();
	}

	public void addParam(final String paramName, final String value) {
		if (this.params.containsKey(paramName)) {
			log.info(this.toString() + "[paramName=" + paramName + ",oldValue=" + this.params.get(paramName) + ",value=" + value + " value replaced]");
		}
		this.params.put(paramName, value);
	}

	/**
	 * Little helper for subclasses (i.e. the ConfigGroups). This method adds the value of the parameter
	 * to the given map only if the getValue() method of this Module doesn't return
	 * null (Java null-type) or the String representation of null, i.e. "null" or "NULL".
	 * If the value is null, the string "null" is added to the map to document the parameter.
	 *
	 * @param map
	 * @param paramName
	 */
	protected void addParameterToMap(final Map<String, String> map, final String paramName) {
		String value = this.getValue(paramName);
		if (!((value == null) || value.equalsIgnoreCase("null"))) {
			map.put(paramName, value);
		} else {
			map.put(paramName, "null");
		}
	}

	/** Check if the set values go well together. This method is usually called after reading the
	 * configuration from a file. If an inconsistency is found, a warning or error should be issued
	 * and (optionally) a RuntimeException being thrown.
	 */
	protected void checkConsistency() {
		/* nothing to do in default */
	}

	public String getValue(final String paramName) {
		return this.params.get(paramName);
	}

	protected final String getName() {
		return this.name;
	}

	/** @return a Map containing all parameters and their values known to this config group. */
	public Map<String, String> getParams() {
		return this.params;
	}

	/**
	 * @return a Map containing description to some or all parameters return in {@link #getParams()}.
	 */
	public Map<String, String> getComments() {
		return new HashMap<String, String>();
	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nOfParams=" + this.params.size() + "]";
	}

	/**
	 * Override if parameter sets of a certain type need a special implementation
	 */
	public Module createParameterSet(final String type) {
		return new Module( type );
	}

	public Module createAndAddParameterSet(final String type) {
		final Module m = createParameterSet( type );
		addParameterSet( type , m );
		return m;
	}

	public void addParameterSet(final String type, final Module set) {
		Collection<Module> parameterSets = parameterSetsPerType.get( type );

		if ( parameterSets == null ) {
			parameterSets = new ArrayList<Module>();
			parameterSetsPerType.put( type ,  parameterSets );
		}

		parameterSets.add( set );
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
