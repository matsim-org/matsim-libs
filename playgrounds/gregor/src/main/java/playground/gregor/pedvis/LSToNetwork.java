/* *********************************************************************** *
 * project: org.matsim.*
 * LSToNetwork.java
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
package playground.gregor.pedvis;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * @author laemmel
 * 
 */
public class LSToNetwork {

	public static void main(String[] args) throws IOException {
		String inFile = "/home/laemmel/devel/dfg/data/90gradNetwork.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(inFile);
		ScenarioImpl sc = new ScenarioImpl();
		NetworkImpl net = sc.getNetwork();
		makeNodes(net, fs);
		makeLinks(net, fs);

		new NetworkWriter(net).write("/home/laemmel/devel/dfg/data/90gradNetwork.xml");
	}

	/**
	 * @param net
	 * @param fs
	 * @throws IOException
	 */
	private static void makeLinks(NetworkImpl net, FeatureSource fs) throws IOException {
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			long intIdFrom = (Long) ft.getAttribute("fromID");
			long intIdTo = (Long) ft.getAttribute("toID");
			long intId = (Long) ft.getAttribute("ID");
			Id from = new IdImpl(intIdFrom);
			Id to = new IdImpl(intIdTo);
			Id id = new IdImpl(intId);
			Id id2 = new IdImpl(intId + 100);
			Node fromN = net.getNodes().get(from);
			Node toN = net.getNodes().get(to);
			LineString ls = ((LineString) ((MultiLineString) ft.getDefaultGeometry()).getGeometryN(0));
			net.createAndAddLink(id, fromN, toN, ls.getLength(), 1.34, 1, 1);
			// net.createAndAddLink(id2, toN, fromN, ls.getLength(), 1.34, 1,
			// 1);
		}

	}

	/**
	 * @param net
	 * @param fs
	 * @throws IOException
	 */
	private static void makeNodes(NetworkImpl net, FeatureSource fs) throws IOException {
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			long intIdFrom = (Long) ft.getAttribute("fromID");
			long intIdTo = (Long) ft.getAttribute("toID");
			Id from = new IdImpl(intIdFrom);
			if (net.getNodes().get(from) == null) {
				net.createAndAddNode(from, MGC.point2Coord(((LineString) ((MultiLineString) ft.getDefaultGeometry()).getGeometryN(0)).getStartPoint()));
			}
			Id to = new IdImpl(intIdTo);
			if (net.getNodes().get(to) == null) {
				net.createAndAddNode(to, MGC.point2Coord(((LineString) ((MultiLineString) ft.getDefaultGeometry()).getGeometryN(0)).getEndPoint()));
			}
		}

	}
}
