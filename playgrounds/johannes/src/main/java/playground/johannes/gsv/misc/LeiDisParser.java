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

package playground.johannes.gsv.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;

import playground.johannes.gsv.visum.NetFileReader;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class LeiDisParser {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		Map<String, Coord> coords = loadCoordinates("/home/johannes/gsv/matsim/studies/netz2030/data/raw/network.net");
		Map<String, Point> coords = LeiDis2Trajectory.loadCoordinates("/home/johannes/gsv/fpd/fraunhofer/ShapeFile_Strecken_Fernverkehr_link_node.SHP");
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/fpd/fraunhofer/gsu_leidis_zlm_2014.12.12.txt"));
		String line;
		
		Map<String, String> timeStamps = new TreeMap<String, String>();
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\\s+");
			String trainId = tokens[1];
			if(trainId.equalsIgnoreCase("90M")) {
				String nodeId = tokens[2];
				String record = tokens[3];
				
				try {
				String timeStamp = record.substring(15);
				
				timeStamps.put(timeStamp, nodeId);
				} catch (Exception e) {
					System.err.println("Failed to parse line: " + line);
				}
			}
			
			
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/fpd/fraunhofer/ice90.txt"));
		writer.write("time\tnode\tlong\tlat");
		writer.newLine();
		for(Entry<String, String> entry : timeStamps.entrySet()) {
			Point coord = coords.get(entry.getValue());
			if (coord != null) {
				writer.write(entry.getKey());
				writer.write("\t");
				writer.write(entry.getValue());
				writer.write("\t");
				writer.write(String.valueOf(coord.getX()));
				writer.write("\t");
				writer.write(String.valueOf(coord.getY()));
				writer.newLine();
			} else {
				System.err.println("Node not found: " + entry.getValue());
			}
		}
		writer.close();
	}
	
	public static Map<String, Coord> loadCoordinates(String filename) throws IOException {
		Map<String, TableHandler> handlers = new HashMap<String, NetFileReader.TableHandler>();
		NodeHandler handler = new NodeHandler();
		handlers.put("KNOTEN", handler);
		
		NetFileReader reader = new NetFileReader(handlers);
		reader.read(filename);
		
		return handler.coords;
	}
	
	private static class NodeHandler extends TableHandler {

		private Map<String, Coord> coords = new HashMap<String, Coord>();
		
		/* (non-Javadoc)
		 * @see playground.johannes.gsv.visum.NetFileReader.TableHandler#handleRow(java.util.Map)
		 */
		@Override
		public void handleRow(Map<String, String> record) {
			String nodeId = record.get("CODE");
			String xCoord = record.get("XKOORD");
			String yCoord = record.get("YKOORD");

			coords.put(nodeId, new Coord(Double.parseDouble(xCoord), Double.parseDouble(yCoord)));
			
		}
		
	}

}
