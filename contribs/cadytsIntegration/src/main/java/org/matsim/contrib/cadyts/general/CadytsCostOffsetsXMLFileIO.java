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

import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.basic.v01.IdImpl;

import cadyts.utilities.misc.DynamicDataXMLFileIO;



/**
 * Enables cadyts to persist the cost offsets to file.
 */
public final class CadytsCostOffsetsXMLFileIO<T extends Identifiable> extends DynamicDataXMLFileIO<T> {

	private static final long serialVersionUID = 1L;
	private LookUp<T> lookUp;

	public CadytsCostOffsetsXMLFileIO(final LookUp<T> lookUp ) {
		super();
		this.lookUp = lookUp ;
	}

	@Override
	protected T attrValue2key(final String stopId) {
		return this.lookUp.lookUp(new IdImpl(stopId)) ;
	}

	@Override
	protected String key2attrValue(final T key) {
		return key.getId().toString();
	}

}
