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

import java.util.*;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;

import com.vividsolutions.jts.geom.*;


public class Schedules2GIS
{
    private List<Vehicle> vehicles;
    private String filename;
    private FeatureType featureType;
    private GeometryFactory geofac;
    private MatsimVrpData data;
    private Collection<Feature> features;


    public Schedules2GIS(List<Vehicle> vehicles, MatsimVrpData data, String filename)
    {
        this.vehicles = vehicles;
        this.data = data;
        this.filename = filename;

        geofac = new GeometryFactory();
        initFeatureType(data.getCoordSystem());
    }


    public void write()
    {
        for (Vehicle v : vehicles) {
            Iterator<DriveTask> driveIter = Schedules.createDriveTaskIter(v.getSchedule());

            if (!driveIter.hasNext()) {
                continue;
            }

            features = new ArrayList<Feature>();

            while (driveIter.hasNext()) {
                DriveTask drive = driveIter.next();
                LineString ls = createLineString(drive);

                if (ls != null) {
                    try {
                        features.add(featureType.create(new Object[] { ls, v.getId(), v.getName(),
                                v.getId(), drive.getTaskIdx() }));
                    }
                    catch (IllegalAttributeException e) {
                        e.printStackTrace();
                    }
                }
            }

            ShapeFileWriter.writeGeometries(features, filename + v.getId() + ".shp");
        }
    }


    private LineString createLineString(DriveTask driveTask)
    {
        SPEntry entry = data.getMatsimVrpGraph()
                .getShortestPath(driveTask.getFromVertex(), driveTask.getToVertex())
                .getSPEntry(driveTask.getBeginTime());

        Id[] ids = entry.linkIds;

        if (ids.length == 0) {
            return null;
        }

        List<Coordinate> coordList = new ArrayList<Coordinate>();
        Map<Id, ? extends Link> linksMap = data.getScenario().getNetwork().getLinks();

        Link link = linksMap.get(entry.linkIds[0]);
        Coord c = link.getFromNode().getCoord();
        coordList.add(new Coordinate(c.getX(), c.getY()));

        for (Id l : entry.linkIds) {
            link = linksMap.get(l);
            c = link.getToNode().getCoord();
            coordList.add(new Coordinate(c.getX(), c.getY()));
        }

        return geofac.createLineString(coordList.toArray(new Coordinate[coordList.size()]));
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        AttributeType[] attribs = new AttributeType[5];
        attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class,
                true, null, null, crs);
        attribs[1] = AttributeTypeFactory.newAttributeType("VEH_ID", Integer.class);
        attribs[2] = AttributeTypeFactory.newAttributeType("VEH_NAME", String.class);
        attribs[3] = AttributeTypeFactory.newAttributeType("ROUTE_ID", Integer.class);
        attribs[4] = AttributeTypeFactory.newAttributeType("ARC_IDX", Integer.class);

        try {
            featureType = FeatureTypeBuilder.newFeatureType(attribs, "vrp_route");
        }
        catch (FactoryRegistryException e) {
            e.printStackTrace();
        }
        catch (SchemaException e) {
            e.printStackTrace();
        }
    }
}
