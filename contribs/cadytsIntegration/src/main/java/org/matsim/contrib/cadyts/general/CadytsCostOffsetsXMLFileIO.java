/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import cadyts.utilities.misc.DynamicDataXMLFileIO;

/**
 * Enables cadyts to persist the cost offsets to file.
 */
public final class CadytsCostOffsetsXMLFileIO<T extends Identifiable<T>> extends DynamicDataXMLFileIO<T> {
	// yyyy this is most probably not "costs" but just "offsets" (which end up being added into the scoring function, so if anything
	// they are negative costs).  --> rename kai/janek, feb'19

	private static final long serialVersionUID = 1L;
	private LookUpItemFromId<T> lookUp;
	private Class<T> idType;

	public CadytsCostOffsetsXMLFileIO(final LookUpItemFromId<T> lookUp, Class<T> idType) {
		super();
		this.lookUp = lookUp;
		this.idType = idType;
	}

	@Override
	protected T attrValue2key(final String stopId) {
		return this.lookUp.getItem(Id.create(stopId, idType));
	}

	@Override
	protected String key2attrValue(final T key) {
		return key.getId().toString();
	}

}
