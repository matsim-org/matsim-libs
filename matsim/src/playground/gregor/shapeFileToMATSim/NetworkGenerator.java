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

package playground.gregor.shapeFileToMATSim;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.identifiers.IdI;

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
		
		String from = getNode(ls.getStartPoint());
		String to  = getNode(ls.getEndPoint());
		this.network.createLink(Integer.toString(this.linkId++), from, to, Double.toString(ls.getLength()), "1.66", "1.33", "1", "0", null);
		this.network.createLink(Integer.toString(this.linkId++), to, from, Double.toString(ls.getLength()), "1.66", "1.33", "1", "0", null);
	}
	
	
	private String getNode(Point p) {
		Collection<Node> tmp = this.nodes.get(p.getX(), p.getY(), GISToMatsimConverter.CATCH_RADIUS);
		if (tmp.size() > 1) {
			throw new RuntimeException("two different nodes on the same location is not allowd!");
		} 
		if (tmp.size() == 0) {
			Node n = network.createNode(Integer.toString(this.nodeId++), Double.toString(p.getX()), Double.toString(p.getY()), "");
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
