/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;

public interface DigicorePathDependentNetworkWriterHandler {

	/* <network> ... </network> */
	public void startNetwork(final PathDependentNetwork network, final BufferedWriter out) throws IOException;
	public void endNetwork(final BufferedWriter out) throws IOException;
	
	/* <node> ... </node> */
	public void startNode(final PathDependentNode node, final BufferedWriter out) throws IOException;
	public void endNode(final PathDependentNode node, final BufferedWriter out) throws IOException;
	
	/* <preceding> ... </preceding> */
	public void startPreceding(final Id<Node> id, final BufferedWriter out) throws IOException;
	public void endPreceding(final BufferedWriter out) throws IOException;
	
	/* <following> ... </following> */
	public void startFollowing(final Map<Id<Node>, Double> following, final BufferedWriter out) throws IOException;
	public void endFollowing(final BufferedWriter out) throws IOException;
	
	/* <starttime> ... </starttime> */
	public void startStartTime(final Map<String, Integer> starttime, final BufferedWriter out) throws IOException;
	public void endStartTime(BufferedWriter out) throws IOException;
	
	/* <activities> ... </activities> */
	public void startActivities(final Map<String, Integer> activities, final BufferedWriter out) throws IOException;
	public void endActivities(BufferedWriter out) throws IOException;
	
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}

