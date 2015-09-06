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

package playground.nmviljoen.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

public class CoordTransform {
	public static void main(String[] args) throws FileNotFoundException{
		String inputFile = args[0];
		String numNodes = args[1];
		String outputFile = args[2];
		int number = Integer.parseInt(numNodes);
		String lineNode = "";
		int count = 0;
		
		BufferedReader br1 = new BufferedReader(new FileReader(inputFile));
		
		String [][] nodeArray = new String[number+1][11];
		
		try {
//			lineNode = br1.readLine();
			while ((lineNode = br1.readLine()) != null) {
				String[] nodeData = lineNode.split(",");
				for (int k = 0; k<11;k++){
					nodeArray[count][k] = nodeData[k];
				}
				count++;

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br1 != null) {
				try {
					br1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for (int row = 1; row<=number;row++){
			double NodeX;
			double NodeY;
			double Long;
			double Lat;
			
			NodeX = Double.parseDouble(nodeArray[row][2]);
			NodeY = Double.parseDouble(nodeArray[row][3]);
			
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
			Coord old = new Coord(NodeX, NodeY);
			Coord trans = ct.transform(old);	
			Long = trans.getX();
			Lat = trans.getY();
			nodeArray[row][4]= String.valueOf(Long);
			nodeArray[row][5]= String.valueOf(Lat);
		}
		
		BufferedWriter bw1 = IOUtils.getBufferedWriter(outputFile);
		try{
			for (int row = 0; row<=number;row++){
				for (int col = 0; col < 11; col++){
				bw1.write(nodeArray[row][col]);
				bw1.write(",");
			}
				bw1.newLine();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw1.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
	
	}

}
