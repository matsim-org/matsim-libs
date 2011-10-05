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

package playground.network.algorithms;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author balmermi
 *
 */
public class SCAGShp2Nodes implements NetworkRunnable {
	
	private final static Logger log = Logger.getLogger(SCAGShp2Nodes.class);
	
	private final String nodeShpFile;
	private final ObjectAttributes nodeObjectAttributes;
	
	private static final String ID_NAME = "ID";

	/**
	 * @param nodeShpFile
	 * @param nodeObjectAttributes
	 */
	public SCAGShp2Nodes(String nodeShpFile, ObjectAttributes nodeObjectAttributes) {
		this.nodeShpFile = nodeShpFile;
		this.nodeObjectAttributes = nodeObjectAttributes;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.internal.NetworkRunnable#run(org.matsim.api.core.v01.network.Network)
	 */
	@Override
	public void run(Network network) {
		log.info("creating nodes from "+nodeShpFile+" shape file...");
		int fCnt = 0;
		FeatureSource fs = ShapeFileReader.readDataFile(nodeShpFile);
		try {
			for (Object o : fs.getFeatures()) {
				Feature f = (Feature)o;
				fCnt++;
				
				Object id = f.getAttribute(ID_NAME);
				if (id == null) { throw new RuntimeException("fCnt "+fCnt+": "+ID_NAME+" not found in feature."); }
				Id nodeId = new IdImpl(id.toString().trim());
				
				Coordinate c = f.getBounds().centre();
				Node n = network.getFactory().createNode(nodeId, new CoordImpl(c.x,c.y));
				network.addNode(n);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("fCnt "+fCnt+": IOException while parsing "+nodeShpFile+".");
		}
		log.info("done. (creating nodes)");
	}

}
