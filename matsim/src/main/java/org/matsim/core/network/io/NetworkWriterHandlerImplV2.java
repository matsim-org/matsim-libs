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

package org.matsim.core.network.io;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/*package*/ class NetworkWriterHandlerImplV2 implements NetworkWriterHandler {
	private final CoordinateTransformation transformation;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

	NetworkWriterHandlerImplV2(CoordinateTransformation transformation) {
		this.transformation = transformation;
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		attributesWriter.putAttributeConverters(converters);
	}

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <network ... > ... </network>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNetwork(final Network network, final BufferedWriter out) throws IOException {
		out.write("<network");
		if (network instanceof Network && (network.getName() != null)) {
			out.write(" name=\"" + network.getName() + "\"");
		}
		out.write(">\n\n");

		attributesWriter.writeAttributes( "\t" , out , network.getAttributes() );
	}

	@Override
	public void endNetwork(final BufferedWriter out) throws IOException {
		out.write("</network>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNodes(final Network network, final BufferedWriter out) throws IOException {
		out.write("\t<nodes>\n");
	}

	@Override
	public void endNodes(final BufferedWriter out) throws IOException {
		out.write("\t</nodes>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </links>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startLinks(final Network network, final BufferedWriter out) throws IOException {
		out.write("\t<links");
		if (network.getCapacityPeriod() != Integer.MIN_VALUE) {
			out.write(" capperiod=\"" + Time.writeTime(network.getCapacityPeriod()) + "\"");
		}

		if (network instanceof Network) {
			out.write(" effectivecellsize=\"" + ((Network) network).getEffectiveCellSize() + "\"");
			out.write(" effectivelanewidth=\"" + network.getEffectiveLaneWidth() + "\"");
		}

		out.write(">\n");
	}

	@Override
	public void endLinks(final BufferedWriter out) throws IOException {
		out.write("\t</links>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <node ... > ... </node>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNode(final Node node, final BufferedWriter out) throws IOException {
		out.write("\t\t<node");
		out.write(" id=\"" + node.getId() + "\"");
		final Coord coord = transformation.transform( node.getCoord() );
		out.write(" x=\"" + coord.getX() + "\"");
		out.write(" y=\"" + coord.getY() + "\"");
		if ( coord.hasZ() ) out.write(" z=\"" + coord.getZ() + "\"");
		if (NetworkUtils.getType( node ) != null) {
			out.write(" type=\"" + NetworkUtils.getType( node ) + "\"");
		}
		if (NetworkUtils.getOrigId( node ) != null) {
			out.write(" origid=\"" + NetworkUtils.getOrigId( node ) + "\"");
		}
		out.write(" >\n");

		attributesWriter.writeAttributes( "\t\t\t" , out , node.getAttributes() );
	}

	@Override
	public void endNode(final BufferedWriter out) throws IOException {
		out.write("\t\t</node>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <link ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	private Set<String> lastSet = null;
	private String lastModes = null;

	@Override
	public void startLink(final Link link, final BufferedWriter out) throws IOException {
		out.write("\t\t<link");
		out.write(" id=\"" + link.getId() + "\"");
		out.write(" from=\"" + link.getFromNode().getId() + "\"");
		out.write(" to=\"" + link.getToNode().getId() + "\"");
		out.write(" length=\"" + link.getLength() + "\"");
		out.write(" freespeed=\"" + link.getFreespeed() + "\"");
		out.write(" capacity=\"" + link.getCapacity() + "\"");
		out.write(" permlanes=\"" + link.getNumberOfLanes() + "\"");
		out.write(" oneway=\"1\"");

		Set<String> modes = link.getAllowedModes();
		if (modes != null) {
			if (modes != this.lastSet) { // default LinkImpl internally caches the modes-set, thus the != operator works indeed
				StringBuilder buffer = new StringBuilder();
				int counter = 0;
				for (String mode : modes) {
					if (counter > 0) {
						buffer.append(',');
					}
					buffer.append(mode);
					counter++;
				}
				this.lastModes = buffer.toString();
				this.lastSet = modes;
			}
			out.write(" modes=\"" + this.lastModes + "\"");
		}

		if (link instanceof Link) {
			Link li = (Link) link;
			if (NetworkUtils.getOrigId( li ) != null) {
				out.write(" origid=\"" + NetworkUtils.getOrigId( li ) + "\"");
			}
			if (NetworkUtils.getType(li) != null) {
				out.write(" type=\"" + NetworkUtils.getType(li) + "\"");
			}
		}
		out.write(" >\n");

		attributesWriter.writeAttributes( "\t\t\t" , out , link.getAttributes() );
	}

	@Override
	public void endLink(final BufferedWriter out) throws IOException {
		out.write("\t\t</link>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
				"===================== -->\n\n");
	}
}
