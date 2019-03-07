/* *********************************************************************** *
 * project: org.matsim.*
 * OrthodromicDistanceCalculator.java
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
package org.matsim.contrib.common.gis;

import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Distance calculater that calculates the distance between two points in a coordinate reference system determined from
 * the points srid property.
 *
 * @author illenberger
 */
public class OrthodromicDistanceCalculator implements DistanceCalculator {

    private static OrthodromicDistanceCalculator instance;

    /**
     * Singleton access.
     *
     * @return a singleton instance
     */
    public static OrthodromicDistanceCalculator getInstance() {
        if (instance == null)
            instance = new OrthodromicDistanceCalculator();
        return instance;
    }

    /**
     * Calculates the distance between two points. If the SRID is 0, a cartesian coordinate system is assumed and {@link
     * CartesianDistanceCalculator} is used, otherwise {@link JTS#orthodromicDistance(Coordinate, Coordinate,
     * CoordinateReferenceSystem)} is used.
     *
     * @param p1 the source point
     * @param p2 the target point
     * @return the distance between <tt>p1</tt> and <tt>p2</tt> or {@code Double.NaN} if the distance cannot be
     * calculated.
     * @throws RuntimeException if points have different SRIDs
     */
    @Override
    public double distance(Point p1, Point p2) {
        if (p1.getSRID() == p2.getSRID()) {
            if (p1.getSRID() == 0) {
                return CartesianDistanceCalculator.getInstance().distance(p1, p2);
            } else {
                try {
                    return JTS.orthodromicDistance(p1.getCoordinate(), p2.getCoordinate(),
                            CRSUtils.getCRS(p1.getSRID()));
                } catch (TransformException e) {
                    e.printStackTrace();
                    return Double.NaN;
                }
            }
        } else {
            throw new RuntimeException("Incompatible coordinate reference systems.");
        }
    }

}
