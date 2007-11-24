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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.identifiers.IdI;

public class BasicNet implements BasicNetI {
	protected final TreeMap<IdI, BasicLinkI> links = new TreeMap<IdI, BasicLinkI>();
	protected final TreeMap<IdI, BasicNodeI> nodes = new TreeMap<IdI, BasicNodeI>();

	public boolean add(BasicNodeI node) {
		nodes.put(node.getId(), node);
		return true;
	}

	public boolean add(BasicLinkI link) {
		links.put(link.getId(), link);
		return true;
	}

	public Map<IdI, ? extends BasicLinkI> getLinks() {
		return links;
	}

	public Map<IdI, ? extends BasicNodeI> getNodes() {
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
