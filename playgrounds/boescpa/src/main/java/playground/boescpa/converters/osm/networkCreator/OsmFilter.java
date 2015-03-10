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

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides different OsmFilters for the OsmReader.
 * The hierarchy of the layers starts with 1 as the top layer.
 *
 * @author boescpa
 */
public abstract class OsmFilter {

	protected final int hierarchy;

	public OsmFilter(final int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public boolean coordInArea(final Coord coord, final int hierarchyLevel) {
		return this.hierarchy >= hierarchyLevel && coordCheck(coord);
	}

	/**
	 * @param coord
	 * @return true if coord is within the specified area, false else.
	 */
	protected abstract boolean coordCheck(Coord coord);

	// ******************* A few default implementations... *******************

	/**
	 * Returns true for all coordinates what equates to "if the hierarchy layer is ok, take the link..."
	 */
	public static class OsmFilterTakeAll extends OsmFilter {

		public OsmFilterTakeAll(int hierarchy) {
			super(hierarchy);
		}

		@Override
		protected boolean coordCheck(Coord coord) {
			return true;
		}
	}

	/**
	 * Filters according to the rectangle defined by coordNW and coordSE.
	 * This is the default implementation defined by mrieser.
	 */
	public static class OsmFilterRectangle extends OsmFilter {
		private final Coord coordNW;
		private final Coord coordSE;

		public OsmFilterRectangle(final Coord coordNW, final Coord coordSE, final int hierarchy) {
			super(hierarchy);
			this.coordNW = coordNW;
			this.coordSE = coordSE;
		}

		@Override
		protected boolean coordCheck(Coord coord) {
			return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE.getX()) &&
					(this.coordNW.getY() > coord.getY() && coord.getY() > this.coordSE.getY()));
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
		private final Coord center;
		private final double radiusASquared;
		private final double radiusBSquared;
		private final double cosPhi;
		private final double sinPhi;

		/**
		 * @param center
		 * @param radiusA	in coordinate units
		 * @param radiusB	in coordinate units
		 * @param anglePhi	in radians
		 * @param hierarchy
		 */
		public OsmFilterEllipse(final Coord center, final double radiusA, final double radiusB, final double anglePhi, final int hierarchy) {
			super(hierarchy);
			this.center = center;
			this.radiusASquared = radiusA*radiusA;
			this.radiusBSquared = radiusB*radiusB;
			this.cosPhi = Math.cos(anglePhi);
			this.sinPhi = Math.sin(anglePhi);
		}

		@Override
		protected boolean coordCheck(Coord coord) {
			// following http://stackoverflow.com/questions/7946187/point-and-ellipse-rotated-position-test-algorithm
			final double a = Math.pow(cosPhi*(coord.getX()-center.getX()) + sinPhi*(coord.getY()-center.getY()),2);
			final double b = Math.pow(sinPhi*(coord.getX()-center.getX()) - cosPhi*(coord.getY()-center.getY()),2);
			final double ellipsePosition = a/radiusASquared + b/radiusBSquared;
			return ellipsePosition <= 1;
		}
	}

	/**
	 * Filters the area defined by a shp-file.
	 * Relies on Dobler's CoordAnalyzer.
	 */
	public static class OsmFilterShp extends OsmFilter {
		private final CoordAnalyzer coordAnalyzer;

		public OsmFilterShp(final String pathToShpFile, final int hierarchy) {
			super(hierarchy);
			Set<SimpleFeature> features = new HashSet<>();
			SHPFileUtil util = new SHPFileUtil();
			features.addAll(ShapeFileReader.getAllFeatures(pathToShpFile));
			Geometry area = util.mergeGeometries(features);
			this.coordAnalyzer = new CoordAnalyzer(area);
		}

		@Override
		protected boolean coordCheck(Coord coord) {
			return coordAnalyzer.isCoordAffected(coord);
		}
	}
}
