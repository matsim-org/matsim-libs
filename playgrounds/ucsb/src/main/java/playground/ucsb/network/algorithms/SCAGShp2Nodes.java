/* *********************************************************************** *
 * project: org.matsim.*
 * SCAGShp2Nodes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.network.algorithms;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author balmermi
 *
 */
public class SCAGShp2Nodes implements NetworkRunnable {
	
	private final static Logger log = Logger.getLogger(SCAGShp2Nodes.class);
	
	private final String nodeShpFile;
	private final ObjectAttributes nodeObjectAttributes;
	
	// for node attribute description see "SCAG TransCAD Regional Model Users Guide.pdf" page 107
	private static final String ID_NAME = "ID";
	private static final String ZONE_CENTR = "ZONE_CENTR";
	private static final String NODE_TYPE = "NODE_TYPE";
	private static final String METROLINK_NODE = "METROLINK_";
	private static final String URBAN_RAIL_NODE = "URBAN_RAIL";
	
	/**
	 * @param nodeShpFile
	 * @param nodeObjectAttributes
	 */
	public SCAGShp2Nodes(String nodeShpFile, ObjectAttributes nodeObjectAttributes) {
		this.nodeShpFile = nodeShpFile;
		this.nodeObjectAttributes = nodeObjectAttributes;
	}

	@Override
	public void run(Network network) {
		log.info("creating nodes from "+nodeShpFile+" shape file...");
		int fCnt = 0;
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(nodeShpFile)) {
			fCnt++;
			
			// node id
			Object id = f.getAttribute(ID_NAME);
			if (id == null) { throw new RuntimeException("fCnt "+fCnt+": "+ID_NAME+" not found in feature."); }
			Id<Node> nodeId = Id.create(id.toString().trim(), Node.class);

			// ignore node if it is a zone centroid
			String zoneCentr = f.getAttribute(ZONE_CENTR).toString().trim(); // "Y" := centroid ; "" := not a centroid 
			if (zoneCentr.equals("Y")) { continue; }

			// get coord
			Coordinate c = new Coordinate((f.getBounds().getMinX() + f.getBounds().getMaxX())/2.0, (f.getBounds().getMinY() + f.getBounds().getMaxY())/2.0);

			// add node
			Node n = network.getFactory().createNode(nodeId, new Coord(c.x, c.y));
			network.addNode(n);

			// node type
			int nodeType = Integer.parseInt(f.getAttribute(NODE_TYPE).toString().trim()); // range = [0-5]
			nodeObjectAttributes.putAttribute(nodeId.toString(),NODE_TYPE,nodeType);
			
			// metro link station
			boolean isMetroLinkStation = false;
			if (Integer.parseInt(f.getAttribute(METROLINK_NODE).toString().trim()) == 1) { isMetroLinkStation = true; }
			nodeObjectAttributes.putAttribute(nodeId.toString(),METROLINK_NODE,isMetroLinkStation);
			
			// urban rail station
			boolean isUrbanRailStation = false;
			if (Integer.parseInt(f.getAttribute(URBAN_RAIL_NODE).toString().trim()) == 1) { isUrbanRailStation = true; }
			nodeObjectAttributes.putAttribute(nodeId.toString(),URBAN_RAIL_NODE,isUrbanRailStation);
		}
		log.info("done. (creating nodes)");
	}

}
