/* *********************************************************************** *
 * project: org.matsim.*
 * NetworGenerator.java
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

package playground.gregor.gis.networkFromShape;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkFromLineStrings {


	private static final Logger log = Logger.getLogger(NetworkFromLineStrings.class);
	private FeatureSource features;
	private Envelope envelope;
//	private QuadTree<LineString> tree;
	private QuadTree<Node> nodes;
//	private HashSet<LineString> lineStrings;
	private NetworkImpl network;
	private int nodeId;
	private int linkId;

	static final double CATCH_RADIUS = 0.5;

	NetworkFromLineStrings(FeatureSource fs, Envelope envelope){
		this.features = fs;
		this.envelope = envelope;
	}


	public static void main(String [] args) throws IOException {
		String ls = "/home/laemmel/devel/sim2d/data/duisburg/d_ls.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(ls);
		Envelope e = fs.getBounds();
		NetworkImpl net = new NetworkFromLineStrings(fs, e).generateFromGraph();
		new NetworkWriter(net).write("/home/laemmel/devel/sim2d/data/duisburg/network.xml");

	}


	public NetworkImpl generateFromGraph() throws IOException {

		log.info("parsing features, building up NetworkLayer and running  NetworkCleaner as well ...");
		this.network = NetworkImpl.createNetwork();
		this.nodes = new QuadTree<Node>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());
		this.nodeId = 0;
		this.linkId = 0;

//		this.lineStrings = new HashSet<LineString>();
//		this.tree = new QuadTree<LineString>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());

		Iterator it = this.features.getFeatures().iterator();
		while (it.hasNext()) {
			Feature feature = (Feature)it.next();
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			if (multiLineString.getNumGeometries() > 1) {
				throw new RuntimeException();
			}
			LineString ls = (LineString) multiLineString.getGeometryN(0);
			processLineString(ls,feature);


		}

		NetworkCleaner nw = new NetworkCleaner();
		nw.run(this.network);
		log.info("done.");
		return this.network;
	}

	private void processLineString(LineString ls, Feature feature){


		Point sP = ls.getStartPoint();
		Point eP = ls.getEndPoint();

		Long fromID = (Long)feature.getAttribute("fromID");
		Collection<Node> tmpFrom = this.network.getNearestNodes(MGC.point2Coord(sP), CATCH_RADIUS);
		if (tmpFrom.size() > 1) {
			throw new RuntimeException();
		} else if (tmpFrom.size() == 1) {
			Node tmp = tmpFrom.iterator().next();
			if (!fromID.toString().equals(tmp.getId().toString())) {
				Point tmpP = sP;
				sP = eP;
				eP = tmpP;
			}
		}

		Long toID = (Long)feature.getAttribute("toID");
		Collection<Node> tmpTo = this.network.getNearestNodes(MGC.point2Coord(eP), CATCH_RADIUS);
		if (tmpTo.size() > 1) {
			throw new RuntimeException();
		} else if (tmpTo.size() == 1) {
			Node tmp = tmpTo.iterator().next();
			if (!toID.toString().equals(tmp.getId().toString())) {
				Point tmpP = sP;
				sP = eP;
				eP = tmpP;
			}
		}



		Node from = this.network.getNodes().get(new IdImpl(getNode(sP,fromID)));
		Node to  = this.network.getNodes().get(new IdImpl(getNode(eP,toID)));
		Long id = (Long) feature.getAttribute("ID");
		this.network.createAndAddLink(new IdImpl(id), from, to, ls.getLength(), 1.66, 1.33, 1);
		this.network.createAndAddLink(new IdImpl(id+100000), to, from, ls.getLength(), 1.66, 1.33, 1);
	}


	private String getNode(Point p, Long id) {
		Collection<Node> tmp = this.nodes.get(p.getX(), p.getY(),CATCH_RADIUS);
		if (tmp.size() > 1) {
			throw new RuntimeException("two different nodes on the same location is not allowd!");
		}
		if (tmp.size() == 0) {
			Node n = network.createAndAddNode(new IdImpl(id), new CoordImpl(p.getX(), p.getY()));
			addNode(n);
			return n.getId().toString();
		} else {
			return tmp.iterator().next().getId().toString();
		}
	}

	private void addNode(Node n){
		this.nodes.put(n.getCoord().getX(),n.getCoord().getY(),n);
	}

}
