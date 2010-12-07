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
	private Set<String[]> lines = null;
	private String[] header = null;
	String inFile;
	
	public DrTabReader(String inFile){
		this.inFile = inFile;
		this.lines = new TreeSet<String[]>();
	}
	
	public void readTabFile(boolean header){
		
		boolean first = header;
		String line;
		
		try {
			BufferedReader reader = IOUtils.getBufferedReader(this.inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split("\t");
					if(first == true){
						this.header = columns;
						first = false;
					}else{
						this.lines.add(columns);
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
	}
	
	public Set<String[]> getContent(){
		if(this.lines == null){
			throw new RuntimeException("call readTabFile() first!");
		}
		return this.lines;
	}
	
	public String[] getHeader(){
		if (this.lines == null){
			throw new RuntimeException("call readTabFile() first!");
		} else if(this.header == null){
			log.error("file has no header!");
			return null;
		}else{
			return this.header;
		}
	}

}
