/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.algorithms.concaveHull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.freight.digicore.algorithms.djcluster.DJCluster;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.ClusterActivity;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;
import playground.southafrica.utilities.Header;

/**
 * Class to run the clustering and concave hull algorithms for the paper by
 * Joubert & Meintjes intended for the Transportation Special Issue on 
 * Frontiers in Transportation.
 *
 * @author jwjoubert
 */
public class PaperPrepper {

	public static void main(String[] args) {
		Header.printHeader(PaperPrepper.class.toString(), args);
		String inputFolder = args[0];
		double radius = Double.parseDouble(args[1]);
		int minimumPoints = Integer.parseInt(args[2]);
		double threshold = Double.parseDouble(args[3]);
		
		List<Coord> coords = readCoordinates(inputFolder + "Points.csv");
		
		/* Cluster the points. */
		DJCluster djc = new DJCluster(coords, false);
		djc.clusterInput(radius, minimumPoints);
		
		/* Write out the unclustered points. */
		String lostFile = String.format("%s%.0f_%d_%.0f_lost.csv", inputFolder, radius, minimumPoints, threshold);
		BufferedWriter bw1 = IOUtils.getBufferedWriter(lostFile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		try{
			bw1.write("Long,Lat");
			bw1.newLine();
			for(ClusterActivity ca : djc.getLostPoints().values()){
				Coord cc = ct.transform(ca.getCoord());
				bw1.write(String.format("%f, %f\n", ca.getCoord().getX(), ca.getCoord().getY()));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to " + lostFile);
		} finally{
			try {
				bw1.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close " + lostFile);
			}
		}
		
		/* Determine the concave hull for each cluster. */
		GeometryCollection points = null;
		GeometryFactory gf = new GeometryFactory();
		for(DigicoreCluster cluster : djc.getClusterList()){
			List<Point> pointList = new ArrayList<Point>();
			for(ClusterActivity ca : cluster.getPoints()){
				pointList.add(gf.createPoint(new Coordinate(ca.getCoord().getX(), ca.getCoord().getY())));
			}
			
			Point[] pa = new Point[pointList.size()];
			for(int i = 0; i < pointList.size(); i++){
				pa[i] = pointList.get(i);
			}
			points = gf.createGeometryCollection(pa);
			ConcaveHull ch = new ConcaveHull(points , threshold);
			Coordinate[] hull = ch.getConcaveHull(cluster.getId().toString()).getCoordinates();
			
			String hullFile = String.format("%s%.0f_%d_%.0f_hull_%s.csv", inputFolder, radius, minimumPoints, threshold, cluster.getId().toString());
			BufferedWriter bw2 = IOUtils.getBufferedWriter(hullFile);
			try{
				bw2.write("Long,Lat");
				bw2.newLine();
				for(Coordinate c : hull){
					bw2.write(String.format("%f, %f\n", c.x, c.y));
				}
			} catch (IOException e) {
				throw new RuntimeException("Could not write to " + hullFile);
			} finally{
				try {
					bw2.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close " + hullFile);
				}
			}
		}
		
		Header.printFooter();
	}

	private static List<Coord> readCoordinates(String filename) {
		List<Coord> list = new ArrayList<Coord>();
		
		/* Set up the coordinate transformation factory. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); // Header
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Coord c = new Coord(Double.parseDouble(sa[1]), Double.parseDouble(sa[0]));
				list.add(ct.transform(c));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		return list;
	}

}
