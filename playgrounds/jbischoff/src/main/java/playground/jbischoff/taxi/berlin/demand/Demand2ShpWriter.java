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

import java.io.*;
import java.util.*;

import org.geotools.feature.simple.*;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.*;
import org.matsim.matrices.Entry;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.*;

import playground.jbischoff.taxi.berlin.data.BeelineDistanceExractor;
import playground.michalm.util.matrices.MatrixUtils;


public class Demand2ShpWriter
{

    /**
     * @param args
     */
    Matrices matrices = MatrixUtils
            .readMatrices("C:/local_jb/data/taxi_berlin/2013/OD/demandMatrices.xml");
    Matrices accmat = new Matrices();
    Scenario scenario;
    private SimpleFeatureBuilder builder;
    private BeelineDistanceExractor bde;
    private GeometryFactory geofac;


    public static void main(String[] args)
    {
        Demand2ShpWriter dsw = new Demand2ShpWriter();
        dsw.accumulate();
//        dsw.writeShape("C:/local_jb/data/taxi_berlin/2013/OD/demandLines.shp");
        dsw.extractSym("C:/local_jb/data/taxi_berlin/2013/OD/symdemand.txt");
    }


    private void accumulate()
    {
        Matrix sum = accmat.createMatrix("accmat", "accumulated demand");
        for (Matrix matrix : matrices.getMatrices().values()) {
            for (ArrayList<Entry> l : matrix.getFromLocations().values()) {
                for (Entry e : l) {
                    MatrixUtils.setOrIncrementValue(sum, e.getFromLocation(), e.getToLocation(), e.getValue());
                }
            }
        }

    }


    private void writeShape(String outfile)
    {
        Matrix matrix = accmat.getMatrix("accmat");
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (ArrayList<Entry> l : matrix.getFromLocations().values()) {
            for (Entry e : l) {

                LineString ls = this.geofac.createLineString(new Coordinate[] {
                        MGC.coord2Coordinate(bde.getZoneCentroid(Id.create(e.getFromLocation(), Zone.class))),
                        MGC.coord2Coordinate(bde.getZoneCentroid(Id.create(e.getToLocation(), Zone.class))) });
                Object[] attribs = new Object[5];
                attribs[0] = ls;
                attribs[1] = e.getFromLocation().toString();
                attribs[2] = e.getToLocation().toString();
                attribs[3] = e.getValue();
                attribs[4] = 0.1 * e.getValue();
                String ftid = e.getFromLocation().toString()+"_"+e.getToLocation().toString();
                SimpleFeature sf = this.builder.buildFeature(ftid, attribs);
                features.add(sf);
            }

        }
        ShapeFileWriter.writeGeometries(features, outfile);

    }

    private void extractSym(String filename){
        Writer writer = IOUtils.getBufferedWriter(filename);
        Set<String> handledRelations = new HashSet<>();
        Matrix matrix = accmat.getMatrix("accmat");

        try{
        for (ArrayList<Entry> l : matrix.getFromLocations().values()) {
            for (Entry e : l) {
            String out = e.getFromLocation().toString()+e.getToLocation().toString();
            String in = e.getToLocation().toString()+e.getFromLocation().toString();
            if (handledRelations.contains(in)) continue; 
            if (handledRelations.contains(out)) continue; 
            double back = 0;
          try{back = matrix.getEntry(e.getToLocation(), e.getFromLocation()).getValue();}
          catch (NullPointerException b) {}
            writer.write(e.getFromLocation().toString()+"\t"+e.getToLocation().toString()+"\t"+Math.floor(e.getValue())+"\t"+Math.floor(back)+"\n");
            handledRelations.add(in);
            handledRelations.add(out);
            }
        }
        writer.flush();
        writer.close();
        }
        catch (IOException e ){
            e.printStackTrace();
        }
    }

    public Demand2ShpWriter()
    {
        bde = new BeelineDistanceExractor();
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        this.geofac = new GeometryFactory();
        initFeatureType();
    }


    private void initFeatureType()
    {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("trips");
        typeBuilder.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM33N));
        typeBuilder.add("location", LineString.class);
        typeBuilder.add("fromId", String.class);
        typeBuilder.add("toId", String.class);
        
        typeBuilder.add("amount", Double.class);
        typeBuilder.add("visWidth", Double.class);

        this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
    }

}
