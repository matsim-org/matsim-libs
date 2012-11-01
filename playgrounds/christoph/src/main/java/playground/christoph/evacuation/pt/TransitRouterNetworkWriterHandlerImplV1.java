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

package playground.christoph.evacuation.pt;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

/*package*/ class TransitRouterNetworkWriterHandlerImplV1 implements TransitRouterNetworkWriterHandler {

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	private final Counter nodesCounter = new Counter("# written nodes: "); 
	private final Counter linksCounter = new Counter("# written links: ");
	
	//////////////////////////////////////////////////////////////////////
	// <network ... > ... </network>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNetwork(final TransitRouterNetwork network, final BufferedWriter out) throws IOException {
		out.write("<transitRouterNetwork");
		out.write(">\n\n");
		
		nodesCounter.reset();
		linksCounter.reset();
	}

	@Override
	public void endNetwork(final BufferedWriter out) throws IOException {
		out.write("</transitRouterNetwork>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNodes(final TransitRouterNetwork network, final BufferedWriter out) throws IOException {
		out.write("\t<nodes>\n");
	}

	@Override
	public void endNodes(final BufferedWriter out) throws IOException {
		out.write("\t</nodes>\n\n");
		
		nodesCounter.printCounter();
	}

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </links>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startLinks(final TransitRouterNetwork network, final BufferedWriter out) throws IOException {
		out.write("\t<links");
		out.write(">\n");
	}

	@Override
	public void endLinks(final BufferedWriter out) throws IOException {
		out.write("\t</links>\n\n");
		
		linksCounter.printCounter();
	}

	//////////////////////////////////////////////////////////////////////
	// <node ... > ... </node>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNode(final TransitRouterNetworkNode node, final BufferedWriter out) throws IOException {
		out.write("\t\t<node");
		out.write(" id=\"" + node.getId() + "\"");
		out.write(" stopfacility=\"" + node.getStop().getStopFacility().getId() + "\"");
		out.write(" route=\"" + node.getRoute().getId() + "\"");
		out.write(" line=\"" + node.getLine().getId() + "\"");
		out.write(" />\n");		
	}

	@Override
	public void endNode(final BufferedWriter out) throws IOException {
		nodesCounter.incCounter();
	}

	//////////////////////////////////////////////////////////////////////
	// <link ... > ... </link>
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void startLink(final TransitRouterNetworkLink link, final BufferedWriter out) throws IOException {
		out.write("\t\t<link");
		out.write(" id=\"" + link.getId() + "\"");
		out.write(" from=\"" + link.getFromNode().getId() + "\"");
		out.write(" to=\"" + link.getToNode().getId() + "\"");
		out.write(" length=\"" + link.getLength() + "\"");
		if (link.getRoute() != null) {
			out.write(" route=\"" + link.getRoute().getId() + "\"");			
		}
		if (link.getLine() != null) {
			out.write(" line=\"" + link.getLine().getId() + "\"");			
		}
		out.write(" />\n");		
	}

	@Override
	public void endLink(final BufferedWriter out) throws IOException {
		linksCounter.incCounter();
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- ====================================================================== -->\n\n");
	}
}
