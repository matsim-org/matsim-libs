/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.pt;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

public interface TransitRouterNetworkWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <network ... > ... </network>
	//////////////////////////////////////////////////////////////////////

	public void startNetwork(final TransitRouterNetwork network, final BufferedWriter out) throws IOException;

	public void endNetwork(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	public void startNodes(final TransitRouterNetwork network, final BufferedWriter out) throws IOException;

	public void endNodes(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </links>
	//////////////////////////////////////////////////////////////////////

	public void startLinks(final TransitRouterNetwork network, final BufferedWriter out) throws IOException;

	public void endLinks(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <node ... > ... </node>
	//////////////////////////////////////////////////////////////////////

	public void startNode(final TransitRouterNetworkNode node, final BufferedWriter out) throws IOException;

	public void endNode(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <link ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	public void startLink(final TransitRouterNetworkLink link, final BufferedWriter out) throws IOException;

	public void endLink(final BufferedWriter out) throws IOException;
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
}
