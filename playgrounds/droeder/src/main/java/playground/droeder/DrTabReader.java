/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class DrTabReader {
	private static final Logger log = Logger.getLogger(DrTabReader.class);
	
	
	public static Set<String[]> readTabFileContent(String inFile, boolean hasHeader){
		
		boolean first = hasHeader;
		Set<String[]> lines = new TreeSet<String[]>();
		
		String line;
		try {
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split("\t");
					if(first == true){
						first = false;
					}else{
						lines.add(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public static String[] readTabFileHeader(String inFile){
		
		String line;
		String[] header = null;
		try {
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			if(!(line == null)){
				header = line.split("\t");
			}
			reader.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return header;
	}
	

}
