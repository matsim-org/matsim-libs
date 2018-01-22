/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.corineLandcover;

import java.util.Collection;
import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.geotools.geometry.jts.JTSFactoryFinder;

/**
 * Created by amit on 10.10.17.
 */

public class GeometryUtils {

    private GeometryUtils(){}
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * @param geom
     * @return a simplified geometry by increasing tolerance until number of vertices are less than 1000.
     */
    public static Geometry getSimplifiedGeom(final Geometry geom){
        Geometry outGeom = geom;
        double distanceTolerance = 1;
        int numberOfVertices = getNumberOfVertices(geom);
        while (numberOfVertices > 300){
            outGeom = getSimplifiedGeom(outGeom, distanceTolerance);
            numberOfVertices = getNumberOfVertices(outGeom);
            distanceTolerance *= 10;
        }
        return outGeom;
    }

    public static int getNumberOfVertices(final Geometry geom){
        return geom.getNumPoints();
    }

    /**
     * simplify the geometry based on given tolerance
     */
    public static Geometry getSimplifiedGeom(final Geometry geom, final double distanceTolerance){
        return TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
    }

    /**
     * It perform "union" for each geometry and return one geometry.
     */
    public static Geometry combine(final List<Geometry> geoms){
        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory( null );

        // note the following geometry collection may be invalid (say with overlapping polygons)
        GeometryCollection geometryCollection =
                (GeometryCollection) factory.buildGeometry( geoms );
        return geometryCollection.union();
    }

    /**
     * @return a random point which is covered by all the geometries
     */
    public static Point getPointInteriorToGeometry(final Geometry landUseGeom, final Geometry zoneGeom) {
        if (landUseGeom.isEmpty() || zoneGeom.isEmpty() ) throw new RuntimeException("No geometries.");

        if (landUseGeom.intersection(zoneGeom).getArea()==0) {
            throw new RuntimeException("There is no common area for the given geoms.");
        }

        Point commonPoint = null;
        do {
            //assuming that zoneGeom is a subset of landUseGeom, it would be better to first find a point in a subset and then look if it's inside landUseGeom
            Coordinate coordinate = getRandomInteriorPoints(zoneGeom,1)[0];
            commonPoint = geometryFactory.createPoint(coordinate);
            if (landUseGeom.contains(commonPoint)) return commonPoint;
        } while(true);
    }

    /**
     * @return a random point which is covered by list of landUseGeom as well as zoneGeom
     */
    public static Point getPointInteriorToGeometries(final Collection<Geometry> landUseGeoms, final Geometry zoneGeom) {
        if (landUseGeoms.isEmpty() || zoneGeom.isEmpty() ) throw new RuntimeException("No geometries.");

        Point commonPoint = null;
        do {
            //assuming that zoneGeom is a subset of landUseGeom, it would be better to first find a point in a subset and then look if it's inside landUseGeom
        	//this can create infinite loops
            Coordinate coordinate = getRandomInteriorPoints(zoneGeom,1)[0];
            commonPoint = geometryFactory.createPoint(coordinate);
            if (isPointInsideGeometries(landUseGeoms,commonPoint)) return commonPoint;
           
        } while(true);
    }
    
    /**
     * @return a random point which is covered by list of landUseGeom as well as zoneGeom
     */
    public static Point getPointInteriorToGeometriesWithFallback(final Collection<Geometry> landUseGeoms, final Geometry zoneGeom) {
        if (landUseGeoms.isEmpty() || zoneGeom.isEmpty() ) throw new RuntimeException("No geometries.");

        Point commonPoint = null;
        int counter = 0;
        do {
            //assuming that zoneGeom is a subset of landUseGeom, it would be better to first find a point in a subset and then look if it's inside landUseGeom
        	//this can create infinite loops, I'm changing it to fallback to a random point in case we can't find anything .
        	counter++;
            Coordinate coordinate = getRandomInteriorPoints(zoneGeom,1)[0];
            commonPoint = geometryFactory.createPoint(coordinate);
            if (isPointInsideGeometries(landUseGeoms,commonPoint)) return commonPoint;
            counter++;
            if (counter>100){
            	return commonPoint;
            }
        } while(true);
    }

    /**
     * Return a random Coordinate in the geometry or null if
     * ({@link Geometry#isEmpty()} || !(g instanceof {@linkPolygonal})).
     *
     * @param g
     * @return
     */
    public static final Coordinate[] getRandomInteriorPoints(Geometry g, int numPoints){
        if(!(g instanceof Polygonal) || g.isEmpty()) throw new RuntimeException("Given geometry is not an instance of polygon or is empty.");

        RandomPointsBuilder rnd = new RandomPointsBuilder(geometryFactory);
        rnd.setNumPoints(numPoints);
        rnd.setExtent(g);
        return rnd.getGeometry().getCoordinates();
    }

    /**
     * @return true if point is covered by ANY of the geometry
     */
    public static boolean isPointInsideGeometries(final Collection<Geometry> geometries, final Point point) {
        if (geometries.isEmpty()) throw new RuntimeException("Collection of geometries is empty.");
        for(Geometry geom : geometries){
            if ( geom.contains(point) ) {
                return true;
            }
        }
        return false;
    }

}
