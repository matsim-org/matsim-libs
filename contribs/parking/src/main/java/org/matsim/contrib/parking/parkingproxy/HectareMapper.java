/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

import org.matsim.api.core.v01.Coord;

/**
 * <p>
 * Class that sorts coordinates into a grid and maps the gridcells to a key.
 * </p>
 * <p>
 * The key is composed in such a way that the x-number (Integer) of the gridcell (starting at
 * 0,0) is stored in the first half of the bits of the Long-key and the y-number (Integer) in
 * the second half. This happens in binary, i.e. you can not "by eye" reconstruct the original
 * coordinate from looking at the key.
 * </p>
 * <p>
 * <b>Important note:</b> This class works with int and long values and is therefore best used
 * with coordinate systems based on a metric grid. It will especially not work with WGS84!
 * </p>
 * 
 * @author tkohl / Senozon after a concept by mrieser (then also Senozon)
 *
 */
class HectareMapper {
	private final int gridsize;
	
	/**
	 * Sets up the mapping grid.
	 * 
	 * @param gridsize the x- and y-dimension of one gridcell (in terms of unitless coordinates)
	 */
	public HectareMapper(int gridsize) {
		this.gridsize = gridsize;
	}
	
	/**
	 * Returns the hectare key of the gridcell containing the given coordinate.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public long getKey(final double x, final double y) {
		long xCell = ((long)x / this.gridsize);
		long yCell = ((long)y / this.gridsize);
		long key = ((yCell & 0x0000_00000_FFFF_FFFFL) << 32) | (xCell & 0x0000_00000_FFFF_FFFFL);
		return key;
	}
	
	/**
	 * Returns the hectare key of the gridcell containing the given coordinates.
	 * 
	 * @param coord
	 * @return
	 */
	public long getKey(final Coord coord) {
		return getKey(coord.getX(), coord.getY());
	}

	/**
	 * Returns the center coordinate of the hectare corresponding to the given hectare key.
	 * 
	 * @param key
	 * @return
	 */
	public Coord getCenter(long key) {
		int xCell = (int) (key & 0x0000_0000_FFFF_FFFFL);
		int yCell = (int) ((key >> 32) & 0x0000_0000_FFFF_FFFFL);
		return new Coord(xCell * this.gridsize + this.gridsize/2, yCell * this.gridsize + this.gridsize/2);
	}
}
