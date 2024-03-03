/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleWriter.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;

import java.io.UncheckedIOException;

/**
 * Writes {@link TransitSchedule}s to file in one of the
 * supported file formats.
 *
 * @author mrieser
 */
public class TransitScheduleWriter implements MatsimSomeWriter {
	private static final Logger log = LogManager.getLogger( TransitScheduleWriter.class ) ;

	private final TransitSchedule schedule;
	private final CoordinateTransformation transformation;

	public TransitScheduleWriter(final TransitSchedule schedule) {
		this( new IdentityTransformation() , schedule );
	}

	public TransitScheduleWriter(
			final CoordinateTransformation transformation,
			final TransitSchedule schedule) {
		this.transformation = transformation;
		this.schedule = schedule;
	}

	/**
	 * Writes the transit schedule to the specified file in the most
	 * current file format (currently V2).
	 *
	 * @param filename
	 * @throws UncheckedIOException
	 * @see {@link #writeFileV2(String)}
	 */
	public void writeFile(final String filename) throws UncheckedIOException {
		writeFileV2(filename);
	}

	/**
	 * Writes the transit schedule to the specified file in the file
	 * format specified by <tt>transitSchedule_v1.dtd</tt>
	 *
	 * @param filename
	 * @throws UncheckedIOException
	 */
	public void writeFileV1(final String filename) throws UncheckedIOException {
		log.info( Gbl.aboutToWrite("transit schedule", filename) ) ;
		new TransitScheduleWriterV1(this.transformation, this.schedule).write(filename);
	}

	/**
	 * Writes the transit schedule to the specified file in the file
	 * format specified by <tt>transitSchedule_v2.dtd</tt>
	 *
	 * @param filename
	 * @throws UncheckedIOException
	 */
	public void writeFileV2(final String filename) throws UncheckedIOException {
		log.info( Gbl.aboutToWrite( "transit schedule" , filename ) ) ;
		new TransitScheduleWriterV2(this.transformation, this.schedule).write(filename);
	}
}
