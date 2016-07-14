/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.util.gis;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.*;


public class PolygonBasedFilter
{
    public static Predicate<Link> createLinkInsidePolygonPredicate(final Geometry polygonGeometry,
            final boolean includeBorderLinks)
    {
        return new Predicate<Link>() {
            public boolean apply(Link link)
            {
                Point fromPoint = MGC.coord2Point(link.getFromNode().getCoord());
                boolean fromPointInside = polygonGeometry.contains(fromPoint);

                if (fromPointInside && includeBorderLinks) {
                    return true;// inclusion of only 1 point is enough
                }
                else if (!fromPointInside && !includeBorderLinks) {
                    return false;// both points must be within
                }

                // now the result depends on the inclusion of "toPoint"
                Point toPoint = MGC.coord2Point(link.getToNode().getCoord());
                return polygonGeometry.contains(toPoint);
            };
        };
    }


    public static Iterable<? extends Link> filterLinksInsidePolygon(Iterable<? extends Link> links,
            Geometry polygonGeometry, boolean includeBorderLinks)
    {
        return Iterables.filter(links,
                createLinkInsidePolygonPredicate(polygonGeometry, includeBorderLinks));
    }


    public static Iterable<? extends Link> filterLinksOutsidePolygon(Iterable<? extends Link> links,
            Geometry polygonGeometry, boolean includeBorderLinks)
    {
        return Iterables.filter(links, Predicates
                .not(createLinkInsidePolygonPredicate(polygonGeometry, !includeBorderLinks)));// includeBorderLinks must be negated
    }


    public static Predicate<SimpleFeature> createFeatureInsidePolygonPredicate(
            final Geometry polygonGeometry)
    {
        return new Predicate<SimpleFeature>() {
            public boolean apply(SimpleFeature feature)
            {
                return polygonGeometry.contains((Geometry)feature.getDefaultGeometry());
            }
        };
    }


    public static Iterable<? extends SimpleFeature> filterFeaturesInsidePolygon(
            Iterable<? extends SimpleFeature> features, Geometry polygonGeometry)
    {
        return Iterables.filter(features, createFeatureInsidePolygonPredicate(polygonGeometry));
    }


    public static Iterable<? extends SimpleFeature> filterFeaturesOutsidePolygon(
            Iterable<? extends SimpleFeature> features, Geometry polygonGeometry)
    {
        return Iterables.filter(features,
                Predicates.not(createFeatureInsidePolygonPredicate(polygonGeometry)));
    }


    public static Geometry readPolygonGeometry(String file)
    {
        Collection<SimpleFeature> ftColl = ShapeFileReader.getAllFeatures(file);
        if (ftColl.size() != 1) {
            throw new RuntimeException("No. of features: " + ftColl.size() + "; should be 1");
        }

        return (Geometry)ftColl.iterator().next().getDefaultGeometry();
    }
}
