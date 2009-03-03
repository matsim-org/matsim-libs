/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriterHandler.java
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

package playground.christoph.knowledge.nodeselection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.OpeningTime;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.population.ActivitySpace;
import org.matsim.population.Knowledge;
import org.matsim.writer.WriterHandler;

public interface SelectionWriterHandler extends WriterHandler {

	//////////////////////////////////////////////////////////////////////
	// <selection ... > ... </selection>
	//////////////////////////////////////////////////////////////////////
	public void startSelection(final String description, final BufferedWriter out) throws IOException;
	
	public void endSelection(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <person ... > ... </person>
	//////////////////////////////////////////////////////////////////////

	public void startPerson(final Person person, final BufferedWriter out) throws IOException;

	public void endPerson(final BufferedWriter out) throws IOException;
	
	//////////////////////////////////////////////////////////////////////
	// <knowledge ... > ... </knowledge>
	//////////////////////////////////////////////////////////////////////

	public void startKnowledge(final Knowledge knowledge, final BufferedWriter out) throws IOException;

	public void endKnowledge(final BufferedWriter out) throws IOException;


	//////////////////////////////////////////////////////////////////////
	// <activityspace ... > ... </activityspace>
	//////////////////////////////////////////////////////////////////////

	public void startActivitySpace(final BufferedWriter out) throws IOException;

	public void endActivitySpace(final BufferedWriter out) throws IOException;

	//////////////////////////////////////////////////////////////////////
	// <nodes ... > ... </nodes>
	//////////////////////////////////////////////////////////////////////

	public void startNodes(final BufferedWriter out) throws IOException;

	public void node(final Node node, final BufferedWriter out) throws IOException;
	
	public void nodes(final Map<Id, Node> nodes, final BufferedWriter out) throws IOException;
	
	public void endNodes(final BufferedWriter out) throws IOException;
	
	//////////////////////////////////////////////////////////////////////
	// <links ... > ... </link>
	//////////////////////////////////////////////////////////////////////

	public void startLinks(final BufferedWriter out) throws IOException;
	
	public void link(final Link link, final BufferedWriter out) throws IOException;
	
	public void links(final Map<Id, Link> links, final BufferedWriter out) throws IOException;
	
	public void endLinks(final BufferedWriter out) throws IOException;
}
