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

import org.matsim.api.core.v01.Coord;

public abstract class CoordUtils {
	
	public static Coord createCoord( final double xx, final double yy ) {
		return new Coord(xx, yy);
	}
	
	public static Coord plus ( Coord coord1, Coord coord2 ) {
		double xx = coord1.getX() + coord2.getX();
		double yy = coord1.getY() + coord2.getY();
		return new Coord(xx, yy);
	}
	
	public static Coord minus ( Coord coord1, Coord coord2 ) {
		double xx = coord1.getX() - coord2.getX();
		double yy = coord1.getY() - coord2.getY();
		return new Coord(xx, yy);
	}
	
	public static Coord scalarMult( double alpha, Coord coord ) {
		double xx = alpha * coord.getX() ;
		double yy = alpha * coord.getY() ;
		return new Coord(xx, yy);
	}
	
	public static Coord getCenter( Coord coord1, Coord coord2 ) {
		double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
		double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
		return new Coord(xx, yy);
	}
	
	public static double length( Coord coord1 ) {
		return Math.sqrt( coord1.getX()*coord1.getX() + coord1.getY()*coord1.getY() ) ;
	}
	
	public static Coord rotateToRight( Coord coord1 ) {
		final double y = -coord1.getX();
		return new Coord(coord1.getY(), y);
	}
	
	public static Coord getCenterWOffset( Coord coord1, Coord coord2 ) {
		Coord fromTo = minus( coord2, coord1 ) ;
		Coord offset = scalarMult( 0.1 , rotateToRight( fromTo ) ) ;
		Coord centerWOffset = plus( getCenter( coord1, coord2 ) , offset ) ;
		return centerWOffset ;
	}

	public static double calcEuclideanDistance(Coord coord, Coord other) {
		//depending on the coordinate system that is used, determining the
		//distance based on the euclidean distance will lead to wrong results.
		//however, if the distance is not to large (<1km) this will be a usable distance estimation.
		//Another comfortable way to calculate correct distances would be, to use the distance functions
		//provided by geotools lib. May be we need to discuss what part of GIS functionality we should implement
		//by our own and for what part we could use an existing GIS like geotools. We need to discuss this in terms
		//of code robustness, performance and so on ... [gl]
		double xDiff = other.getX()-coord.getX();
		double yDiff = other.getY()-coord.getY();
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}

	/**
	 * Calculates the shortest distance of a point to a line segment. The line segment
	 * is given by two points, <code>lineFrom</code> and <code>lineTo</code>. Note that
	 * the line segment has finite length, and thus the shortest distance cannot
	 * always be the distance on the tangent to the line through <code>point</code>.
	 *
	 * @param lineFrom The start point of the line segment
	 * @param lineTo The end point of the line segment
	 * @param point The point whose distance to the line segment should be calculated
	 * @return the distance of <code>point</code> to the line segment given by the two
	 *    end points of the line segment, <code>lineFrom</code> and <code>lineTo</code>
	 *
	 * @author mrieser
	 */
	public static double distancePointLinesegment(final Coord lineFrom, final Coord lineTo, final Coord point) {
		/* The shortest distance is where the tangent of the line goes through "point".
		 * The dot product (point - P) dot (lineTo - lineFrom) must be 0, when P is a point
		 * on the line. P can be substituted with lineFrom + u*(lineTo - lineFrom).
		 * Thus it must be:
		 *    (point - lineFrom - u*(lineTo - lineFrom)) dot (lineTo - lineFrom) == 0
		 * From this follows:
		 *        (point.x - lineFrom.x)(lineTo.x - lineFrom.x) + (point.y - lineFrom.y)(lineTo.y - lineFrom.y)
		 *    u = ---------------------------------------------------------------------------------------------
		 *       (lineTo.x - lineFrom.x)(lineTo.x - lineFrom.x) + (lineTo.y - lineFrom.y)(lineTo.y - lineFrom.y)
		 *
		 * Substituting this gives:
		 *   x = lineFrom.x + u*(lineFrom.x - lineTo.x) , y = lineFrom.y + u*(lineFrom.y - lineTo.y)
		 *
		 * The shortest distance is now the distance between "point" and (x | y)
		 *
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
	}

}
