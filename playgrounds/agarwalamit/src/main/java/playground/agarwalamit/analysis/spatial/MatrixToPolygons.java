/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.spatial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p> 1) Read the pre-existing parameter (emission, delays, toll etc.) and weights (population density, toll payers etc) file in matrix format.
 * <p> 2) Get the centroid from matrix and create polygons depending on bounding box and number of x and y bins.
 * <p> 3) Store polygon coordinates, centroid coordinates, parameter and weight in a file to plot it.
 * @author amit
 */

public class MatrixToPolygons {
	
	private final static int X_BINS = 160;
	private final static int Y_BINS = 120;
	
	private final static double X_MIN=4452550.25;
	private final static double X_MAX=4479483.33;
	private final static double Y_MIN=5324955.00;
	private final static double Y_MAX=5345696.81;


	private final static String OUT_FILE =  "/Users/amit/Downloads/bk/poly_centroid_toll_weight.txt";
	private final static String INPUT_FILE_TOLL = "/Users/amit/Downloads/bk/pricing.1500-baseCase.1500.absoluteDelta.Routput.AvgUserBenefitsDifferencesNoRefund.txt";
	private final static String INPUT_FILE_WEIGHT = "/Users/amit/Downloads/bk/baseCase.1500.Routput.UserBenefitsWeights.txt";
	

	public static void main(String[] args) {
		new MatrixToPolygons().writeRData();
	}
	
	private void writeRData(){
		
		GeometryFactory gf = new GeometryFactory();
		
		Map<Polygon, Double> poly2toll = new HashMap<>();
		Map<Polygon, Double> poly2weight = new HashMap<>();
		Map<Polygon, Coordinate> poly2Centroid = new HashMap<>();
		
		String [] [] arrayToll = readInputFile(INPUT_FILE_TOLL);
		String [] [] arrayWeight = readInputFile(INPUT_FILE_WEIGHT);
		
		//get xs and ys
		
		String xCoords [] = new String [X_BINS];
		System.arraycopy(arrayToll[0], 1, xCoords, 0, arrayToll[0].length - 1);
		String yCoords [] = new String [arrayToll.length-1];
 		for (int yIndex =1; yIndex < arrayToll.length;yIndex++){
			yCoords[yIndex-1] = arrayToll[yIndex][0];
		}
 		
 		// create polygons
 		for(int ii=1;ii<arrayToll.length;ii++){
 			for(int jj=1;jj<arrayToll[ii].length;jj++){
 				double centroidX = Double.valueOf(xCoords[jj-1]);
 				double centroidY = Double.valueOf(yCoords[ii-1]);
 				Coordinate centroid = new Coordinate(centroidX, centroidY);
                double xWidth = (X_MAX - X_MIN) / X_BINS;
				double yWidth = (Y_MAX - Y_MIN) / Y_BINS;
				Coordinate c1 =  new Coordinate(centroid.x - xWidth /2, centroid.y- yWidth /2);
 				Coordinate c2 =  new Coordinate(centroid.x + xWidth /2, centroid.y- yWidth /2);
 				Coordinate c3 =  new Coordinate(centroid.x + xWidth /2, centroid.y+ yWidth /2);
 				Coordinate c4 =  new Coordinate(centroid.x - xWidth /2, centroid.y+ yWidth /2);
 				Polygon poly = gf.createPolygon(new Coordinate[] {c1,c2,c3,c4,c1});
 				poly2Centroid.put(poly, centroid);
 				poly2toll.put(poly, Double.valueOf(arrayToll[ii][jj]));
 				poly2weight.put(poly, Double.valueOf(arrayWeight[ii][jj]));
 			}
 		}
 		
 		BufferedWriter writer = IOUtils.getBufferedWriter(OUT_FILE);
 		
 		int noOfSidesOfPolygon = 4;
 		try {
			for(int i=0;i<noOfSidesOfPolygon;i++){
				writer.write("polyX"+i+"\t"+"polyY"+i+"\t");
			}
			writer.write("centroidX \t centroidY \t toll \t weight \n ");
			
			for(Entry<Polygon, Coordinate> e : poly2Centroid.entrySet()){
				Coordinate [] ca = e.getKey().getCoordinates();
				for(int i = 0; i < ca.length-1; i++){ // a square polygon have 5 coordinate, first and last is same. 
					writer.write(ca[i].x+"\t"+ca[i].y+"\t");
				}
				writer.write(e.getValue().x+"\t"+e.getValue().y+"\t"+poly2toll.get(e.getKey())+"\t"+poly2weight.get(e.getKey())+"\n");
			}
			writer.close();
 		}catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	private String [] [] readInputFile(final String inputFile){
		BufferedReader reader = IOUtils.getBufferedReader(inputFile);
		String [] [] array = new String [Y_BINS+1][X_BINS];
		try {
			String line = reader.readLine();
			int xCount = 0;
			while(line!=null){
				String [] parts = line.split("\t");
				array[xCount] = parts;
				xCount++;
				line=reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not read from the file. Reason "+e);
		}
	return array;
	}
}