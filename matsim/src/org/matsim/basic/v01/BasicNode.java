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

import java.util.HashMap;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

public class BasicNode implements BasicNodeI {
	protected final Map<IdI, BasicLinkI> inlinks  = new HashMap<IdI, BasicLinkI>(4, 0.95f);
	protected final Map<IdI, BasicLinkI> outlinks = new HashMap<IdI, BasicLinkI>(4, 0.95f);

	/* TODO [balmermi]: Since the basic link is a location, it MUST have
	 * defined some geographical information (coords). These are defined
	 * by its from- and to-node. Therefore, the BasicNode MUST have a coordinate
	 * defined. See also BasicLink. If this is not O.K., then the BasicLink must
	 * not extend Location. */
	protected final CoordI coord;
	protected IdI id;

	public BasicNode(IdI id, CoordI coord) {
		this.id = id;
		if (coord == null) { Gbl.errorMsg("Coord must be defined!"); }
		this.coord = coord;
	}

	public boolean addInLink(BasicLinkI link) {
		this.inlinks.put(link.getId(), link);
		return true;
	}


	public boolean addOutLink(BasicLinkI link) {
		this.outlinks.put(link.getId(), link);
		return true;
	}


	public Map<IdI, ? extends BasicLinkI> getInLinks() {
		return this.inlinks;
	}


	public Map<IdI, ? extends BasicLinkI> getOutLinks() {
		return this.outlinks;
	}

	// TODO [balmermi] see above why ...
	public CoordI getCoord() {
		return this.coord;
	}


	public IdI getId() {
		return this.id;
	}

	public void setId(final IdI id) {
		this.id = id;
	}
}
