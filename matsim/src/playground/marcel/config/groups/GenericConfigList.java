/* *********************************************************************** *
 * project: org.matsim.*
 * GenericConfigList.java
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

package playground.marcel.config.groups;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

public class GenericConfigList implements ConfigListI {

	private Map<String, ConfigGroupI> entries = new LinkedHashMap<String, ConfigGroupI>();
	
	public ConfigGroupI getGroup(String key) {
		return this.entries.get(key);
	}

	public ConfigGroupI addGroup(String key) {
		ConfigGroupI group = new GenericConfigGroup(key);
		this.entries.put(key, group);
		return group;
	}
	
	public Set<String> keySet() {
		return this.entries.keySet();
	}


}
