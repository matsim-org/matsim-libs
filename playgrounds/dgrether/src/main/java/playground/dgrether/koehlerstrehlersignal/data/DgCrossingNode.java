/* *********************************************************************** *
 * project: org.matsim.*
 * DgNode
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgCrossingNode {

	private Id<DgCrossingNode> id;
	
	private Coord coordinate;

	public DgCrossingNode(Id<DgCrossingNode> id) {
		this.id = id;
	}

	public Id<DgCrossingNode> getId() {
		return this.id;
	}

	
	public Coord getCoordinate() {
		return coordinate;
	}

	
	public void setCoordinate(Coord coordinate) {
		this.coordinate = coordinate;
	}

}
