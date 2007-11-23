/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigGroupI.java
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

package playground.marcel.config;

import java.util.LinkedHashSet;
import java.util.Set;

public interface ConfigGroupI {
	
	Set<String> EMPTY_LIST_SET= new LinkedHashSet<String>();
	
	public String getName();
	public void setValue(String key, String value);
	public String getValue(String key);
	public Set<String> paramKeySet();
	public Set<String> listKeySet();
	
	public ConfigListI getList(String key);
}
