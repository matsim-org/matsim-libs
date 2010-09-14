/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkToGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.shapeFileProcessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class NetworkToGraph {
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	private final NetworkImpl network;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;

	public NetworkToGraph(NetworkImpl network, CoordinateReferenceSystem coordinateReferenceSystem) {
		this.geofac = new GeometryFactory();
		this.network = network;
		this.crs = coordinateReferenceSystem;
	}

	public Collection<Feature> generateFromNet() throws FactoryRegistryException, SchemaException, IllegalAttributeException {

		Collection<Feature> features = new ArrayList<Feature>();

		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", String.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", String.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", String.class);
		AttributeType length = AttributeTypeFactory.newAttributeType(
				"length", Double.class);
		AttributeType minWidth = AttributeTypeFactory.newAttributeType(
				"minWidth", Double.class);
		AttributeType effectiveWidth = AttributeTypeFactory.newAttributeType(
				"effWidth", Double.class);
		FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode, length, minWidth, effectiveWidth }, "link");


		for (Link link : this.network.getLinks().values()){
			LinearRing lr = getLinearRing(link);
			Polygon p = new Polygon(lr, null, this.geofac);
			MultiPolygon mp = new MultiPolygon(new Polygon[] {p},this.geofac);
			LineString ls = new LineString(new CoordinateArraySequence(new Coordinate [] {MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord())}),this.geofac);

			Feature ft = ftRoad.create(new Object [] {mp , link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(),"",""},"network");
			features.add(ft);
		}
		return features;
	}

	private LinearRing getLinearRing(Link link) {
		double minWidth = link.getCapacity() / GISToMatsimConverter.CAPACITY_COEF;
		minWidth = Math.min(minWidth,20);
//		minWidth = 10;
//		Coordinate zero = new Coordinate(0,0);
		Coordinate  from = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()) ;

		Coordinate  to = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY()) ;

		double xdiff = to.x - from.x;
		double ydiff = to.y - from.y;

//		double distA = from.distance(zero);
//		double distB = to.distance(zero);

		double ogradient = Double.MAX_VALUE;
		if (ydiff != 0)
			ogradient = -xdiff / ydiff;
		double csq = Math.pow(minWidth,2);
//		double csq = Math.pow(4,2);
		double xwidth = Math.sqrt(csq/(1+Math.pow(ogradient,2)));
		double ywidth = xwidth*ogradient;

		Coordinate fromB = new Coordinate(from.x+xwidth,from.y+ywidth,0);
		Coordinate toB = new Coordinate(to.x+xwidth,to.y+ywidth,0);

		CoordinateSequence coords = new CoordinateArraySequence(new Coordinate [] {from, to, toB, fromB, from});

		return new LinearRing(coords,this.geofac);



	}

	public static void main(String [] args) throws FactoryException, FactoryRegistryException, SchemaException, IllegalAttributeException, IOException {
		String netfile = "./networks/padang_net_v20080618.xml";
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netfile);
		CoordinateReferenceSystem crs = CRS.parseWKT(WGS84_UTM47S);
		Collection<Feature> ft = new NetworkToGraph(network,crs).generateFromNet();
		ShapeFileWriter.writeGeometries(ft, "./padang/network_v20080618.shp");
	}

}
