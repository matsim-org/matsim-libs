/* *********************************************************************** *
 * project: org.matsim.*
 * PtNode.java
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

package playground.marcel.ptnetwork;
import org.matsim.basic.v01.Id;
import org.matsim.network.Node;

public class PtNode extends Node{

	protected PtNode(final String id, final String x, final String y, final String type) {
		super(new Id(id), x, y, type);
	}

	protected int actTime = Integer.MAX_VALUE;
	protected int actCost = Integer.MAX_VALUE;
	protected PtLink shortestPath = null;
	protected long dijkstraCounter = Long.MIN_VALUE;

}
