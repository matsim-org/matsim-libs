/* *********************************************************************** *
 * project: org.matsim.*
 * ThresholdDigiScorer.java                                                                        *
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
/**
 * 
 */
package playground.southafrica.projects.digicore.scoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.utilities.Header;

/**
 * This class aims to calculate the risk by applying the fixed thresholds 
 * proposed in literature. More specifically, by the following three papers:
 * 
 * <ol>
 * 	<li> Baldwin, K.C., Duncan, D.D. and West, S.K. (2004). The driver monitor 
 *       system: A means of assessing driver performance. John Hopkins APL 
 *       Technical Digest, 25(3):269-277.
 * 	<li> Bergasa, L.M., Almeria, D., Almazan, J., Yebes, J.J. and Arroyo, R. 
 *       (2014). DriveSafe: an app for alerting inattentive drivers and scoring 
 *       driving behaviours. In IEEE Intelligent Vehicles Symposium (IV), 
 *       pages 240-245.
 * 	<li> Paefgen, K., Kehr, F., Zhai, Y. and Michahelles, F. (2012). Driving 
 *       behavior analysis with smartphones: Insights from a controlled field 
 *       study. In Proceedings of the 11th International Conference on Mobile 
 *       and Ubiquitous Multimedia, MUM '12, pages 36:1-36:8.
 * </ol>
 * 
 * @author jwjoubert
 */
public class ThresholdDigiScorer implements DigiScorer {
	final private GeometryFactory gf = new GeometryFactory();
	final private boolean isLogging = false;
	final private Polygon BaldwinPolygon;
	final private Polygon BergasaPolygon;
	final private Polygon PaefgenPolygon;

	public ThresholdDigiScorer() {
		/* Create the three polygons. */
		Coordinate c11 = new Coordinate(-150.0, -150.0);
		Coordinate c12 = new Coordinate(+150.0, -150.0);
		Coordinate c13 = new Coordinate(+150.0, +150.0);
		Coordinate c14 = new Coordinate(-150.0, +150.0);
		Coordinate[] ca1 = {c11, c12, c13, c14, c11};
		BaldwinPolygon = gf.createPolygon(ca1);

		Coordinate c21 = new Coordinate(-400.0, -400.0);
		Coordinate c22 = new Coordinate(+400.0, -400.0);
		Coordinate c23 = new Coordinate(+400.0, +400.0);
		Coordinate c24 = new Coordinate(-400.0, +400.0);
		Coordinate[] ca2 = {c21, c22, c23, c24, c21};
		BergasaPolygon = gf.createPolygon(ca2);
		
		Coordinate c31 = new Coordinate(-100.0, -200.0);
		Coordinate c32 = new Coordinate(+100.0, -200.0);
		Coordinate c33 = new Coordinate(+100.0, +200.0);
		Coordinate c34 = new Coordinate(-100.0, +200.0);
		Coordinate[] ca3 = {c31, c32, c33, c34, c31};
		PaefgenPolygon = gf.createPolygon(ca3);
	}

	@Override
	public void buildScoringModel(String filename) {
		/* Read in the data and sort the records per person. */
		
		
	}

