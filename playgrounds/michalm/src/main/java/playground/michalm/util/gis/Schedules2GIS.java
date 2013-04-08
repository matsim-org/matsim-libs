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

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedules;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.MatsimArc;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;

import com.vividsolutions.jts.geom.*;


public class Schedules2GIS
{
    private List<Vehicle> vehicles;
    private String filename;
    private GeometryFactory geofac;
    private MatsimVrpData data;
    private Collection<SimpleFeature> features;
		private PolylineFeatureFactory factory;

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

            features = new ArrayList<SimpleFeature>();

            while (driveIter.hasNext()) {
                DriveTask drive = driveIter.next();
                Coordinate[] coords = createLineString(drive);

                if (coords != null) {
                        features.add(this.factory.createPolyline(coords, new Object[] { v.getId(), v.getName(),
                                v.getId(), drive.getTaskIdx() }, null));
                }
            }

            ShapeFileWriter.writeGeometries(features, filename + v.getId() + ".shp");
        }
    }


    private Coordinate[] createLineString(DriveTask driveTask)
    {
        MatsimArc arc = (MatsimArc)driveTask.getArc();
        ShortestPath path = arc.getShortestPath(driveTask.getBeginTime());

        Id[] ids = path.linkIds;

        if (ids.length == 0) {
            return null;
        }

        List<Coordinate> coordList = new ArrayList<Coordinate>();
        Map<Id, ? extends Link> linksMap = data.getScenario().getNetwork().getLinks();

        Link link = linksMap.get(path.linkIds[0]);
        Coord c = link.getFromNode().getCoord();
        coordList.add(new Coordinate(c.getX(), c.getY()));

        for (Id l : path.linkIds) {
            link = linksMap.get(l);
            c = link.getToNode().getCoord();
            coordList.add(new Coordinate(c.getX(), c.getY()));
        }

        return coordList.toArray(new Coordinate[coordList.size()]);
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        
        this.factory = new PolylineFeatureFactory.Builder().
        		setCrs(crs).
        		setName("vrp_route").
        		addAttribute("VEH_ID", Integer.class).
        		addAttribute("VEH_NAME", String.class).
        		addAttribute("ROUTE_ID", Integer.class).
        		addAttribute("ARC_IDX", Integer.class).
        		create();
    }
}
