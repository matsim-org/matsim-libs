/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.droeder.stockholm;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author droeder / Senozon Deutschland GmbH
 *
 */
public class CreateShapeFiles {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(CreateShapeFiles.class);

    public CreateShapeFiles() {

    }

    public static void main(String[] args) {
	
	String networkfile = "C:\\Users\\Daniel\\Desktop\\_Ablage\\Stockholm\\osm\\network.xml.gz";

	Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(sc).readFile(networkfile);
	
	Collection<SimpleFeature> features = new HashSet<SimpleFeature>();
	LinkFeatureBuilder builder = new LinkFeatureBuilder(MGC.getCRS("EPSG:3006"));
	for(Link l: sc.getNetwork().getLinks().values()){
	    features.add(builder.getFeature(l));
	}
	
	ShapeFileWriter.writeGeometries(features, "C:\\Users\\Daniel\\Desktop\\_Ablage\\Stockholm\\osm\\network.shp");
	
    }
    
    
    private static class LinkFeatureBuilder{
	
	private SimpleFeatureBuilder builder;
	private GeometryFactory factory;

	public LinkFeatureBuilder(CoordinateReferenceSystem crs) {
	    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
	    builder.add("the_geom", LineString.class);
	    builder.add("id", String.class);
	    builder.add("freespeed", Double.class);
	    builder.setCRS(crs);
	    builder.setName("");
	    this.factory = new GeometryFactory();
	    this.builder = new SimpleFeatureBuilder(builder.buildFeatureType());
	}
	
	public SimpleFeature getFeature(Link l){
	    LineString ls = factory.createLineString(new Coordinate[]{
		    MGC.coord2Coordinate(l.getFromNode().getCoord()), 
		    MGC.coord2Coordinate(l.getToNode().getCoord())}) ;
	    this.builder.add(ls);
	    this.builder.add(l.getId().toString());
	    this.builder.add(l.getFreespeed());
	    return this.builder.buildFeature(null);
	}
    }
    


}

