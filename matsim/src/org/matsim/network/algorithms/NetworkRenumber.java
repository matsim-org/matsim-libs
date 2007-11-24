/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkRenumber.java
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

package org.matsim.network.algorithms;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 * Renumbers the links and nodes in a network.<br/>
 * The C-Version of matsim may support only 32bit-Numbers, but in some cases
 * the ids in networks are larger. This Algorithm just renumbers all links
 * and nodes, beginning at 1. The original ids are stored in the "origid"-
 * attributes. Older origids are overwritten.<br/>
 * If the restore-option is set, the numbers from the origid-attribute are
 * set as node- and link-ids, and the origid-attributes are removed.
 */
public class NetworkRenumber extends NetworkAlgorithm {

	// predefined values for use in constructor
	public static final boolean RENUMBER_IDS = false;
	public static final boolean RESTORE_IDS = true;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private boolean restore;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkRenumber() {
		this(false);
	}

	public NetworkRenumber(final boolean restore) {
		super();
		this.restore = restore;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final NetworkLayer network) {
		// renumber links and nodes if requested
		if (this.restore) {
			// restore
			for (Node node : network.getNodes().values()) {
				node.setId(new Id(node.getOrigId()));
				node.setOrigId(null);
			}

			for (Link link : network.getLinks().values()) {
				link.setId(new Id(link.getOrigId()));
				link.setOrigId(null);
			}
		} 	else {
			// renumber
			int id = 1;
			for (Node node : network.getNodes().values()) {
				node.setOrigId(node.getId().toString());
				node.setId(new Id(id));
				id++;
			}

			id = 1;
			for (Link link : network.getLinks().values()) {
				link.setOrigId(link.getId().toString());
				link.setId(new Id(id));
				id++;
			}
		}
	}
}
