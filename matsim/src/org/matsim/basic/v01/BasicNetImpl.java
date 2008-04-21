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

import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.interfaces.networks.basicNet.BasicNode;

public class BasicNetImpl implements BasicNet {
	protected final TreeMap<Id, BasicLink> links = new TreeMap<Id, BasicLink>();
	protected final TreeMap<Id, BasicNode> nodes = new TreeMap<Id, BasicNode>();

	public boolean add(BasicNode node) {
		nodes.put(node.getId(), node);
		return true;
	}

	public boolean add(BasicLink link) {
		links.put(link.getId(), link);
		return true;
	}

	public Map<Id, ? extends BasicLink> getLinks() {
		return links;
	}

	public Map<Id, ? extends BasicNode> getNodes() {
		return nodes;
	}

	public BasicLink newLink(String label) {
		throw new UnsupportedOperationException("Please implement/override method before using!");
	}

	public BasicNode newNode(String label) {
		throw new UnsupportedOperationException("Please implement/override method before using!");
	}

	public void connect() {
		System.out.println("Please implement method before using!");
	}

}
