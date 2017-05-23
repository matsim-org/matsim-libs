/* *********************************************************************** *
 * project: org.matsim.*
 * ComplexNetworkUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.projects.complexNetworks.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;

/**
 * A variety of (re)usable utilities that can be applied to a path-dependent
 * complex network.
 * 
 * @author jwjoubert
 */
public class ComplexNetworkUtils {
	final private static Logger LOG = Logger.getLogger(ComplexNetworkUtils.class);

	/**
	 * Reduces a given path-dependent complex network to only those nodes that
	 * are inside a given geometry, or that are connected to a nodes inside the
	 * geometry. As a result, the 'path-dependent-ness' of the network is lost
	 * after running this method. 
	 * 
	 * @param network the path-dependent network;
	 * @param geometry the area within which nodes are kept. <b>Note:</b> this
	 * 		  geometry must be in the same coordinate reference system than the
	 * 		  network.
	 * @return
	 */
	public static Map<Id<Node>, Map<Id<Node>, Double>> cleanupNetwork(PathDependentNetwork network, Geometry geometry){
		LOG.info("Reducing the path-dependent network to those connected to or inside geometry...");
		LOG.warn("The network's node coordinates must be in the same coordinate reference system than the given geometry!");
		LOG.info("Original network statistics:");
		network.writeNetworkStatisticsToConsole();

		GeometryFactory gf = new GeometryFactory();
		Geometry envelope = geometry.getEnvelope();

		Map<Id<Node>, Map<Id<Node>, Double>> original = network.getEdges();
		LOG.info("Total number of origin nodes to consider: " + original.size());
		Map<Id<Node>, Map<Id<Node>, Double>> reduced = new TreeMap<Id<Node>, Map<Id<Node>,Double>>();

		Counter counter = new Counter("  origin nodes # ");
		for(Id<Node> o : original.keySet()){
			Map<Id<Node>, Double> map = original.get(o);

			/* Check if the origin node is inside the geomtery. */
			PathDependentNode n1 = network.getPathDependentNode(o);
			Point p1 = gf.createPoint(new Coordinate(n1.getCoord().getX(), n1.getCoord().getY()));
			boolean is1Inside = false;
			if(envelope.contains(p1)){
				if(geometry.contains(p1)){
					is1Inside = true;
				}
			}

			if(is1Inside){
				/* Add all the nodes that are not already in the reduced map. */ 
				if(!reduced.containsKey(o)){
					reduced.put(o, map);
				} else{
					Map<Id<Node>, Double> existingMap = reduced.get(o);
					for(Id<Node> d : map.keySet()){
						if(!existingMap.containsKey(d)){
							existingMap.put(d, map.get(d));
						} else{
							double oldValue = map.get(d);
							existingMap.put(d, oldValue + map.get(d));
						}
					}
				}
			} else{
				for(Id<Node> d : map.keySet()){
					PathDependentNode n2 = network.getPathDependentNode(d);

					/* Check if destination node is inside geometry. */
					Point p2 = gf.createPoint(new Coordinate(n2.getCoord().getX(), n2.getCoord().getY()));
					boolean is2Inside = false;
					if(envelope.contains(p2)){
						if(geometry.contains(p2)){
							is2Inside = true;
						}
					}

					/* Add all the nodes that are not already in the reduced map. */ 
					if(is2Inside){
						if(!reduced.containsKey(o)){
							reduced.put(o, new HashMap<Id<Node>, Double>());
							reduced.get(o).put(d, map.get(d));
						} else{
							Map<Id<Node>, Double> existingMap = reduced.get(o);
							if(!existingMap.containsKey(d)){
								existingMap.put(d, map.get(d));
							} else{
								double oldValue = existingMap.get(d);
								existingMap.put(d, oldValue + map.get(d));
							}
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done reducing the path-dependent network.");
		return reduced;
	}


	/**
	 * Writes the weighted edge list of a network to file so that it can be 
	 * used in, for example, the iGraph library in R.
	 * 
	 * @param network the path-dependent network (this is needed only for the
	 * 		  node coordinates);
	 * @param edgeMap the weighted edge map (this may be the same as the one in
	 * 		  the given network, but may also be a reduced version); 
	 * @param filename absolute path to where the weighted edge list will be 
	 * 		  written to; and
	 * @param networkCRS the (projected) coordinate reference system of the
	 * 		  given network. FIXME In future this should be embedded inside the
	 * 		  network object.
	 */
	public static void writeWeightedEdgelistToFile(PathDependentNetwork network,
			Map<Id<Node>, Map<Id<Node>, Double>> edgeMap, String filename, 
			String networkCRS){
		LOG.info("Writing weighted edge list to file...");

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				networkCRS, TransformationFactory.WGS84);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("oId,oLon,oLat,oX,oY,dId,dLon,dLat,dX,dY,weight");
			bw.newLine();

			Iterator<Id<Node>> oIt = edgeMap.keySet().iterator();
			while(oIt.hasNext()){
				Id<Node> oId = oIt.next();
				Map<Id<Node>, Double> thisMap = edgeMap.get(oId);
				Iterator<Id<Node>> dIt = thisMap.keySet().iterator();
				while(dIt.hasNext()){
					Id<Node> dId = dIt.next();
					double weight = edgeMap.get(oId).get(dId);

					/* Get and convert the two nodes. */
					Coord oC = network.getPathDependentNode(oId).getCoord();
					Coord oCT = ct.transform(oC);
					Coord dC = network.getPathDependentNode(dId).getCoord();
					Coord dCT = ct.transform(dC);

					bw.write(String.format("%s,%.6f,%.6f,%.0f,%.0f,%s,%.6f,%.6f,%.0f,%.0f,%f\n", 
							oId.toString(),
							oCT.getX(),
							oCT.getY(),
							oC.getX(),
							oC.getY(),
							dId.toString(),
							dCT.getX(),
							dCT.getY(),
							dC.getX(),
							dC.getY(),
							weight));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing degree values to file.");
	}

}
