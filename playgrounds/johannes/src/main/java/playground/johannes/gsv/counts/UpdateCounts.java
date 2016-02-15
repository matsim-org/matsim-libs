/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.counts;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class UpdateCounts {

	private static String DTV_KFZ_R1 = "DTV_KFZ_MO-SO_RI1";
	
	private static String DTV_KFZ_R2 = "DTV_KFZ_MO-SO_RI2";
	
	private static String DTV_SV_R1 = "DTV_SV_MO-SO_RI1";
	
	private static String DTV_SV_R2 = "DTV_SV_MO-SO_RI2";
	
	private static String DIR1_X = "Fernziel_Ri1_long";
	
	private static String DIR1_Y = "Fernziel_Ri1_lat";
	
	private static String DIR2_X = "Fernziel_Ri2_long";
	
	private static String DIR2_Y = "Fernziel_Ri2_lat";
	
	private static String CODE = "CODE";
	
	private static String XCOORD = "XKOORD";
	
	private static String YCOORD = "YKOORD";
	
	private static String NAME = "NAME";
	
	private static final Logger logger = Logger.getLogger(UpdateCounts.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/*
		 * read original file
		 */
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/counts/counts-modena.geo.txt"));
		String line = reader.readLine();
		
		String[] header = line.split("\t");
		Map<String, Integer> colIndices = new HashMap<String, Integer>();
		for(int i = 0; i < header.length; i++) {
			colIndices.put(header[i], i);
		}
		
		int idIdx = colIndices.get(CODE);
		int xIdx = colIndices.get(XCOORD);
		int yIdx = colIndices.get(YCOORD);
		int valDirect1Idx = colIndices.get(DTV_KFZ_R1);
		int valDirect2Idx = colIndices.get(DTV_KFZ_R2);
		int valDirect1SVIdx = colIndices.get(DTV_SV_R1);
		int valDirect2SVIdx = colIndices.get(DTV_SV_R2);
		int nameIdx = colIndices.get(NAME);
		
		int xDirect1 = colIndices.get(DIR1_X);
		int yDirect1 = colIndices.get(DIR1_Y);
		int xDirect2 = colIndices.get(DIR2_X);
		int yDirect2 = colIndices.get(DIR2_Y);
		
		Map<String, Map<String, String>> records = new HashMap<>();
		while((line = reader.readLine()) != null) {
			Map<String, String> record = new HashMap<>();
			String tokens[] = line.split("\t", -1);
			
			record.put(CODE, tokens[idIdx]);
			record.put(XCOORD, tokens[xIdx]);
			record.put(YCOORD, tokens[yIdx]);
			record.put(DTV_KFZ_R1, tokens[valDirect1Idx]);
			record.put(DTV_KFZ_R2, tokens[valDirect2Idx]);
			record.put(DTV_SV_R1, tokens[valDirect1SVIdx]);
			record.put(DTV_SV_R2, tokens[valDirect2SVIdx]);
			record.put(NAME, tokens[nameIdx]);
			record.put(DIR1_X, tokens[xDirect1]);
			record.put(DIR1_Y, tokens[yDirect1]);
			record.put(DIR2_X, tokens[xDirect2]);
			record.put(DIR2_Y, tokens[yDirect2]);
			
			records.put(tokens[idIdx], record);
		}
		
		reader.close();
		/*
		 * load update file
		 */
		reader = new BufferedReader(new FileReader("/home/johannes/gsv/counts/bast2013.raw.csv"));
		line = reader.readLine();
		
		header = line.split(";");
		colIndices = new HashMap<String, Integer>();
		for(int i = 0; i < header.length; i++) {
			colIndices.put(header[i].toUpperCase(), i);
		}
		
		int id1Idx = colIndices.get("TK_NR");
		int id2Idx = colIndices.get("DZ_NR");
		
		valDirect1Idx = colIndices.get(DTV_KFZ_R1);
		valDirect2Idx = colIndices.get(DTV_KFZ_R2);
		valDirect1SVIdx = colIndices.get(DTV_SV_R1);
		valDirect2SVIdx = colIndices.get(DTV_SV_R2);
		
		int notfound = 0;
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";", -1);
			
			String id = String.format("%s/%s", tokens[id1Idx], tokens[id2Idx]);
			Map<String, String> record = records.get(id);
			if(record != null) {
				if(isValid(tokens[valDirect1Idx])) record.put(DTV_KFZ_R1, tokens[valDirect1Idx]);
				if(isValid(tokens[valDirect2Idx])) record.put(DTV_KFZ_R2, tokens[valDirect2Idx]);
				if(isValid(tokens[valDirect1SVIdx])) record.put(DTV_SV_R1, tokens[valDirect1SVIdx]);
				if(isValid(tokens[valDirect2SVIdx])) record.put(DTV_SV_R2, tokens[valDirect2SVIdx]);
			} else {
				logger.info(String.format("Count station %s not found", id));
				notfound++;
			}
		}
		
		if(notfound > 0) logger.warn(String.format("%s count stations not found.", notfound));
		
		List<String> keys = new ArrayList<>(records.entrySet().iterator().next().getValue().keySet());
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/counts/counts.2013.txt"));
		for(String key : keys) {
			writer.write(key);
			writer.write("\t");
		}
		writer.newLine();
		
		for(Map<String, String> record : records.values()) {
			for(String key : keys) {
				writer.write(record.get(key));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
	}

	private static boolean isValid(String token) {
		if(token.isEmpty()) return false;
		double val = Double.parseDouble(token);
		if(val > 0) {
			return true;
		} else {
			return false;
		}
	}
	
}
