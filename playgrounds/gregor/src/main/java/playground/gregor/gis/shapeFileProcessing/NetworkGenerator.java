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

package playground.gregor.gis.shapeFileProcessing;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkGenerator {


	private static final Logger log = Logger.getLogger(NetworkGenerator.class);
	private Collection<Feature> features;
	private Envelope envelope;
//	private QuadTree<LineString> tree;
	private QuadTree<Node> nodes;
//	private HashSet<LineString> lineStrings;
	private NetworkLayer network;
	private int nodeId;
	private int linkId;

	public NetworkGenerator(Collection<Feature> features, Envelope envelope){
		this.features = features;
		this.envelope = envelope;
	}


//	public NetworkLayer generateFromGraph() throws Exception {
//		parseLineStrings();
//		return processParsed();
//
//	}
//	private NetworkLayer processParsed() {
//		NetworkLayer network = new NetworkLayer();
//		this.nodes = new QuadTree<Node>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());
//		int nodeId = 0;
//		int linkId = 0;
//		for (LineString ls : this.lineStrings){
//			Collection<Node> tmp = this.nodes.get(ls.getStartPoint().getX(), ls.getStartPoint().getY(), GISToMatsimConverter.CATCH_RADIUS);
//			String from;
//			String to;
//			if (tmp.size() > 1) {
//				throw new RuntimeException("two different nodes on the same location is not allowd!");
//			}
//			if (tmp.size() == 0) {
//				Node n = network.createNode(Integer.toString(nodeId++), Double.toString(ls.getStartPoint().getX()), Double.toString(ls.getStartPoint().getY()), "");
//				addNode(n);
//				from = n.getId().toString();
//			} else {
//				from = tmp.iterator().next().getId().toString();
//			}
//
//			tmp = this.nodes.get(ls.getEndPoint().getX(), ls.getEndPoint().getY(), GISToMatsimConverter.CATCH_RADIUS);
//			if (tmp.size() > 1) {
//				throw new RuntimeException("two different nodes on the same location is not allowd!");
//			}
//			if (tmp.size() == 0) {
//				Node n = network.createNode(Integer.toString(nodeId++), Double.toString(ls.getEndPoint().getX()), Double.toString(ls.getEndPoint().getY()), "");
//				addNode(n);
//				to = n.getId().toString();
//			}	else {
//				to = tmp.iterator().next().getId().toString();
//			}
//			network.createLink(Integer.toString(linkId++), from, to, Double.toString(ls.getLength()), "1.66", "1.33", "1", "0", null);
//		}
//
//		return network;
//	}


	public NetworkLayer generateFromGraph() throws IOException {

		log.info("parsing features, building up NetworkLayer and running  NetworkCleaner as well ...");
		this.network = new NetworkLayer();
		this.nodes = new QuadTree<Node>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());
		this.nodeId = 0;
		this.linkId = 0;

//		this.lineStrings = new HashSet<LineString>();
//		this.tree = new QuadTree<LineString>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());

		for (Feature feature : this.features) {
			MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				LineString ls = (LineString) multiLineString.getGeometryN(i);
				processLineString(ls);
			}

		}

		NetworkCleaner nw = new NetworkCleaner();
		nw.run(this.network);
		log.info("done.");
		return this.network;
	}

	private void processLineString(LineString ls){

		Node from = this.network.getNodes().get(new IdImpl(getNode(ls.getStartPoint())));
		Node to  = this.network.getNodes().get(new IdImpl(getNode(ls.getEndPoint())));
		this.network.createAndAddLink(new IdImpl(this.linkId++), from, to, ls.getLength(), 1.66, 1.33, 1);
		this.network.createAndAddLink(new IdImpl(this.linkId++), to, from, ls.getLength(), 1.66, 1.33, 1);
	}


	private String getNode(Point p) {
		Collection<Node> tmp = this.nodes.get(p.getX(), p.getY(), GISToMatsimConverter.CATCH_RADIUS);
		if (tmp.size() > 1) {
			throw new RuntimeException("two different nodes on the same location is not allowd!");
		}
		if (tmp.size() == 0) {
			Node n = network.createAndAddNode(new IdImpl(this.nodeId++), new CoordImpl(p.getX(), p.getY()));
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
