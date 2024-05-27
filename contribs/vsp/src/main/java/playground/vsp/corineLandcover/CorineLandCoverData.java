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

package playground.vsp.corineLandcover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.gis.GeoFileReader;

/**
 * Created by amit on 31.07.17.
 */

public class CorineLandCoverData {

    public static final Logger LOGGER = LogManager.getLogger(CorineLandCoverData.class);

    private final Map<LandCoverUtils.LandCoverActivityType, Geometry> activityType2CombinedLandcoverZone = new HashMap<>();
    Map<LandCoverUtils.LandCoverActivityType, List<Geometry>> activityTypes2ListOfLandCoverZones = new HashMap<>();


    private int warnCnt = 0;
    private final LandCoverUtils landCoverUtils = new LandCoverUtils();

    private final boolean simplifyGeometries;
    private final  boolean combiningGeom;

    private int thresholdForPointInsideLandUseGeoms = 10000;

    public CorineLandCoverData( String corineLandCoverShapeFile, boolean simplifyGeometries, boolean combiningGeom) {

        LOGGER.info("Reading CORINE landcover shape file . . .");
        Collection<SimpleFeature> landCoverFeatures = GeoFileReader.getAllFeatures(corineLandCoverShapeFile);

        this.simplifyGeometries = simplifyGeometries;
        if (this.simplifyGeometries) LOGGER.warn("Geometries will be simplified such that number of vertices in each geometry is less than 1000. " +
                "This is likely to speed up the process.");

        for (SimpleFeature landCoverZone : landCoverFeatures) {
            int landCoverId = Integer.valueOf( (String) landCoverZone.getAttribute(LandCoverUtils.CORINE_LANDCOVER_TAG_ID));
            List<LandCoverUtils.LandCoverActivityType> acts = landCoverUtils.getActivityTypesFromZone(landCoverId);

            for (LandCoverUtils.LandCoverActivityType activityTypeFromLandCover : acts ) {
                List<Geometry> geoms = activityTypes2ListOfLandCoverZones.get(activityTypeFromLandCover);
                if (geoms==null) {
                    geoms = new ArrayList<>();
                }

                Geometry geomToAdd = (Geometry)landCoverZone.getDefaultGeometry();
                if (this.simplifyGeometries) geomToAdd = GeometryUtils.getSimplifiedGeom(geomToAdd);

                geoms.add(  geomToAdd );
                activityTypes2ListOfLandCoverZones.put(activityTypeFromLandCover, geoms);
            }
        }

        this.combiningGeom = combiningGeom;
        if (this.combiningGeom) {
            LOGGER.warn("Combining geoms is most expensive operation. See http://docs.geotools.org/latest/userguide/library/jts/combine.html");
            // combine geoms of the same activity types
            // Not sure, Out of two options --simplify and merge OR merge and simplify-- I think, the former would be somewhat better. Amit Oct'17
            for (LandCoverUtils.LandCoverActivityType activityTypeFromLandCover : activityTypes2ListOfLandCoverZones.keySet()) {
                LOGGER.info("Merging the geometries of the activity types "+activityTypeFromLandCover+".");
                Geometry combinedGeom = GeometryUtils.combine(activityTypes2ListOfLandCoverZones.get(activityTypeFromLandCover));
                activityType2CombinedLandcoverZone.put(activityTypeFromLandCover, combinedGeom);
            }
        }
    }

    public CorineLandCoverData(final String corineLandCoverShapeFile) {
        this(corineLandCoverShapeFile, false, false);
    }

    // An example
    public static void main(String[] args) {
        String landcoverFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        CorineLandCoverData landCoverInformer = new CorineLandCoverData(landcoverFile, true, false);
//        landCoverInformer.getRandomPoint(...);
    }

    /**
     * @param thresholdForPointInsideLandUseGeoms number of times the check is performed (if the point is inside 'landUseGeoms'); afterwards a random point is returned.
     */
    public void setThresholdForPointInsideLandUseGeoms(int thresholdForPointInsideLandUseGeoms) {
        this.thresholdForPointInsideLandUseGeoms = thresholdForPointInsideLandUseGeoms;
    }

    /**
     * @param feature simpleFeature (zone)
     * @param activityType classified as 'home' and 'other' activity types.
     * @return a random point such that it is inside the @param feature as well as inside the zones corresponding to the given activity type.
     */
    public Point getRandomPoint (final SimpleFeature feature, final LandCoverUtils.LandCoverActivityType activityType) {
        return getRandomPoint((Geometry)feature.getDefaultGeometry(), activityType);
    }

    /**
     * @param geometry geometry of zone simplefeature
     * @param activityType classified as 'home' and 'other' activity types.
     * @return a random point such that it is inside the @param feature as well as inside the zones corresponding to the given activity type.
     */
    public Point getRandomPoint(final Geometry geometry, final LandCoverUtils.LandCoverActivityType activityType) {
        Geometry zoneGeom;
        if ( this.simplifyGeometries ) {
            zoneGeom = GeometryUtils.getSimplifiedGeom( geometry) ;
        } else {
            zoneGeom = geometry;
        }

        Geometry landUseGeom ;
        Collection<Geometry> landUseGeoms ;
        if (activityType.equals(LandCoverUtils.LandCoverActivityType.home) ) {
            landUseGeom =  this.activityType2CombinedLandcoverZone.get(activityType) ;
            landUseGeoms = this.activityTypes2ListOfLandCoverZones.get(activityType);
        } else {
            if (! activityType.equals(LandCoverUtils.LandCoverActivityType.other) && warnCnt < 1) {
                LOGGER.warn("A random point is desired for activity type "+ activityType+ ". However, the CORINE landcover data is categorized only for 'home' and 'other' activity types.");
                LOGGER.warn(Gbl.ONLYONCE);
                warnCnt++;
            }

            landUseGeom =  this.activityType2CombinedLandcoverZone.get(LandCoverUtils.LandCoverActivityType.other) ;
            landUseGeoms = this.activityTypes2ListOfLandCoverZones.get(LandCoverUtils.LandCoverActivityType.other);
        }

        if (this.combiningGeom) return GeometryUtils.getPointInteriorToGeometry( landUseGeom, zoneGeom );
        else return GeometryUtils.getPointInteriorToGeometriesWithFallback( landUseGeoms, zoneGeom, this.thresholdForPointInsideLandUseGeoms );
    }

    /**
     * @param feature simpleFeature (zone)
     * @param activityType classified as 'home' and 'other' activity types.
     * @return
     */
    public Coord getRandomCoord (final SimpleFeature feature, final LandCoverUtils.LandCoverActivityType activityType) {
        Point p = getRandomPoint(feature, activityType);
        return new Coord(p.getX(), p.getY());
    }

    /**
     * @param geometry simpleFeature (zone)
     * @param activityType classified as 'home' and 'other' activity types.
     * @return
     */
    public Coord getRandomCoord (final Geometry geometry, final LandCoverUtils.LandCoverActivityType activityType) {
        Point p = getRandomPoint(geometry, activityType);
        return new Coord(p.getX(), p.getY());
    }

    /**
     *
     * @param activityType
     * @param point
     * @return if point falls inside the geom of given activity type
     */
    public boolean isPointInsideLandCover(LandCoverUtils.LandCoverActivityType activityType, Point point){
        if (combiningGeom) {
            Geometry geometry = this.activityType2CombinedLandcoverZone.get(activityType);
            return geometry.contains(point);
        } else {
            return GeometryUtils.isPointInsideGeometries(this.activityTypes2ListOfLandCoverZones.get(activityType), point);
        }
    }
}
