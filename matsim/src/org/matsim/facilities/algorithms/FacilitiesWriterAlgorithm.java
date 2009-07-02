/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriterAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities.algorithms;

import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.algorithms.AbstractFacilityAlgorithm;

/**
 * Use this facilities writer when streaming facilities.
 *
 * @author meisterk
 *
 */
public class FacilitiesWriterAlgorithm extends AbstractFacilityAlgorithm {

	private FacilitiesWriter facilitiesWriter = null;

	public FacilitiesWriterAlgorithm(final ActivityFacilities facilities) {
		super();
		this.facilitiesWriter = new FacilitiesWriter(facilities);
		this.facilitiesWriter.writeOpenAndInit();
	}

	public void run(final ActivityFacility facility) {
		this.facilitiesWriter.writeFacility(facility);
	}

	/**
	 * Calls the facilities writer to close the out stream.
	 * Don't forget to call this method after streaming all facilities.
	 */
	public void finish() {
		this.facilitiesWriter.writeFinish();
	}

}
