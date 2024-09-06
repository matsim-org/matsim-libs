/* *********************************************************************** *
 * project: org.matsim.*
 * CoordUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.geometry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;

public abstract class CoordUtils {
	final private static Logger LOG = LogManager.getLogger(CoordUtils.class);
	final private static EucledianDistanceCalculator eucledianDistanceCalculator = new EucledianDistanceCalculator();

	public static Coordinate createGeotoolsCoordinate( final Coord coord ) {
		return new Coordinate( coord.getX(), coord.getY() ) ;
	}

	public static Coord createCoord( final Coordinate coordinate ) {
		return new Coord( coordinate.x, coordinate.y ) ;
	}

	public static Coord createCoord( final double xx, final double yy ) {
		return new Coord(xx, yy);
	}

	public static Coord createCoord( final double xx, final double yy, final double zz){
		return new Coord(xx, yy, zz);
	}

	public static Coord plus ( Coord coord1, Coord coord2 ) {
		if( !coord1.hasZ() && !coord2.hasZ() ){
			/* Both are 2D coordinates. */
			double xx = coord1.getX() + coord2.getX();
			double yy = coord1.getY() + coord2.getY();
			return new Coord(xx, yy);
		} else if( coord1.hasZ() && coord2.hasZ() ){
			/* Both are 3D coordinates. */
			double xx = coord1.getX() + coord2.getX();
			double yy = coord1.getY() + coord2.getY();
			double zz = coord1.getZ() + coord2.getZ();
			return new Coord(xx, yy, zz);
		} else{
			throw new RuntimeException("Cannot 'plus' coordinates if one has elevation (z) and the other not; coord1.hasZ=" + coord1.hasZ()
			+ "; coord2.hasZ=" + coord2.hasZ() );
		}
	}

	public static Coord minus ( Coord coord1, Coord coord2 ) {
		if( !coord1.hasZ() && !coord2.hasZ() ){
			/* Both are 2D coordinates. */
			double xx = coord1.getX() - coord2.getX();
			double yy = coord1.getY() - coord2.getY();
			return new Coord(xx, yy);
		} else if( coord1.hasZ() && coord2.hasZ() ){
			/* Both are 3D coordinates. */
			double xx = coord1.getX() - coord2.getX();
			double yy = coord1.getY() - coord2.getY();
			double zz = coord1.getZ() - coord2.getZ();
			return new Coord(xx, yy, zz);
		} else{
			throw new RuntimeException("Cannot 'minus' coordinates if one has elevation (z) and the other not.");
		}
	}

	public static Coord scalarMult( double alpha, Coord coord ) {
		if(!coord.hasZ()){
			/* 2D coordinate. */
			double xx = alpha * coord.getX();
			double yy = alpha * coord.getY();
			return new Coord(xx, yy);
		} else {
			/* 3D coordinate. */
			double xx = alpha * coord.getX();
			double yy = alpha * coord.getY();
			double zz = alpha * coord.getZ();
			return new Coord(xx, yy, zz);
		}
	}


	public static Coord getCenter( Coord coord1, Coord coord2 ) {
		if( !coord1.hasZ() && !coord2.hasZ() ){
			/* Both are 2D coordinates. */
			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
			return new Coord(xx, yy);
		} else if( coord1.hasZ() && coord2.hasZ() ){
			/* Both are 3D coordinates. */
			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
			double zz = 0.5*( coord1.getZ() + coord2.getZ() ) ;
			return new Coord(xx, yy, zz);
		} else{
			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
		}
	}

	public static double length( Coord coord ) {
		if(!coord.hasZ()){
			return Math.sqrt(
					coord.getX()*coord.getX() +
					coord.getY()*coord.getY() ) ;
		} else{
			return Math.sqrt(
					coord.getX()*coord.getX() +
					coord.getY()*coord.getY() +
					coord.getZ()*coord.getZ()) ;
		}
	}

	/**
	 * Note: If the given {@link Coord} has elevation, it's elevation will stay
	 * the same (jjoubert, Sep '16).
	 * @param coord
	 * @return
	 */
	public static Coord rotateToRight( Coord coord ) {
		if( !coord.hasZ() ){
			/* 2D coordinate */
			final double y = -coord.getX();
			return new Coord(coord.getY(), y);
		} else{
			/* 3D coordinate */
			final double y = -coord.getX();
			return new Coord(coord.getY(), y, coord.getZ());
		}
	}


	public static Coord getCenterWOffset( Coord coord1, Coord coord2 ) {
		if( !coord1.hasZ() && !coord2.hasZ() ){
			/* Both are 2D coordinates. */
			Coord fromTo = minus( coord2, coord1 ) ;
			Coord offset = scalarMult( 0.1 , rotateToRight( fromTo ) ) ;
			Coord centerWOffset = plus( getCenter( coord1, coord2 ) , offset ) ;
			return centerWOffset ;
		} else if( coord1.hasZ() && coord2.hasZ() ){
			/* TODO Both are 3D coordinates. */
			throw new RuntimeException("3D version not implemented.");
		} else{
			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
		}
	}

	/**
	 * Round coordinate by using default precision. For coordinates that are assumed lat lon, it will be 6 decimals.
	 * Otherwise, UTM is assumed and rounded to 2 decimals places.
	 * When these assumptions hold, the resulting coordinate will be precise up to ~0.1 meter.
	 */
	public static Coord round(Coord coord) {
		int scale = Math.abs(coord.getX()) > 180 || Math.abs(coord.getY()) > 180 ? 2 : 6;
		return round(coord, scale);
	}

	/**
	 * Round coordinate with a fixed scale.
	 */
	public static Coord round(Coord coord, int scale) {
		if (coord.hasZ()) {
			return new Coord(roundNumber(coord.getX(), scale), roundNumber(coord.getY(), scale), coord.getZ());
		}

		return new Coord(roundNumber(coord.getX(), scale), roundNumber(coord.getY(), scale));
	}

	private static double roundNumber(double x, int scale) {
		return BigDecimal.valueOf(x).setScale(scale, RoundingMode.HALF_EVEN).doubleValue();
	}

	public static double calcEuclideanDistance(Coord coord, Coord other) {
		return eucledianDistanceCalculator.calculateDistance(coord, other);
	}

	/**
	 * Method to deal with distance calculation when only the x and y-components
	 * of the coordinates are used. The elevation (z component) is ignored,
	 * whether it is available or not.
	 * (xy-plane)
	 * @param coord
	 * @param other
	 * @return
	 */
	public static double calcProjectedEuclideanDistance(Coord coord, Coord other) {
		double xDiff = other.getX()-coord.getX();
		double yDiff = other.getY()-coord.getY();
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}


	/**
	 * Method should only be used in within this class, and only by
	 * {@link #distancePointLinesegment(Coord, Coord, Coord)}.
	 * @param coord1
	 * @param coord2
	 * @return
	 */
	private static double dotProduct( Coord coord1, Coord coord2 ) {
		if( !coord1.hasZ() && !coord2.hasZ() ){
			/* Both are 2D coordinates. */
			return 	coord1.getX()*coord2.getX() +
					coord1.getY()*coord2.getY();
		} else if( coord1.hasZ() && coord2.hasZ() ){
			/* Both are 3D coordinates. */
			return 	coord1.getX()*coord2.getX() +
					coord1.getY()*coord2.getY() +
					coord1.getZ()*coord2.getZ();
		} else{
			throw new RuntimeException("Cannot get the dot-product of coordinates if one has elevation (z) and the other not.");
		}
	}


	/**
	 * Calculates the shortest distance of a point to a line segment. The line segment
	 * is given by two points, <code>lineFrom</code> and <code>lineTo</code>. Note that
	 * the line segment has finite length, and thus the shortest distance cannot
	 * always be the distance on the tangent to the line through <code>point</code>.
	 *
	 * <br><br>
	 * The 3D version was adapted from the C++ implementation of
	 * <a href="http://geomalgorithms.com/a02-_lines.html">Dan Sunday</a>.
	 *
	 * @param lineFrom The start point of the line segment
	 * @param lineTo The end point of the line segment
	 * @param point The point whose distance to the line segment should be calculated
	 * @return the distance of <code>point</code> to the line segment given by the two
	 *    end points of the line segment, <code>lineFrom</code> and <code>lineTo</code>
	 *
	 * @author mrieser, jwjoubert
	 */
	private static boolean onlyOnceWarnGiven = false;

	public static double distancePointLinesegment(final Coord lineFrom, final Coord lineTo, final Coord point) {
		if( !lineFrom.hasZ() && !lineTo.hasZ() && !point.hasZ() ){
			/* All coordinates are 2D and in the XY plane. */

			/* The shortest distance is where the tangent of the line goes
			 * through "point". The dot product (point - P) dot (lineTo - lineFrom)
			 * must be 0, when P is a point on the line. P can be substituted
			 * with lineFrom + u*(lineTo - lineFrom). Thus it must be:
			 *    (point - lineFrom - u*(lineTo - lineFrom)) dot (lineTo - lineFrom) == 0
			 * From this follows:
			 *        (point.x - lineFrom.x)(lineTo.x - lineFrom.x) + (point.y - lineFrom.y)(lineTo.y - lineFrom.y)
			 *    u = ---------------------------------------------------------------------------------------------
			 *       (lineTo.x - lineFrom.x)(lineTo.x - lineFrom.x) + (lineTo.y - lineFrom.y)(lineTo.y - lineFrom.y)
			 *
			 * Substituting this gives:
			 *   x = lineFrom.x + u*(lineFrom.x - lineTo.x) , y = lineFrom.y + u*(lineFrom.y - lineTo.y)
			 *
			 * The shortest distance is now the distance between "point" and
			 * (x | y)
			 */
			double lineDX = lineTo.getX() - lineFrom.getX();
			double lineDY = lineTo.getY() - lineFrom.getY();

			if ((lineDX == 0.0) && (lineDY == 0.0)) {
				// the line segment is a point without dimension
				return calcEuclideanDistance(lineFrom, point);
			}

			double u = ((point.getX() - lineFrom.getX())*lineDX + (point.getY() - lineFrom.getY())*lineDY) /
					(lineDX*lineDX + lineDY*lineDY);

			if (u <= 0) {
				// (x | y) is not on the line segment, but before lineFrom
				return calcEuclideanDistance(lineFrom, point);
			}
			if (u >= 1) {
				// (x | y) is not on the line segment, but after lineTo
				return calcEuclideanDistance(lineTo, point);
			}

			return calcEuclideanDistance(new Coord(lineFrom.getX() + u * lineDX, lineFrom.getY() + u * lineDY), point);
		} else if( lineFrom.hasZ() && lineTo.hasZ() && point.hasZ() ){
			/* All coordinates are 3D. */
			double lineDX = lineTo.getX() - lineFrom.getX();
			double lineDY = lineTo.getY() - lineFrom.getY();
			double lineDZ = lineTo.getZ() - lineFrom.getZ();

			if((lineDX == 0.0) && (lineDY == 0.0) && (lineDZ == 0.0)){
				return calcEuclideanDistance(lineFrom, point);
			}

			Coord v = minus(lineTo, lineFrom);
			Coord w = minus(point, lineFrom);

			double c1 = dotProduct(w, v);
			if(c1 <= 0.0){
				Coord m = minus(point, lineFrom);
				return Math.sqrt(dotProduct(m, m));
			}

			double c2 = dotProduct(v, v);
			if(c2 <= c1){
				Coord m = minus(point, lineTo);
				return Math.sqrt(dotProduct(m, m));
			}

			double b = c1 / c2;
			Coord p = plus(lineFrom, scalarMult(b, v));
			Coord m = minus(point, p);
			return Math.sqrt(dotProduct(m, m));
		} else{
			if (!onlyOnceWarnGiven) {
				LogManager.getLogger(CoordUtils.class).warn("Mix of 2D / 3D coordinates. Assuming 2D only.\n" + Gbl.ONLYONCE);
				onlyOnceWarnGiven = true;
			}
			return distancePointLinesegment(new Coord(lineFrom.getX(), lineFrom.getY()), new Coord(lineTo.getX(), lineTo.getY()), new Coord(point.getX(), point.getY()));
		}
	}


	/**
	 * Calculates the coordinate of the intersection point of the orthogonal projection
	 * of a given point on a line segment with that line segment. The line segment
	 * is given by two points, <code>lineFrom</code> and <code>lineTo</code>. If the
	 * projection point does not lie *on* the line segment (but only somewhere on
	 * the extension of the line segment, i.e. the infinite line), the end point of
	 * the line segment which is closest to the given point is returned.
	 *
	 * <br><br>
	 * The 3D version was adapted from the documentation of
	 * <a href="http://www.geometrictools.com/Documentation/DistancePointLine.pdf">
	 * David Eberly/a>.
	 *
	 * @param lineFrom The start point of the line segment
	 * @param lineTo The end point of the line segment
	 * @param point The point whose distance to the line segment should be calculated
	 * @return the <code>coordinate</code> of the intersection point of the orthogonal
	 * projection of a given point on a line segment with that line segment
	 *
	 * @author dziemke, jwjoubert
	 */
	public static Coord orthogonalProjectionOnLineSegment(final Coord lineFrom, final Coord lineTo, final Coord point) {
		if( !lineFrom.hasZ() && !lineTo.hasZ() && !point.hasZ() ){
			/* All coordinates are 2D. */

			/* Concerning explanation and usage of the dot product for these calculation, please
			 * read comments of "distancePointLinesegment".
			 */
			double lineDX = lineTo.getX() - lineFrom.getX();
			double lineDY = lineTo.getY() - lineFrom.getY();

			if ((lineDX == 0.0) && (lineDY == 0.0)) {
				// the line segment is a point without dimension
				return lineFrom;
			}

			double u = ((point.getX() - lineFrom.getX())*lineDX + (point.getY() - lineFrom.getY())*lineDY) /
					(lineDX*lineDX + lineDY*lineDY);

			if (u <= 0) {
				// (x | y) is not on the line segment, but before lineFrom
				return lineFrom;
			}
			if (u >= 1) {
				// (x | y) is not on the line segment, but after lineTo
				return lineTo;
			}
			return new Coord(lineFrom.getX() + u * lineDX, lineFrom.getY() + u * lineDY);
		} else if(lineFrom.hasZ() && lineTo.hasZ() && point.hasZ() ){
			/* All coordinates are 3D. */
			Coord direction = minus(lineTo, lineFrom);

			double t0 = dotProduct(direction, minus(point, lineFrom)) / dotProduct(direction, direction);
			Coord q = plus(lineFrom, scalarMult(t0, direction));
			return q;
		} else{
			if (!onlyOnceWarnGiven) {
				LogManager.getLogger(CoordUtils.class).warn("Mix of 2D / 3D coordinates. Assuming 2D only.\n" + Gbl.ONLYONCE);
				onlyOnceWarnGiven = true;
			}
			return orthogonalProjectionOnLineSegment(new Coord(lineFrom.getX(), lineFrom.getY()), new Coord(lineTo.getX(), lineTo.getY()), new Coord(point.getX(), point.getY()));
			//throw new RuntimeException("All given coordinates must either be 2D, or 3D. A mix is not allowed.");
		}
	}

	private static class EucledianDistanceCalculator {


		private static final int maxWarnCount = 10;
		private final AtomicLong warnCounter = new AtomicLong(0);

		private double calculateDistance(Coord coord, Coord other) {
			/* Depending on the coordinate system that is used, determining the
			 * distance based on the euclidean distance will lead to wrong results.
			 * However, if the distance is not to large (<1km) this will be a usable
			 * distance estimation. Another comfortable way to calculate correct
			 * distances would be, to use the distance functions provided by
			 * geotools lib. May be we need to discuss what part of GIS functionality
			 * we should implement by our own and for what part we could use an
			 * existing GIS like geotools. We need to discuss this in terms of code
			 * robustness, performance and so on ... [gl] */
			if( !coord.hasZ() && !other.hasZ() ){
				/* Both are 2D coordinates. */
				double xDiff = other.getX()-coord.getX();
				double yDiff = other.getY()-coord.getY();
				return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
			} else if( coord.hasZ() && other.hasZ() ){
				/* Both are 3D coordinates. */
				double xDiff = other.getX()-coord.getX();
				double yDiff = other.getY()-coord.getY();
				double zDiff = other.getZ()-coord.getZ();
				return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff) + (zDiff*zDiff));
			} else{
				// there used to be a warning here, but it would clutter our log file
				// hence we silently calculate the distance on a 2D pane now. janek mai'21
				if (warnCounter.incrementAndGet() <= maxWarnCount) {
					LOG.warn("Mixed use of elevation in coordinates: " + coord +
							"; " + other);
					LOG.warn("Returning projected coordinate distance (using x and y components only)");

					if (warnCounter.get() == maxWarnCount) {
						LOG.warn("Future occurences of this logging statement are suppressed.");
					}
				}

				return calcProjectedEuclideanDistance(coord, other);
			}
		}

	}
}
