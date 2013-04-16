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

package playground.michalm.demand;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import pl.poznan.put.util.random.*;

import com.vividsolutions.jts.geom.*;


public class DefaultActivityGenerator
    implements ActivityGenerator
{
    private final UniformRandom uniform = RandomUtils.getGlobalUniform();
    private final NetworkImpl network;
    private final Scenario scenario;
    private final PopulationFactory pf;

    private GeometryProvider geometryProvider;
    private PointAcceptor pointAcceptor;


    public DefaultActivityGenerator(Scenario scenario)
    {
        this(scenario, DEFAULT_GEOMETRY_PROVIDER, DEFAULT_POINT_ACCEPTOR);
    }


    public DefaultActivityGenerator(Scenario scenario, GeometryProvider geometryProvider,
            PointAcceptor pointAcceptor)
    {
        this.scenario = scenario;
        this.network = (NetworkImpl)scenario.getNetwork();
        this.pf = scenario.getPopulation().getFactory();
        this.geometryProvider = geometryProvider;
        this.pointAcceptor = pointAcceptor;
    }


    @Override
    public Activity createActivityInZone(Zone zone, String actType)
    {
        return createActivityInZone(zone, actType, null);
    }


    @Override
    public Activity createActivityInZone(Zone zone, String actType, Activity previousActivity)
    {
        Geometry geometry = geometryProvider.getGeometry(zone, actType);
        Envelope envelope = geometry.getEnvelopeInternal();
        double minX = envelope.getMinX();
        double maxX = envelope.getMaxX();
        double minY = envelope.getMinY();
        double maxY = envelope.getMaxY();

        Point p = null;
        Id bannedLinkId = previousActivity != null ? previousActivity.getLinkId() : null;

        for (int i = 0;; i++) {
            double x = uniform.nextDouble(minX, maxX);
            double y = uniform.nextDouble(minY, maxY);
            p = MGC.xy2Point(x, y);

            if (i == 1000) {
                CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
                PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().setCrs(crs)
                        .setName("PolygonFeatureType").create();
                SimpleFeature feature = factory.createPolygon((Polygon)geometry,
                        Collections.<String, Object> emptyMap(), null);
                Set<SimpleFeature> featureSet = new HashSet<SimpleFeature>();
                featureSet.add(feature);
                ShapeFileWriter.writeGeometries(featureSet, "d:\\looped_zoneId_" + zone.getId() + "_actType_" + actType + ".shp");

                System.out.println("Got stuck at zoneId=" + zone.getId() + " actType=" + actType);
            }

            if (!geometry.contains(p) || !pointAcceptor.acceptPoint(zone, actType, p)) {
                continue;
            }

            Coord coord = scenario.createCoord(p.getX(), p.getY());
            Link link = network.getNearestLink(coord);

            if (link.getId().equals(bannedLinkId)) {
                continue;
            }

            ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
            activity.setLinkId(link.getId());
            return activity;
        }
    }


    public static final GeometryProvider DEFAULT_GEOMETRY_PROVIDER = new GeometryProvider() {
        public Geometry getGeometry(Zone zone, String actType)
        {
            return (Geometry)zone.getZonePolygon().getDefaultGeometry();
        }
    };


    public static interface GeometryProvider
    {
        Geometry getGeometry(Zone zone, String actType);
    }


    public static final PointAcceptor DEFAULT_POINT_ACCEPTOR = new PointAcceptor() {
        public boolean acceptPoint(Zone zone, String actType, Point point)
        {
            return true;
        }
    };


    public static interface PointAcceptor
    {
        boolean acceptPoint(Zone zone, String actType, Point point);
    }
}