	@Override
	public void rateIndividuals(String filename, String outputFolder) {
		LOG.info("Rating individuals...");
		Map<String, Map<Long, String>> map = new HashMap<>();
		
		/* Build the "big" map containing all records. */
		LOG.info("Parsing all records:");
		Counter counter = new Counter("  records # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[1];
				long time = Long.parseLong(sa[2]);
				if(!map.containsKey(id)){
					map.put(id, new TreeMap<Long, String>());
				}
				map.get(id).put(time, line);
				counter.incCounter();
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
		counter.printCounter();
		
		/* Estimate distance travelled for each user. */
		Map<String, Double> distanceMap = this.estimateDistance(map);
		
		/* Check the number of risky points for each record. */
		Map<String, Integer[]> riskMap = this.evaluateRisk(map);
		
		/* Now write the output. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "thresholdCounts.csv");
		try{
			bw.write("id,km,total,baldwin,bergasa,paefgen");
			bw.newLine();
			for(String id : distanceMap.keySet()){
				bw.write(String.format("%s,%.2f,%d,%d,%d,%d\n", 
						id,
						distanceMap.get(id)/1000.0,
						map.get(id).size(),
						riskMap.get(id)[0],
						riskMap.get(id)[1],
						riskMap.get(id)[2] ));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to output.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close writer.");
			}
		}
		
	}

	private Map<String, Integer[]> evaluateRisk(Map<String, Map<Long, String>> map) {
		LOG.info("Evaluate the risk for each user.");
		Counter counter = new Counter("  user # ");
		Map<String, Integer[]> riskMap = new HashMap<>(map.size());
		for(String id : map.keySet()){
			Integer[] ia = {0,0,0};
			for(String record : map.get(id).values()){
				String[] sa = record.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				Coord c = CoordUtils.createCoord(x, y);
				
				if(isBaldwinRisky(c)){
					ia[0] += 1;
				}
				if(isBergasaRisky(c)){
					ia[1] += 1;
				}
				if(isPaefgenRisky(c)){
					ia[2] += 1;
				}
			}
			riskMap.put(id, ia);
			counter.incCounter();
			if(isLogging) LOG.info("id: " + id + " [" + ia[0] + ";" + ia[1] + ";" + ia[2] + "]");
		}
		counter.printCounter();
		
		LOG.info("Done evaluating the risk.");
		return riskMap;
	}

	/* (non-Javadoc)
	 * @see playground.southafrica.projects.digicore.scoring.DigiScorer#getRiskGroup(java.lang.String)
	 */
	@Override
	public RISK_GROUP getRiskGroup(String record) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private boolean isBaldwinRisky(Coord c){
		return !BaldwinPolygon.covers(convertCoordToPoint(c));
	}

	private boolean isBergasaRisky(Coord c){
		return !BergasaPolygon.covers(convertCoordToPoint(c));
	}
	
	private boolean isPaefgenRisky(Coord c){
		return !PaefgenPolygon.covers(convertCoordToPoint(c));
	}
	
	private Geometry convertCoordToPoint(Coord c){
		return gf.createPoint(new Coordinate(c.getX(), c.getY()));
	}
	
	
	
	private Map<String, Double> estimateDistance(Map<String, Map<Long, String>> map){
		LOG.info("Estimating distance for each user.");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		Counter counter = new Counter("  user # ");
		Map<String, Double> distanceMap = new HashMap<>(map.size());
		
		for(String id : map.keySet()){
			Iterator<String> records = map.get(id).values().iterator();
			double distance = 0.0;

			/* Get the first coordinate. */
			String[] sa = records.next().split(",");
			Coord c = CoordUtils.createCoord(Double.parseDouble(sa[3]), Double.parseDouble(sa[4]));
			Coord lastCoord = ct.transform(c);
			
			/* Calculate the distance to each consecutive coordinate. */
			while(records.hasNext()){
				String record = records.next();
				sa = record.split(",");
				c = CoordUtils.createCoord(Double.parseDouble(sa[3]), Double.parseDouble(sa[4]));
				Coord coord = ct.transform(c);
				distance += CoordUtils.calcEuclideanDistance(lastCoord, coord);
				lastCoord = CoordUtils.createCoord(coord.getX(), coord.getY());
			}
			distanceMap.put(id, distance);
			if(isLogging) LOG.info("Distance travelled by " + id + ": " + distance/1000.0 + "km");
			counter.incCounter();
		}
		counter.printCounter();
		return distanceMap;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ThresholdDigiScorer.class.toString(), args);
		
		String inputFile = args[0];
		String outputfolder = args[1];
		outputfolder += outputfolder.endsWith("/") ? "" : "/";
		
		ThresholdDigiScorer tds = new ThresholdDigiScorer();
		tds.rateIndividuals(inputFile, outputfolder);
		
		Header.printFooter();
	}

}
