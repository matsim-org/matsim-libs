/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriterHandler.java
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

package org.matsim.core.network.io;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.io.BufferedWriter;
import java.io.IOException;

interface NetworkWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <network ... > ... </network>
	//////////////////////////////////////////////////////////////////////

	public void startNetwork(final Network network, final BufferedWriter out) throws IOException;

	public void endNetwork(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	public void startNodes(final Network network, final BufferedWriter out) throws IOException;

	public void endNodes(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </links>
	//////////////////////////////////////////////////////////////////////

	public void startLinks(final Network network, final BufferedWriter out) throws IOException;

	public void endLinks(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <node ... > ... </node>
	//////////////////////////////////////////////////////////////////////

	public void startNode(final Node node, final BufferedWriter out) throws IOException;

	public void endNode(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <link ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	public void startLink(final Link link, final BufferedWriter out) throws IOException;

	public void endLink(final BufferedWriter out) throws IOException;
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
}
