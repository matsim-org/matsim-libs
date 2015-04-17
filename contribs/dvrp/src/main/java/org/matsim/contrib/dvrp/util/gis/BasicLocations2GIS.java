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

package org.matsim.contrib.dvrp.util.gis;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


// taken from org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape
public class BasicLocations2GIS<T extends BasicLocation<T>>
{
    private List<T> basicLocations;
    private String file;
    private PointFeatureFactory factory;


    public BasicLocations2GIS(List<T> basicLocations, String file, String coordinateSystem)
    {
        this.basicLocations = basicLocations;
        this.file = file;
        initFeatureType(coordinateSystem);
    }


    public void write()
    {
        Collection<SimpleFeature> features = new ArrayList<>();

        for (BasicLocation<T> bl : basicLocations) {
            features.add(getFeature(bl.getCoord()));
        }

        ShapeFileWriter.writeGeometries(features, file);
    }


    private SimpleFeature getFeature(Coord coord)
    {
        try {
            return this.factory.createPoint(coord, new Object[] { coord.toString() },
                    coord.toString());
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        this.factory = new PointFeatureFactory.Builder().setCrs(crs).setName("vrp_node")
                .addAttribute("ID", Integer.class).addAttribute("Name", String.class).create();
    }
}
