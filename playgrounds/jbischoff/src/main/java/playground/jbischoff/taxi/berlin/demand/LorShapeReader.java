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

package playground.jbischoff.taxi.berlin.demand;

import java.util.*;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;


public class LorShapeReader
{
    private Map<String, Geometry> shapeMap;


    public LorShapeReader()
    {
        this.shapeMap = new HashMap<String, Geometry>();

    }


    public void readShapeFile(String filename, String attrString)
    {

        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;

            try {
                geometry = wktReader.read( (ft.getAttribute("the_geom")).toString());
                this.shapeMap.put(ft.getAttribute(attrString).toString(), geometry);

            }
            catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


    public Map<String, Geometry> getShapeMap()
    {
        return shapeMap;
    }


    public static void main(String[] args)
    {
        LorShapeReader lsr = new LorShapeReader();
        lsr.readShapeFile("/Users/jb/Downloads/LOR_SHP_EPSG_25833/Planungsraum.shp", "SCHLUESSEL");
        System.out.println(lsr.getShapeMap().keySet());
        System.out.println(lsr.getShapeMap().keySet().size());

    }
}
