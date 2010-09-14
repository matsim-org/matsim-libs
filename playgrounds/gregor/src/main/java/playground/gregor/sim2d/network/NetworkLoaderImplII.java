/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLoaderImplII.java
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
package playground.gregor.sim2d.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class NetworkLoaderImplII implements NetworkLoader{
	
	private static final String shapefile = "../../../../sim2d/sg4graph.shp";
	private static final double RANGE = 0.25;
	
	
	private boolean initialized = false;
	private QuadTree<NodeInfo> quad;
	private NetworkImpl net;

	public NetworkLoaderImplII(NetworkImpl net) {
		this.net = net;
	}
	
	
	
	@Override
	public Map<MultiPolygon, List<Link>> getFloors() {
		if (!this.initialized) {
			try {
				init();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}




	private void init() throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(shapefile);
		Envelope e = fs.getBounds();
		this.quad = new QuadTree<NodeInfo>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature next = (Feature) it.next();
			LineString ls = (LineString)((MultiLineString) next.getDefaultGeometry()).getGeometryN(0);
			handleLs(ls,ls.getStartPoint().getCoordinate());
			handleLs(null,ls.getEndPoint().getCoordinate());
		}
		createNodes();
		createLinks();
		this.initialized = true;
		
	}




	private void createLinks() {
		int count = 0;
		for (NodeInfo ni : this.quad.values()) {
			for (LineString ls : ni.links) {
				Node n1 = this.net.getNearestNode(MGC.point2Coord(ls.getStartPoint()));
				Node n2 = this.net.getNearestNode(MGC.point2Coord(ls.getEndPoint()));
				
				Id id1 = new IdImpl(count++);
				this.net.createAndAddLink(id1, n1, n2, ls.getLength(), 13.4, 1, 1);
				
				Id id2 = new IdImpl(count++);
				this.net.createAndAddLink(id2, n2, n1, ls.getLength(), 13.4, 1, 1);
			}
			
			
			
		}
		
	}



	private void createNodes() {
		int count = 0;
		for (NodeInfo ni : this.quad.values()) {
			Id id = new IdImpl(count++);
			this.net.createAndAddNode(id, MGC.coordinate2Coord(ni.c));
		}
			
	}




	private void handleLs(LineString ls, Coordinate c) {
		Collection<NodeInfo> coll = quad.get(c.x,c.y, RANGE);
		NodeInfo ni  = null;
		if (coll.size() == 0) {
			ni = new NodeInfo();
			ni.c = c;
			quad.put(c.x,c.y,ni);
		} else if (coll.size() > 1) {
			throw new RuntimeException("there seems to be more than one node at a location!!");
		} else {
			ni = coll.iterator().next();
		}
		if (ls != null) {
			ni.links.add(ls);
		}
	}




	@Override
	public Network loadNetwork() {
		if (!this.initialized) {
			try {
				init();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return this.net;
	}

	private static class NodeInfo {
		List<LineString> links = new ArrayList<LineString>();
		Coordinate c;
	}

}
