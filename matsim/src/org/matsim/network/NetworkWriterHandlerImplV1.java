/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriterHandlerImplV1.java
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

package org.matsim.network;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.utils.misc.Time;

public class NetworkWriterHandlerImplV1 implements NetworkWriterHandler {

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <network ... > ... </network>
	//////////////////////////////////////////////////////////////////////

	public void startNetwork(final NetworkLayer network, final BufferedWriter out) throws IOException {
		out.write("<network");
		if (network.getName() != null) {
			out.write(" name=\"" + network.getName() + "\"");
		}
		out.write(">\n\n");
	}

	public void endNetwork(final BufferedWriter out) throws IOException {
		out.write("</network>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	public void startNodes(final NetworkLayer network, final BufferedWriter out) throws IOException {
		out.write("\t<nodes");
		out.write(">\n");
	}

	public void endNodes(final BufferedWriter out) throws IOException {
		out.write("\t</nodes>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </links>
	//////////////////////////////////////////////////////////////////////

	public void startLinks(final NetworkLayer network, final BufferedWriter out) throws IOException {
		out.write("\t<links");
		if (network.getCapacityPeriod() != Integer.MIN_VALUE) {
			out.write(" capperiod=\"" + Time.writeTime(network.getCapacityPeriod()) + "\"");
		}
		out.write(">\n");
	}

	public void endLinks(final BufferedWriter out) throws IOException {
		out.write("\t</links>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <node ... > ... </node>
	//////////////////////////////////////////////////////////////////////

	public void startNode(final Node node, final BufferedWriter out) throws IOException {
		out.write("\t\t<node");
		out.write(" id=\"" + node.getId() + "\"");
		out.write(" x=\"" + node.getCoord().getX() + "\"");
		out.write(" y=\"" + node.getCoord().getY() + "\"");
		if (node.getType() != null) {
			out.write(" type=\"" + node.getType() + "\"");
		}
		if (node.getOrigId() != null) {
			out.write(" origid=\"" + node.getOrigId() + "\"");
		}
		out.write(" />\n");
	}

	public void endNode(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <link ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	public void startLink(final Link link, final BufferedWriter out) throws IOException {
		out.write("\t\t<link");
		out.write(" id=\"" + link.getId() + "\"");
		out.write(" from=\"" + link.getFromNode().getId() + "\"");
		out.write(" to=\"" + link.getToNode().getId() + "\"");
		out.write(" length=\"" + link.getLength() + "\"");
		out.write(" freespeed=\"" + link.getFreespeed() + "\"");
		out.write(" capacity=\"" + link.getCapacity() + "\"");
		out.write(" permlanes=\"" + link.getLanes() + "\"");
		out.write(" oneway=\"" + "1" + "\"");
		if (link.getOrigId() != null) {
			out.write(" origid=\"" + link.getOrigId() + "\"");
		}
		if (link.getType() != null) {
			out.write(" type=\"" + link.getType() + "\"");
		}
		out.write(" />\n");
	}

	public void endLink(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}
