/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.networkCreator;

import org.matsim.api.core.v01.Coord;
import playground.boescpa.lib.tools.coordUtils.CoordFilter;

/**
 * Provides different OsmFilters for the OsmReader.
 * The hierarchy of the layers starts with 1 as the top layer.
 *
 * @author boescpa
 */
public abstract class OsmFilter {

	protected int hierarchy = Integer.MAX_VALUE;
	private final CoordFilter coordFilter;

	protected OsmFilter(CoordFilter coordFilter) {
		this.coordFilter = coordFilter;
	}

	public void setHierarchy(final int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public boolean coordInArea(final Coord coord, final int hierarchyLevel) {
		return this.hierarchy >= hierarchyLevel && coordFilter.coordCheck(coord);
	}

	// ******************* A few default implementations... *******************

	/**
	 * Returns true for all coordinates what equates to "if the hierarchy layer is ok, take the link..."
	 */
	public static class OsmFilterTakeAll extends OsmFilter {
		public OsmFilterTakeAll(int hierarchy) {
			super(new CoordFilter.CoordFilterTakeAll());
			this.setHierarchy(hierarchy);
		}
	}

	/**
	 * Filters according to the rectangle defined by coordNW and coordSE.
	 * This is the default implementation defined by mrieser.
	 */
	public static class OsmFilterRectangle extends OsmFilter {
		public OsmFilterRectangle(final Coord coordNW, final Coord coordSE) {
			super(new CoordFilter.CoordFilterRectangle(coordNW, coordSE));
		}

		public OsmFilterRectangle(final Coord coordNW, final Coord coordSE, final int hierarchy) {
			this(coordNW, coordSE);
			this.setHierarchy(hierarchy);
		}
	}

	public static class OSMFilterCircle extends OsmFilter {
		public OSMFilterCircle(final Coord center, final double radius) {
			super(new CoordFilter.CoordFilterCircle(center, radius));
		}

		public OSMFilterCircle(final Coord center, final double radius, final int hierarchy) {
			this(center, radius);
			this.setHierarchy(hierarchy);
		}
	}

	/**
	 * Filters the area defined by an ellipse set by the four values
	 * 	Coord center,
	 * 	double radiusA,
	 * 	double radiusB,
	 * 	double anglePhi.
	 */
	public static class OsmFilterEllipse extends OsmFilter {

		/**
		 * @param center
		 * @param radiusA	in coordinate units
		 * @param radiusB	in coordinate units
		 * @param anglePhi	in radians
		 */
		public OsmFilterEllipse(final Coord center, final double radiusA, final double radiusB, final double anglePhi) {
			super(new CoordFilter.CoordFilterEllipse(center, radiusA, radiusB, anglePhi));
		}

		/**
		 * @param center
		 * @param radiusA	in coordinate units
		 * @param radiusB	in coordinate units
		 * @param anglePhi	in radians
		 * @param hierarchy
		 */
		public OsmFilterEllipse(final Coord center, final double radiusA, final double radiusB, final double anglePhi, final int hierarchy) {
			this(center, radiusA, radiusB, anglePhi);
			this.setHierarchy(hierarchy);
		}
	}

	/**
	 * Filters the area defined by a shp-file.
	 * Relies on Dobler's CoordAnalyzer.
	 */
	public static class OsmFilterShp extends OsmFilter {
		public OsmFilterShp(final String pathToShpFile) {
			super(new CoordFilter.CoordFilterShp(pathToShpFile));
		}

		public OsmFilterShp(final String pathToShpFile, final int hierarchy) {
			this(pathToShpFile);
			this.setHierarchy(hierarchy);
		}
	}
}
