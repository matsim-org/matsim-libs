/* *********************************************************************** *
 * project: org.matsim.*
 * RunM2U_eThekwini.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class RunM2U {
	private static final Logger log = Logger.getLogger(RunM2U.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int numberOfArguments = 6;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		}
		
		/*
		 * First get the private car travel time. Once I have this, I simply 
		 * can convert it to public transport travel times. 
		 */
		CarTimeEstimator cte = new CarTimeEstimator(args[0], args[1], args[2], args[3]);
		cte.estimateCarTime(args[4], args[5], false);
		DenseDoubleMatrix2D odMatrix = cte.getOdMatrix();
		List<MyZone> zones = cte.getZones();
		
		/*
		 * Next, get the public transport network.
		 */
		Scenario sPt = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(sPt);
		nr.readFile(cte.sb.getPtNetworkFilename());
		
		/*
		 * Calculate, for each zone, the distance to the closest transit node.
		 */
		log.info("Calculating the distance to the closest transit node.");
		NetworkImpl nPt = (NetworkImpl) sPt.getNetwork();
		Map<Id,Double> distanceToPt = new TreeMap<Id, Double>();
		GeometryFactory gf = new GeometryFactory();
		for(MyZone z : zones){
			NodeImpl c = new NodeImpl(new IdImpl("dummy"));
			c.setCoord(new CoordImpl(z.getCentroid().getX(), z.getCentroid().getY()));
			Node n = nPt.getNearestNode(c.getCoord());
			if(n != null){
				Point p1 = gf.createPoint(new Coordinate(c.getCoord().getX(), c.getCoord().getY()));
				Point p2 = gf.createPoint(new Coordinate(n.getCoord().getX(), n.getCoord().getY()));
				distanceToPt.put(z.getId(), p1.distance(p2));
			} else{
				log.warn("Could not get nearest transit node for zone " + z.getId());
			}
		}

		/*
		 * Process the odMatrix, and write the entries to file.
		 */
		MyConverter mc = new MyConverter(args[1]);
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(cte.sb.getTravelDataFilename());
			try{
				bw.write("fromZone,toZone,carTime,walkTo,ptTime,walkFrom");
				bw.newLine();
				int total = odMatrix.rows()*odMatrix.columns();
				int counter = 0;
				int multiplier = 1;
				for(int row = 0; row < odMatrix.rows(); row++){
					for(int col = 0; col < odMatrix.columns(); col++){
						Id fromId = cte.getMatrixToZoneMap().get(row);
						Id toId = cte.getMatrixToZoneMap().get(col);
						bw.write(fromId.toString());
						bw.write(",");
						bw.write(toId.toString());
						bw.write(",");
						double carTime = odMatrix.get(row, col) / 60;
						bw.write(String.valueOf(carTime));
						bw.write(",");
						double walkTo = mc.convertWalkDistanceToWalkTime(distanceToPt.get(fromId)) / 60;
						bw.write(String.valueOf(walkTo));
						bw.write(",");
						double ptTime = mc.convertCarTimeToPtTime(carTime) / 60;
						bw.write(String.valueOf(ptTime));
						bw.write(",");
						double walkFrom = mc.convertWalkDistanceToWalkTime(distanceToPt.get(toId)) / 60;
						bw.write(String.valueOf(walkFrom));
						bw.newLine();
						
						// Report progress.
						if(++counter == multiplier){
							double percentage = (((double) counter) / ((double) total))*100;
							log.info(String.format("   matrix entries completed: %d (%3.2f%%)", counter, percentage));
							multiplier *= 2;
						}
					}
				}
				log.info(String.format("   matrix entries completed: %d (Done)", counter));
				
				
			} finally {
				bw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("------------------------------------------------------");
		log.info("  Process complete.");
		log.info("======================================================");
	}

}

