/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.world;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * @author nagel
 *
 */
public interface Mappings {
	
	/**
	 * Connects two location of two different layers. The layers have to be 'neighbors' which
	 * means that the layers are connected via a MappingRule. The other layer have to be 'above'
	 * the one this location belongs to.
	 * @param other
	 */
	@Deprecated
	public abstract void addUpMapping(final MappedLocation other);

	/**
	 * Connects two location of two different layers. The layers have to be 'neighbors' which
	 * means that the layers are connected via a MappingRule. The other layer have to be 'below'
	 * the one this location belongs to.
	 * @param other
	 */
	@Deprecated
	public abstract void addDownMapping(final MappedLocation other);

	@Deprecated
	public abstract boolean removeAllUpMappings();

	@Deprecated
	public abstract boolean removeAllDownMappings();

	@Deprecated
	public abstract TreeMap<Id, MappedLocation> getUpMapping();

	@Deprecated
	public abstract TreeMap<Id, MappedLocation> getDownMapping();

}
