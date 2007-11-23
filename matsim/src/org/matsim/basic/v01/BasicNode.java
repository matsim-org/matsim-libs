/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNode.java
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

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Coord;

public class BasicNode extends BasicIdentified implements BasicNodeI{
	protected final BasicLinkSetI inlinks  = new BasicLinkSet();
	protected final BasicLinkSetI outlinks = new BasicLinkSet();

	// TODO [balmermi]: Since the basic link is a location, it MUST have
	// defined some geographical information (coords). These are defined
	// by its from- and to-node. Therefore, the BasicNode MUST have a coordinate
	// defined. See also BasicLink. If this is not O.K., then the BasicLink must
	// not extend Location.
	protected final CoordI coord;

	public BasicNode(String str, CoordI coord) {
		super(new Id(str));
		if (coord == null) { Gbl.errorMsg("Coord must be defined!"); }
		this.coord = coord;
	}

	// TODO [balmermi] see above why ...
	@Deprecated
	public BasicNode(String str) {
		super(new Id(str));
		this.coord = new Coord(0,0);
	}

	public boolean addInLink(BasicLinkI link) {
		return inlinks.add(link);
	}


	public boolean addOutLink(BasicLinkI link) {
		return outlinks.add(link);
	}


	public BasicLinkSetI getInLinks() {
		return inlinks;
	}


	public BasicLinkSetI getOutLinks() {
		return outlinks;
	}

	// TODO [balmermi] see above why ...
	public CoordI getCoord() {
		return this.coord;
	}
}
