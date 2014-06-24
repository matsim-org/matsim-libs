/* *********************************************************************** *
 * project: org.matsim.*
 * GridElevationProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.elevation;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

/**
 * @author thibautd
 */
public class GridElevationProvider implements ElevationProvider<Coord> {
	private final double[][] grid;
	private final Coord lowerLeftCorner;
	private final double cellSize;

	private CoordinateTransformation facilityToGridCoordTransformation =
		new IdentityTransformation();

	public GridElevationProvider(
			final int width,
			final int height,
			final Coord lowerLeftCorner,
			final double cellSize) {
		this.grid = new double[width][height];
		this.lowerLeftCorner = lowerLeftCorner;
		this.cellSize = cellSize;
	}

	public void setFacilityToGridCoordTransformation(
			final CoordinateTransformation facilityToGridCoordTransformation) {
		this.facilityToGridCoordTransformation = facilityToGridCoordTransformation;
	}

	public void putAltitude(final int x, final int y, final double alt) {
		this.grid[ x ][ y ] = alt;
	}

	public int getNRows() {
		return grid[ 0 ].length;
	}

	public int getNCols() {
		return grid.length;
	}

	@Override
	public double getAltitude(final Coord coord) {
		final Coord coordInGridSystem = facilityToGridCoordTransformation.transform( coord );

		final int xOffset = (int) ( ( coordInGridSystem.getX() - lowerLeftCorner.getX() ) / cellSize );
		final int yOffset = (int) ( ( coordInGridSystem.getY() - lowerLeftCorner.getY() ) / cellSize );

		// TODO handle cells with no value
		if ( xOffset < 0 || xOffset >= grid.length ) return Double.NaN;
		if ( yOffset < 0 || yOffset >= grid[ xOffset ].length ) return Double.NaN;
		return grid[ xOffset ][ yOffset ];
	}
}

