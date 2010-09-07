/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFromShape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.networkFromShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class NetworkFromShape {


	public static void main(String [] args) throws IOException, IllegalAttributeException {

		String links = "/home/laemmel/workspace/vsp-cvs/studies/padang/gis/network_v20080618/links.shp";
		String nodes = "/home/laemmel/workspace/vsp-cvs/studies/padang/gis/network_v20080618/nodes.shp";

		double freespeed = 13.8888888888888;
		double laneWidth = 3; //TODO find correct value
		double cellSize = 7.5;  //TODO find correct value
		double flowCapPerLane = 0.5; // vehicle per second and lane

		NetworkLayer net = new NetworkLayer();
		net.setEffectiveCellSize(cellSize);
		net.setEffectiveLaneWidth(laneWidth);
		FeatureSource fs = ShapeFileReader.readDataFile(nodes);
		Iterator it = fs.getFeatures().iterator();
		while(it.hasNext()) {
			Feature f = (Feature) it.next();
			MultiPolygon geo = (MultiPolygon) f.getDefaultGeometry();
			Coordinate c = geo.getGeometryN(0).getCentroid().getCoordinate();
			Coord coord = MGC.coordinate2Coord(c);
			Integer id = (Integer) f.getAttribute(1);
			Node n = net.createAndAddNode(new IdImpl(id), coord);
			if (n == null) {
				System.out.println("id:" + id);
			}
		}
		System.out.println(net.getNodes().size());
		fs = ShapeFileReader.readDataFile(links);
		it = fs.getFeatures().iterator();

		Collection<Feature> fts = new ArrayList<Feature>();
		while(it.hasNext()) {

			Feature f = (Feature) it.next();
			Polygon p = (Polygon) ((MultiPolygon)f.getDefaultGeometry()).getGeometryN(0);
			f.setDefaultGeometry(p);
			fts.add(f);
			Integer id = (Integer) f.getAttribute("ID");
			Integer from = (Integer) f.getAttribute("from");
			Integer to = (Integer) f.getAttribute("to");
			Double length = (Double) f.getAttribute("length");
			Double minWidth = (Double) f.getAttribute("min_width");

			double lanes = minWidth/laneWidth/2;
			double flowCap = lanes * flowCapPerLane;

			Node nFrom = net.getNodes().get(new IdImpl(Integer.toString(from)));
			Node nTo = net.getNodes().get(new IdImpl(Integer.toString(to)));
			if (nTo == null    || nFrom == null) {
				int i = 0;
				i++;
				continue;
			}

			net.createAndAddLink(new IdImpl(id), nFrom, nTo, length, freespeed, flowCap, Math.max(lanes,1));
			net.createAndAddLink(new IdImpl(id+100000), nTo, nFrom, length, freespeed, flowCap, Math.max(lanes,1));
		}

		new NetworkCleaner().run(net);

		new NetworkWriter(net).write("./network.xml");

		ShapeFileWriter.writeGeometries(fts, "");
	}

}
