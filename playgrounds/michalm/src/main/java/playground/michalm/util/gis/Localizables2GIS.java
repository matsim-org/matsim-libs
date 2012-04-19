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

import java.io.IOException;
import java.util.*;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.*;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.file.LacknerReader;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;

import com.vividsolutions.jts.geom.Point;


// taken from org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape
public class Localizables2GIS<T extends Localizable>
{
    private List<T> localizables;
    private String filename;
    private FeatureType featureType;


    public Localizables2GIS(List<T> localizables, String filename, String coordinateSystem)
    {
        this.localizables = localizables;
        this.filename = filename;
        initFeatureType(coordinateSystem);
    }


    public void write()
    {
        Collection<Feature> features = new ArrayList<Feature>();

        for (Localizable localizable : localizables) {
            features.add(getFeature(localizable.getVertex()));
        }

        ShapeFileWriter.writeGeometries(features, filename);
    }


    private Feature getFeature(Vertex vertex)
    {
        Point p = MGC.xy2Point(vertex.getX(), vertex.getY());

        try {
            return featureType.create(new Object[] { p, vertex.getId(), vertex.getName() });
        }
        catch (IllegalAttributeException e) {
            throw new RuntimeException(e);
        }
    }


    private void initFeatureType(final String coordinateSystem)
    {
        CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);
        AttributeType[] attribs = new AttributeType[3];
        attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null,
                null, crs);
        attribs[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
        attribs[2] = AttributeTypeFactory.newAttributeType("Name", String.class);

        try {
            featureType = FeatureTypeBuilder.newFeatureType(attribs, "vrp_node");
        }
        catch (FactoryRegistryException e) {
            e.printStackTrace();
        }
        catch (SchemaException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args)
        throws IOException
    {
        String dirName;
        String vrpDirName;
        String vrpStaticFileName;
        String outFileNameCust;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            vrpDirName = dirName + "dvrp\\";
            vrpStaticFileName = "A102.txt";
            outFileNameCust = vrpDirName + "customers.shp";
        }
        else if (args.length == 4) {
            dirName = args[0];
            vrpDirName = dirName + args[1];
            vrpStaticFileName = args[2];
            outFileNameCust = vrpDirName + args[3];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        VrpData data = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                VertexImpl.getBuilder());
        String coordSystem = TransformationFactory.WGS84_UTM33N;

        new Localizables2GIS<Customer>(data.getCustomers(), outFileNameCust, coordSystem).write();
    }
}
