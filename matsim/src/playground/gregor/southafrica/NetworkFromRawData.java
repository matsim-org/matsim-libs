/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFromRawData.java
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

package playground.gregor.southafrica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class NetworkFromRawData {


	private static final Logger log = Logger.getLogger(NetworkFromRawData.class);
	private static final double CATCH_RADIUS = 1;
	private final NetworkLayer network;
	private final Collection<Feature> l;

	private final QuadTree<Node> el1;
	private final QuadTree<Node> el2;
	private final QuadTree<Node> el3;

	private Integer nodeID = 0;

	public NetworkFromRawData(final Collection<Feature> l, final NetworkLayer network, Envelope e) {
		this.el1 = new QuadTree<Node>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		this.el2 = new QuadTree<Node>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		this.el3 = new QuadTree<Node>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		this.network = network;
		this.l = l;
	}

	private NetworkLayer constructNetwork() {
		createNet();
//		new NetworkCleaner().run(this.network);

		return this.network;
	}








	/**
	 * 
	 */
	private void createNet() {
		for(Feature ft : this.l) {
			//this variable is used to skip features such as railways, walkways, restricted access roads
			boolean writeThisLink = true;

			LineString geo = (LineString)((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
			Coordinate fromC = geo.getStartPoint().getCoordinate();
			Coordinate toC = geo.getEndPoint().getCoordinate();

			int from_el = (Integer) ft.getAttribute(16);
			int id = ((Integer) ft.getAttribute(24));
			int to_el = ((Integer) ft.getAttribute(17));
			double length = ((Double) ft.getAttribute(22));
			String oneWay = (String) ft.getAttribute(18);
			String roadType = (String) ft.getAttribute(9);
			double freespeed = ((Integer) ft.getAttribute(23)*10/36);
			double capacity = 0;
			double permlanes = 1;
			if (roadType.equals("STREET")) {
				capacity = 1000;
				permlanes =1;

//				writeThisLink = false;
			} else if (roadType.equals("SECONDARY ROAD")) {
				capacity = 1500;
				permlanes =1;
//				writeThisLink = false;
			} else if (roadType.equals("MAIN ROAD")) {
				capacity = 2000;
				permlanes =2;
			} else if (roadType.equals("DUAL CARRIAGEWAY")) {
				capacity = 2000;
				permlanes =2;
			} else if (roadType.equals("OTHER ROAD")) {
				capacity = 1000;
				permlanes =1;
//				writeThisLink = false;
			} else if (roadType.equals("PUBLIC ACCESS ROAD")) {
				capacity = 2000;
				permlanes =2;
//				writeThisLink = false;
			}else if (roadType.equals("NATIONAL HIGHWAY")) {
				capacity = 3000;
				permlanes =3;
			}else if (roadType.equals("NATIONAL ROAD")) {
				capacity = 2000;
				permlanes =2;
			}else if (roadType.equals("PEDESTRIAN WALKWAY") ||
					  roadType.equals("RESTRICTED ACCESS ROAD")) {
				writeThisLink = false;
			}else {
				writeThisLink = false;
			}/*else{
				throw new RuntimeException("Unknown road type: " + roadType);
			}*/

			if(writeThisLink){
				Node fromNode = getNode(fromC,from_el);
				Node toNode = getNode(toC,to_el);

				if (fromNode == toNode) {
					int i = 0 ; i++;
				}
				if (oneWay.equals("B")) {
					this.network.createLink(id + "", fromNode.getId().toString(), toNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
					this.network.createLink((1000000+id) + "", toNode.getId().toString(), fromNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
				} else if (oneWay.equals("FT")) {
					this.network.createLink(id + "", fromNode.getId().toString(), toNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
				} else if (oneWay.equals("TF")) {
					this.network.createLink(id + "", toNode.getId().toString(), fromNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
				} else	if (oneWay.equals("N")) {
					this.network.createLink(id + "", fromNode.getId().toString(), toNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
					this.network.createLink((1000000+id) + "", toNode.getId().toString(), fromNode.getId().toString(), ""+length, ""+freespeed, ""+capacity, ""+permlanes, id + "", "");
				} else {
					throw new RuntimeException("Unknown directional information: " + oneWay);
				}
			}
		}
	}

	private Node getNode(Coordinate c, int ele) {

		Node node = null;
		if (ele  == 0) {
			Collection<Node> nodes = this.el1.get(c.x, c.y, CATCH_RADIUS);
			if (nodes.size() == 0) {
				node = this.network.createNode(new IdImpl(this.nodeID++), new CoordImpl(c.x, c.y));
				this.el1.put(c.x, c.y, node);
			} else {
				node = this.el1.get(c.x,c.y);
			}

		} else if (ele == 1) {
			Collection<Node> nodes = this.el2.get(c.x, c.y, CATCH_RADIUS);
			if (nodes.size() == 0) {
				node = this.network.createNode(new IdImpl(this.nodeID++), new CoordImpl(c.x, c.y));
				this.el2.put(c.x, c.y, node);
			} else {
				node = this.el2.get(c.x,c.y);
			}
		} else if (ele == 2) {
			Collection<Node> nodes = this.el3.get(c.x, c.y, CATCH_RADIUS);
			if (nodes.size() == 0) {
				node = this.network.createNode(new IdImpl(this.nodeID++), new CoordImpl(c.x, c.y));
				this.el3.put(c.x, c.y, node);
			} else {
				node = this.el3.get(c.x,c.y);
			}
		} else {
			throw new RuntimeException("ele = " + ele);
		}


//		this.network.createNode((this.nodeID++).toString(), Double.toString(c.x), Double.toString(c.y), "");


		return node;
	}

	public static void main(final String [] args) throws FactoryRegistryException, IOException, FactoryException, SchemaException, IllegalAttributeException, Exception {



		final String links = "./southafrica/GP_UTM/routes_UTM.shp";

		FeatureSource l = null;
		try {
			l = ShapeFileReader.readDataFile(links);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> ls = getPolygons(l);

		Envelope e = l.getBounds();

		final NetworkLayer network = new NetworkLayer();

		network.setCapacityPeriod(3600);
		new NetworkFromRawData(ls, network, e).constructNetwork();
//		new NetworkCleaner().run(network);
//		new NetworkCleaner().run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);
		new NetworkWriter(network,"southafrica/network/routes_networkRAW.xml").write();
//		ShapeFileWriter.writeGeometries(genFeatureCollection((Collection<Link>) network.getLinks().values(), n),"./padang/network_" + VERSION + "/matsim_net.shp" );

	}

	private static Collection<Feature> genFeatureCollection(final Collection<Link> links, final FeatureSource fs) throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{


//		dummy.id = -1;
		final GeometryFactory geofac = new GeometryFactory();
		final Collection<Feature> features = new ArrayList<Feature>();

		final AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, fs.getSchema().getDefaultGeometry().getCoordinateSystem());
		final AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", Integer.class);
		final AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", Integer.class);
		final AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", Integer.class);
		final FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode }, "link");
		int ID = 0;
		for (final Link link : links){
			final Coordinate c1 = MGC.coord2Coordinate(link.getFromNode().getCoord());
			final Coordinate c2 = MGC.coord2Coordinate(link.getToNode().getCoord());
			final LineString ls = geofac.createLineString(new Coordinate[] {c1,c2});

			final Feature ft = ftRoad.create(new Object [] {new MultiLineString(new LineString []{ls},geofac) , ID++, link.getFromNode().getId(),link.getToNode().getId()},"network");
			features.add(ft);

		}


		return features;
	}

	private static Collection<Feature> getPolygons(final FeatureSource n) {
		final Collection<Feature> polygons = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(feature);
	}

		return polygons;
	}



}

