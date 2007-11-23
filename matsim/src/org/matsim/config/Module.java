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


/* *********************************************************************** *
 *                    org.matsim.demandmodeling.config                     *
 *                               Module.java                               *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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

import org.matsim.gbl.Gbl;

public class Module {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String name;
	private final TreeMap<String,String> params;

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public Module(final String name) {
		this.name = name;
		this.params = new TreeMap<String,String>();
	}

	//////////////////////////////////////////////////////////////////////
	// add / set methods
	//////////////////////////////////////////////////////////////////////

	public void addParam(final String param_name, final String value) {
		if (this.params.containsKey(param_name)) {
			Gbl.noteMsg(this.getClass(),"addParam(...)",this.toString() + "[param_name=" + param_name + ",old_value=" + this.params.get(param_name) + ",value=" + value + " value replaced]");
		}

		// changing windows path backslashes into unix path slashes
		String new_value = value.replace('\\','/');
		if (!new_value.equals(value)) {
			Gbl.noteMsg(this.getClass(),"addParam(...)",this.toString() + "[value=" + value + ",new_value=" + new_value + " replaced backslashes with slashes]");
		}
		this.params.put(param_name,new_value);
	}
	
	/**
	 * Little helper for subclasses (i.e. the ConfigGroups). This method adds a parameter
	 * to the given map only if the getValue() method of this Module doesn't return
	 * null (Java null-type) or the String representation of null, i.e. "null" or "NULL".
	 * @param map
	 * @param paramName
	 */
	protected void addNotNullParameterToMap(final Map<String, String> map, final String paramName) {
		String value = this.getValue(paramName);
		if (!((value == null) || value.equalsIgnoreCase("null"))) {
			map.put(paramName, value);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public String getValue(final String param_name) {
		return this.params.get(param_name);
	}

	protected final String getName() {
		return this.name;
	}

	protected TreeMap<String, String> getParams() {
		return this.params;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_params=" + this.params.size() + "]";
	}
}
