/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriterHandler.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.writer.WriterHandler;

interface FacilitiesWriterHandler extends WriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <facilities ... > ... </facilities>
	//////////////////////////////////////////////////////////////////////

	public void startFacilities(final Facilities facilities, final BufferedWriter out) throws IOException;

	public void endFacilities(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <facility ... > ... </facility>
	//////////////////////////////////////////////////////////////////////

	public void startFacility(final Facility facility, final BufferedWriter out) throws IOException;

	public void endFacility(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <activity ... > ... </activity>
	//////////////////////////////////////////////////////////////////////

	public void startActivity(final Activity activity, final BufferedWriter out) throws IOException;

	public void endActivity(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <capacity ... />
	//////////////////////////////////////////////////////////////////////

	public void startCapacity(final Activity activity, final BufferedWriter out) throws IOException;

	public void endCapacity(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <opentime ... />
	//////////////////////////////////////////////////////////////////////

	public void startOpentime(final BasicOpeningTime opentime, final BufferedWriter out) throws IOException;

	public void endOpentime(final BufferedWriter out) throws IOException;
}
