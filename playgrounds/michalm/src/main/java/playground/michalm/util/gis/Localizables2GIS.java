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

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import pl.poznan.put.vrp.dynamic.data.model.Localizable;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;

import com.vividsolutions.jts.geom.Coordinate;


// taken from org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape
public class Localizables2GIS<T extends Localizable>
{
    private List<T> localizables;
    private String filename;
    private PointFeatureFactory factory;


    public Localizables2GIS(List<T> localizables, String filename, String coordinateSystem)
    {
        this.localizables = localizables;
        this.filename = filename;
        initFeatureType(coordinateSystem);
    }


    public void write()
    {
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (Localizable localizable : localizables) {
            features.add(getFeature(localizable.getVertex()));
        }

        ShapeFileWriter.writeGeometries(features, filename);
    }


    private SimpleFeature getFeature(Vertex vertex)
    {
        try {
            return this.factory.createPoint(new Coordinate(vertex.getX(), vertex.getY()),
                    new Object[] { vertex.getId(), vertex.getName() }, null);
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
