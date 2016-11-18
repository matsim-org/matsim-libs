/* *********************************************************************** *
 * project: org.matsim.*
 * WasteClustering.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.southafrica.projects.wasteGen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.freight.digicore.algorithms.concaveHull.ConcaveHull;
import playground.southafrica.freight.digicore.algorithms.djcluster.DJCluster;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.ClusterActivity;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;
import playground.southafrica.utilities.Header;

/**
 * Parsing a given (City of Cape Town) waste collection vehicle's GPS trace 
 * file, and generating 'service area' polygons from it.
 *
 * @author jwjoubert
 */
public class WasteClustering {
	final private static Logger LOG = Logger.getLogger(WasteClustering.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(WasteClustering.class.toString(), args);
		
		String traceFile = args[0];
		String outputFile = args[1];
		double eps = Double.parseDouble(args[2]);
		int pmin = Integer.parseInt(args[3]);
		
		WasteClustering wc = new WasteClustering();
		List<Coord> coords = wc.parseCoordsFromFile(traceFile, "WGS84", "WGS84_SA_Albers");
		List<Geometry> geometries = wc.clusterCoordsIntoGeometries(coords, eps, pmin, 20.0);		
		wc.writeGeometriesToFileForR(geometries, outputFile, "WGS84_SA_Albers");
		
		Header.printFooter();
	}
	
	public WasteClustering() {
		// TODO Auto-generated constructor stub
	}
	
	public void writeGeometriesToFileForR(List<Geometry> geometries, String filename, String projectedCRS){
		LOG.info("Write geometries to file...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(projectedCRS, "WGS84");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("gid,pid,lon,lat,x,y");
			bw.newLine();
			int g = 1;
			for(Geometry geometry : geometries){
				int i = 1;
				for(Coordinate c : geometry.getCoordinates()){
					Coord coord = ct.transform(CoordUtils.createCoord(c.x, c.y));
					bw.write(String.format("%d,%d,%.6f,%.6f,%.0f,%.0f\n", g, i, coord.getX(), coord.getY(), c.x, c.y));
					i++;
				}
				g++;
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

		LOG.info("Done writing geometries.");
	}
	
	public List<Geometry> clusterCoordsIntoGeometries(List<Coord> coords, double epsilon, int pmin, double concavityLength){
		LOG.info("Clustering points...");
		
		/* Cluster the points. */
		DJCluster djc = new DJCluster(coords, true);
		djc.clusterInput(epsilon, pmin);
		LOG.info("Done clustering points (" + djc.getClusterList().size() + " clusters)");
		
		/* For each cluster, find the concave hull. */
		GeometryFactory gf = new GeometryFactory();
		List<Geometry> geometries = new ArrayList<>();
		for(DigicoreCluster cluster : djc.getClusterList()){
			List<Point> listOfPoints = new ArrayList<>();
			for(ClusterActivity ca : cluster.getPoints()){
				listOfPoints.add(gf.createPoint(new Coordinate(ca.getCoord().getX(), ca.getCoord().getY())));
			}
			Point[] pa = new Point[listOfPoints.size()];
			pa = listOfPoints.toArray(pa);
			
			GeometryCollection points = new GeometryCollection(pa, gf);
			ConcaveHull ch = new ConcaveHull(points, concavityLength);
			geometries.add(ch.getConcaveHull());
		}
		
		return geometries;
	}
	
	public List<Coord> parseCoordsFromFile(String filename, String originCRS, String finalCRS){
		LOG.info("Parsing coordinates from waste GPS trace file...");
		List<Coord> list = new ArrayList<>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(originCRS, finalCRS);
		try{
			String line = br.readLine();
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String vid = sa[1];
				String date = sa[2];
				String time = sa[3];
				double lon = Double.parseDouble(sa[4]);
				double lat = Double.parseDouble(sa[5]);
				
				Coord cAlbers = ct.transform(CoordUtils.createCoord(lon, lat));
				list.add(cAlbers);
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
		
		LOG.info("Done parsing (" + list.size() + " points)");
		return list;
	}

}
