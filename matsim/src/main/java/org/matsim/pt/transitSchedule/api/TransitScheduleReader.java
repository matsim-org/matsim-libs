/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule.api;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;

/**
 * Reads {@link TransitSchedule}s from file as long as the files are in one of the
 * supported file formats.
 *
 * @author mrieser
 */
public class TransitScheduleReader implements MatsimSomeReader {

	private final Scenario scenario;
	private final CoordinateTransformation transformation;

	public TransitScheduleReader(
			final CoordinateTransformation transformation,
			final Scenario scenario) {
		this.transformation = transformation;
		this.scenario = scenario;
	}

	public TransitScheduleReader(final Scenario scenario) {
		this( new IdentityTransformation() , scenario );
	}

	public void readFile(final String filename) throws UncheckedIOException {
		MatsimFileTypeGuesser guesser = new MatsimFileTypeGuesser(filename);
		String systemId = guesser.getSystemId();
		if (systemId.endsWith("transitSchedule_v1.dtd")) {
			new TransitScheduleReaderV1( transformation , this.scenario).readFile(filename);
		} else {
			throw new UncheckedIOException("Unsupported file format: " + systemId);
		}

	}

}
