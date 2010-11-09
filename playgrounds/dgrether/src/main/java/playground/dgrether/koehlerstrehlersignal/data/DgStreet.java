/* *********************************************************************** *
 * project: org.matsim.*
 * DgStreet
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.data;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgStreet {

	private DgCrossingNode toNode;
	private DgCrossingNode fromNode;
	private Id id;

	public DgStreet(Id id, DgCrossingNode fromNode, DgCrossingNode toNode) {
		this.toNode = toNode;
		this.fromNode = fromNode;
		this.id = id;
	}

	public DgCrossingNode getToNode(){
		return toNode;
	}
	
	public DgCrossingNode getFromNode() {
		return fromNode;
	}

	public Id getId() {
		return this.id;
	}

}
