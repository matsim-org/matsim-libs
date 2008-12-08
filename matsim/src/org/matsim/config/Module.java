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

package org.matsim.config;

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
	
	private final static Logger log = Logger.getLogger(Module.class);

	public Module(final String name) {
		this.name = name;
		this.params = new TreeMap<String,String>();
	}

	public void addParam(final String param_name, final String value) {
		if (this.params.containsKey(param_name)) {
			log.info(this.toString() + "[param_name=" + param_name + ",old_value=" + this.params.get(param_name) + ",value=" + value + " value replaced]");
		}

		// changing windows path backslashes into unix path slashes
		String new_value = value.replace('\\','/');
		if (!new_value.equals(value)) {
			log.info(this.toString() + "[value=" + value + ",new_value=" + new_value + " replaced backslashes with slashes]");
		}
		this.params.put(param_name,new_value);
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

	public String getValue(final String param_name) {
		return this.params.get(param_name);
	}

	protected final String getName() {
		return this.name;
	}

	/** @return a Map containing all parameters and their values known to this config group. */
	protected Map<String, String> getParams() {
		return this.params;
	}
	
//	protected Map<String,String> getComments() { // TODO (see email)
//		return null ;
//	}

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_params=" + this.params.size() + "]";
	}
}
