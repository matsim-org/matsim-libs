/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
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

package playground.toronto.mapping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

public class NetworkCreateL2ZMapping {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(NetworkCreateL2ZMapping.class);
	private final String outfile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkCreateL2ZMapping(final String outfile) {
		this.outfile = outfile;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private QuadTree<Node> buildCentroidNodeQuadTree(final Map<Id,Node> nodes) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<Node> ns = new ArrayList<Node>();
		for (Node n : nodes.values()) {
			try {
				int nid = Integer.parseInt(n.getId().toString());
				if (nid < 10000) {
					ns.add(n);
					if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
					if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
					if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
					if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
				}
			} catch (NumberFormatException e) {
			}
//			if (Integer.parseInt(n.getId().toString()) < 10000) {
//				ns.add(n);
//				if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
//				if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
//				if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
//				if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
//			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		QuadTree<Node> qt = new QuadTree<Node>(minx,miny,maxx,maxy);
		for (Node n : ns) {
			qt.put(n.getCoord().getX(),n.getCoord().getY(),n);
		}
		return qt;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkImpl network) {
		QuadTree<Node> qt = buildCentroidNodeQuadTree(network.getNodes());
		log.info("# centroid nodes: "+qt.size());
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			for (Link l : network.getLinks().values()) {
				Node n = qt.get(l.getCoord().getX(),l.getCoord().getY());
				out.write(l.getId().toString()+"\t"+n.getId().toString()+"\n");
			}
			out.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
