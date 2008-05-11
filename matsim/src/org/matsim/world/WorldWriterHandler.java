/* *********************************************************************** *
 * project: org.matsim.*
 * WorldWriterHandler.java
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

package org.matsim.world;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.writer.WriterHandler;

interface WorldWriterHandler extends WriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <world ... > ... </world>
	//////////////////////////////////////////////////////////////////////

	public void startWorld(final World world, final BufferedWriter out) throws IOException;

	public void endWorld(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <layer ... > ... </layer>
	//////////////////////////////////////////////////////////////////////

	public void startLayer(final ZoneLayer layer, final BufferedWriter out) throws IOException;

	public void endLayer(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <zone ... />
	//////////////////////////////////////////////////////////////////////

	public void startZone(final Zone zone, final BufferedWriter out) throws IOException;

	public void endZone(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <mapping ... > ... </mapping>
	//////////////////////////////////////////////////////////////////////

	public void startMapping(final MappingRule mappingRule, final BufferedWriter out) throws IOException;

	public void endMapping(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <ref ... />
	//////////////////////////////////////////////////////////////////////

	public void startRef(final Zone down_zone, final Zone up_zone, final BufferedWriter out) throws IOException;

	public void endRef(final BufferedWriter out) throws IOException;
}
