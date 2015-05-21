/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.cliques.config;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

/**
 * Defines the file used to import clique information.
 * @author thibautd
 */
public class CliquesConfigGroup extends ConfigGroup {

	private static final Logger log = Logger.getLogger(CliquesConfigGroup.class);

	public static final String GROUP_NAME = "Cliques";

	//parameter names
	private static final String FILE = "inputCliquesFile";

	//parameter values
	private String file;

	public CliquesConfigGroup() {
		super(GROUP_NAME);
		log.debug("cliques config group initialized");
	}

	/*
	 * =========================================================================
	 * base class methods
	 * =========================================================================
	 */
	@Override
	public void addParam(String param_name, String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
	
		log.debug("addParam called for cliques: param_name="+param_name+", value="+value);
		if (param_name.equals(FILE)) {
			log.debug("file field detected");
			this.file = value;
		}
	}

	@Override
	public String getValue(String param_name) {
		if (param_name.equals(FILE)) {
			return this.file;
		}
		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		this.addParameterToMap(map, FILE);
		return map;
	}

	/*
	 * =========================================================================
	 * getters/setters
	 * =========================================================================
	 */

	public String getInputFile() {
		return this.file;
	}

	public void setInputFile(String file) {
		this.file = file;
	}

}

