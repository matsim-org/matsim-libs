/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriter.java
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

package org.matsim.facilities;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.internal.MatsimWriter;

/**
 * @author mrieser / Senozon AG
 */
public class FacilitiesWriter implements MatsimWriter {

	private final ActivityFacilities facilities;

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the file.
	 *
	 * @param facilities
	 */
	public FacilitiesWriter(final ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	/**
	 * Writes the activity facilities in the current default format 
	 * (currently facilities_v1.dtd). 
	 */
	@Override
	public final void write(final String filename) {
		new FacilitiesWriterV1(facilities).write(filename);
	}
	
	public final void writeV1(final String filename) {
		new FacilitiesWriterV1(facilities).write(filename);
	}


}
