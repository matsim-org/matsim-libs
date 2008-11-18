/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.roadpricing;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;


/**
 * @author dgrether
 *
 */
public class EditableNetworkLayer extends NetworkLayer {

	public void addLink(Link l) {
		if (this.locations.containsKey(l.getId()))
			throw new IllegalArgumentException("link already exists");
		else {
			Node from = this.nodes.get(l.getFromNode().getId());
			Node to = this.nodes.get(l.getToNode().getId());
			if (from == null) {
				this.nodes.put(l.getFromNode().getId(), l.getFromNode());
			}
			if (to == null) {
				this.nodes.put(l.getToNode().getId(), l.getToNode());
			}
			this.locations.put(l.getId(), l);
		}
	}
}
