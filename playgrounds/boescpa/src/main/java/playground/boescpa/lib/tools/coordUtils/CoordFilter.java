/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.boescpa.lib.tools.coordUtils;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.boescpa.lib.tools.SHPFileUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides different filters to check if a coord is in a given area.
 *
 * @author boescpa
 */
public abstract class CoordFilter {

	/**
	 * @param coord
	 * @return true if coord is within the specified area, false else.
	 */
	public abstract boolean coordCheck(Coord coord);

	// ******************* A few default implementations... *******************

	/**
	 * Returns true for all coordinates what equates to "if the hierarchy layer is ok, take the link..."
	 */
	public static class CoordFilterTakeAll extends CoordFilter {

		@Override
		public boolean coordCheck(Coord coord) {
			return true;
		}
	}

	/**
	 * Filters according to the rectangle defined by coordNW and coordSE.
	 * This is the default implementation defined by mrieser.
	 */
	public static class CoordFilterRectangle extends CoordFilter {
		private final Coord coordNW;
		private final Coord coordSE;

		public CoordFilterRectangle(final Coord coordNW, final Coord coordSE) {
			this.coordNW = coordNW;
			this.coordSE = coordSE;
		}

		@Override
		public boolean coordCheck(Coord coord) {
			return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE.getX()) &&
					(this.coordNW.getY() > coord.getY() && coord.getY() > this.coordSE.getY()));
		}
	}

	public static class CoordFilterCircle extends CoordFilter {

		private final Coord center;
		private final double radius;

		public CoordFilterCircle(final Coord center, final double radius) {
			this.center = center;
			this.radius = radius;
		}

		@Override
		public boolean coordCheck(Coord coord) {
			return CoordUtils.calcEuclideanDistance(center, coord) < radius;
		}
	}

	/**
	 * Filters the area defined by an ellipse set by the four values
	 * 	Coord center,
	 * 	double radiusA,
	 * 	double radiusB,
	 * 	double anglePhi.
	 */
	public static class CoordFilterEllipse extends CoordFilter {
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
		 */
		public CoordFilterEllipse(final Coord center, final double radiusA, final double radiusB, final double anglePhi) {
			this.center = center;
			this.radiusASquared = radiusA*radiusA;
			this.radiusBSquared = radiusB*radiusB;
			this.cosPhi = Math.cos(anglePhi);
			this.sinPhi = Math.sin(anglePhi);
		}

		@Override
		public boolean coordCheck(Coord coord) {
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
	public static class CoordFilterShp extends CoordFilter {
		private final String pathToShpFile;
		private final CoordAnalyzer coordAnalyzer;

		public CoordFilterShp(final String pathToShpFile) {
			this.pathToShpFile = pathToShpFile;
			Set<SimpleFeature> features = new HashSet<>();
			SHPFileUtils util = new SHPFileUtils();
			features.addAll(ShapeFileReader.getAllFeatures(this.pathToShpFile));
			Geometry area = util.mergeGeometries(features);
			this.coordAnalyzer = new CoordAnalyzer(area);
		}

		@Override
		public boolean coordCheck(Coord coord) {
			return coordAnalyzer.isCoordAffected(coord);
		}
	}

}
