/* *********************************************************************** *
 * project: org.matsim.*
 * JoinColumns.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.io.IOUtils;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author illenberger
 *
 */
public class JoinColumns {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(args[0]);
		int col = Integer.parseInt(args[1]);
		TabularFileParser parser = new TabularFileParser();
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterRegex("\t");
		
		int minRow = Integer.MAX_VALUE;
		int maxRow = Integer.MIN_VALUE;
		List<List<String>> colums = new LinkedList<List<String>>();
		for(int i = 2; i < args.length; i++) {
			Handler handler = new Handler();
			handler.col = col;
			config.setFileName(args[i]);
			try {
			parser.parse(config, handler);
			colums.add(handler.rows);
			if(handler.rows.size() < minRow)
				minRow = handler.rows.size();
			
			maxRow = Math.max(maxRow, handler.rows.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for(int r = 0; r < maxRow; r++) {
			double sum = 0;
			int cnt = 0;
			for(int c = 0; c < colums.size(); c++) {
				if(r == 0) //Header!
					writer.write(colums.get(c).get(r) + c);
				else {
					if(r >= colums.get(c).size())
						writer.write("");
					else {
						writer.write(colums.get(c).get(r));
						if(!colums.get(c).get(r).equalsIgnoreCase("NaN")) { 
							sum += Double.parseDouble(colums.get(c).get(r));
							cnt++;
						}
					}
				}
				writer.write("\t");
			}
			if(r == 0)
				writer.write("mean");
			else
				writer.write(String.valueOf(sum/(double)cnt));

			writer.newLine();
		}
		writer.close();
	}
	
	private static class Handler implements TabularFileHandler {
		
		public int col;
		
		public LinkedList<String> rows = new LinkedList<String>();
		
		public void startRow(String[] row) {
//			if(row[col].equalsIgnoreCase("NaN"))
////				throw new IllegalArgumentException("Nan");
//				rows.add("0");
			
			rows.add(row[col]);
		}
		
	}

}
