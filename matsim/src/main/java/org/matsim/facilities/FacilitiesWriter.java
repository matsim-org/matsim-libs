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

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

/**
 * @author mrieser / Senozon AG
 */
public class FacilitiesWriter implements MatsimWriter {

	private final ActivityFacilities facilities;
	private final CoordinateTransformation coordinateTransformation;

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the file.
	 *
	 * @param facilities the facilities to write
	 */
	public FacilitiesWriter(final ActivityFacilities facilities) {
		this( new IdentityTransformation() , facilities );
	}

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the file.
	 *
	 * @param coordinateTransformation a transformation from the CRS in the data structure to the CRS to use in the file
	 * @param facilities the facilities to write
	 */
	public FacilitiesWriter(
			final CoordinateTransformation coordinateTransformation,
			final ActivityFacilities facilities) {
		this.coordinateTransformation = coordinateTransformation;
		this.facilities = facilities;
	}

	/**
	 * Writes the activity facilities in the current default format 
	 * (currently facilities_v1.dtd). 
	 */
	@Override
	public final void write(final String filename) {
		new FacilitiesWriterV1( coordinateTransformation , facilities).write(filename);
	}
	
	public final void writeV1(final String filename) {
		new FacilitiesWriterV1( coordinateTransformation , facilities ).write(filename);
	}


}
