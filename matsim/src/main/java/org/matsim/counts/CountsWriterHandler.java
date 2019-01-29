/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWriterHandler.java
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
import java.io.BufferedWriter;
import java.io.IOException;

interface CountsWriterHandler {
	//////////////////////////////////////////////////////////////////////
	// <counts ... > ... </counts>
	//////////////////////////////////////////////////////////////////////
	public void startCounts(final Counts counts, final BufferedWriter out) throws IOException;
	public void endCounts(final BufferedWriter out) throws IOException;
	//////////////////////////////////////////////////////////////////////
	// <count ... > ... </count>
	//////////////////////////////////////////////////////////////////////
	public void startCount(final Count count, final BufferedWriter out) throws IOException;
	public void endCount(final BufferedWriter out) throws IOException;
	//////////////////////////////////////////////////////////////////////
	// <volume ... />
	//////////////////////////////////////////////////////////////////////
	public void startVolume(final Volume volume, final BufferedWriter out) throws IOException;
	public void endVolume(final BufferedWriter out) throws IOException;
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
}
