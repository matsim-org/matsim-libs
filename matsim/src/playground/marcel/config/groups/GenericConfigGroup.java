/* *********************************************************************** *
 * project: org.matsim.*
 * GenericConfigGroup.java
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

public class GenericConfigGroup implements ConfigGroupI {

	private String name;
	
	private Map<String, String> values = new LinkedHashMap<String, String>();
	private Map<String, ConfigListI> lists = new LinkedHashMap<String, ConfigListI>();
	
	public GenericConfigGroup(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
	
	public String getValue(String key) {
		return this.values.get(key);
	}

	public void setValue(String key, String value) {
		this.values.put(key, value);
	}

	public Set<String> paramKeySet() {
		return this.values.keySet();
	}

	public Set<String> listKeySet() {
		return this.lists.keySet();
	}

	public ConfigListI getList(String key) {
		ConfigListI list = this.lists.get(key);
		if (list == null) {
			list = new GenericConfigList();
			this.lists.put(key, list);
		}
		return list;
	}

}
