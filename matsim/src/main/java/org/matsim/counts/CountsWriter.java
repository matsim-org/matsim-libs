/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWriter.java
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

package org.matsim.counts;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;

/**
 * Generalized counts writer. Which delegates to specific version. Version 1 can be forced with system property
 * MATSIM_COUNTS_VERSION=1
 */
public class CountsWriter extends MatsimXmlWriter implements MatsimWriter {

	private final CoordinateTransformation ct;
	private final Counts counts;

	public CountsWriter(final Counts counts) {
		this(new IdentityTransformation(), counts);
	}

	public CountsWriter(
		final CoordinateTransformation ct,
		final Counts counts) {
		this.ct = ct;
		this.counts = counts;
	}

	@Override
	public final void write(final String filename) {
		try {
			if (System.getProperty("MATSIM_COUNTS_VERSION", "").equalsIgnoreCase("1"))
				new CountsWriterV1(ct, counts).write(filename);
			else
				new CountsWriterV2(ct, counts).write(filename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final String toString() {
		return super.toString();
	}
}
