/* *********************************************************************** *
 * project: org.matsim.*
 * SelectionWriterHandlerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge.nodeselection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;

import playground.christoph.knowledge.container.NodeKnowledge;

public class SelectionWriterHandlerImpl implements SelectionWriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <selection ... > ... </selection>
	//////////////////////////////////////////////////////////////////////
	@Override
	public void startSelection(final String description, final BufferedWriter out) throws IOException
	{
		out.write("<selection");
		out.write(" desc=\"" + description + "\"");
		out.write(">\n");
	}

	@Override
	public void endSelection(final BufferedWriter out) throws IOException
	{
		out.write("</selection>");
	}


	//////////////////////////////////////////////////////////////////////
	// <person ... > ... </person>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startPerson(final Person pp, final BufferedWriter out) throws IOException
	{
		PersonImpl person = (PersonImpl) pp;
		out.write("\t<person");
		out.write(" id=\"" + person.getId() + "\"");
		if (person.getSex() != null)
			out.write(" sex=\"" + person.getSex() + "\"");
		if (person.getAge() != Integer.MIN_VALUE)
			out.write(" age=\"" + person.getAge() + "\"");
		if (person.getLicense() != null)
			out.write(" license=\"" + person.getLicense() + "\"");
		if (person.getCarAvail() != null)
			out.write(" car_avail=\"" + person.getCarAvail() + "\"");
		if (person.isEmployed() != null)
			out.write(" employed=\"" + (person.isEmployed() ? "yes" : "no") + "\"");
		out.write(">\n");
	}

	@Override
	public void endPerson(final BufferedWriter out) throws IOException
	{
		out.write("\t</person>\n\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <knowledge ... > ... </knowledge>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startKnowledge(final NodeKnowledge nodeKnowledge, final BufferedWriter out) throws IOException
	{
		out.write("\t\t<knowledge");
		out.write(">\n");
	}

	@Override
	public void endKnowledge(final BufferedWriter out) throws IOException
	{
		out.write("\t\t</knowledge>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <activityspace ... > ... </activityspace>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startActivitySpace(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t<activityspace>\n");
	}

	@Override
	public void endActivitySpace(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t</activityspace>\n");
	}


	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startNodes(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t<nodes>\n");
	}

	@Override
	public void node(final Node node, final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t\t<node id=\"" + node.getId() + "\"/>\n");
	}

	@Override
	public void nodes(final Map<Id, Node> nodes, final BufferedWriter out) throws IOException
	{
		for(Node node:nodes.values())
		{
			node(node, out);
		}
	}

	@Override
	public void endNodes(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t</nodes>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	@Override
	public void startLinks(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t<links>");
	}

	@Override
	public void link(final Link link, final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t\t<link id=\"" + link.getId() + "/>\n");
	}

	@Override
	public void links(final Map<Id, Link> links, final BufferedWriter out) throws IOException
	{
		for(Link link:links.values())
		{
			link(link, out);
		}
	}

	@Override
	public void endLinks(final BufferedWriter out) throws IOException
	{
		out.write("\t\t\t\t</links>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	@Override
	public void writeSeparator(final BufferedWriter out) throws IOException
	{
		out.write("<!-- ====================================================================== -->\n\n");
	}
}
