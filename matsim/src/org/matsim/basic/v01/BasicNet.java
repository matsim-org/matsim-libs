/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNet.java
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

package org.matsim.basic.v01;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;

public class BasicNet implements BasicNetI{
	protected final BasicLinkSetI links  = new BasicLinkSet();
	protected final BasicNodeSetI nodes  = new BasicNodeSet();

	public boolean add(BasicNodeI node) {
		nodes.add(node);
		return true;
	}

	public boolean add(BasicLinkI link) {
		links.add(link);
		return true;
	}

	public BasicLinkSetI getLinks() {
		return links;
	}

	public BasicNodeSetI getNodes() {
		return nodes;
	}

	public BasicLinkI newLink(String label) {
		throw new UnsupportedOperationException("Please implement/override method before using!");
	}

	public BasicNodeI newNode(String label) {
		throw new UnsupportedOperationException("Please implement/override method before using!");
	}

	public void connect() {
		System.out.println("Please implement method before using!");
	}

}
