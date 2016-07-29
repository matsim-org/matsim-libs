/* *********************************************************************** *
 * project: org.matsim.*
 * Stop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs.lib;

import org.matsim.api.core.v01.Coord;

public class GTFSStop {

	private Coord point;
	private String name;
	private boolean blocks;

	//Methods
	public GTFSStop(Coord point, String name, boolean blocks) {
		super();
		this.point = point;
		this.name = name;
		this.blocks = blocks;
	}
	/**
	 * @return the point
	 */
	public Coord getPoint() {
		return point;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the blocks
	 */
	public boolean isBlocks() {
		return blocks;
	}

}
