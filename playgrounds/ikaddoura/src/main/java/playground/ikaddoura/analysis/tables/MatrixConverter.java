/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.tables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a table into a matrix.
 * 
 * 			V	H	X
 * 			v1	h1	x1
 * 			v1	h2	x2
 * 			v2	h1	x3
 * 			v2	h2	x4
 * 			.	.	.
 * 			.	.	.
 * 			.	.	.
 * 
 * 			==>
 * 
 * 			V\H	h1	h2	.	.
 * 			v1	x1	x2	.	.
 * 			v2	x3	x4	.	.
 * 			.	.	.	.	.
 * 			.	.	.	.	.
 * 
 * 
 * @author ikaddoura
 */
public class MatrixConverter {

	private String inputTable = "/Users/ihab/Desktop/TC_SD.csv"; // data
	private String params_vertical_file = "/Users/ihab/Desktop/busNr.csv"; 
	private String params_horizontal_file = "/Users/ihab/Desktop/fares.csv"; 
	private String outputMatrix = "/Users/ihab/Desktop/TC_SD_matrix.csv";
	private String seperator = ";";
	
	private List<List<String>> lines = new ArrayList<List<String>>();
	private List<String> params_vertical;
	private List<String> params_horizontal;
	private BufferedWriter bw;
	
	public static void main(String[] args) {
		MatrixConverter matrixConverter = new MatrixConverter();
		matrixConverter.run();
	}

	private void run() {
		
		readInputTable();
		params_vertical = readParameters(params_vertical_file);
		params_horizontal = readParameters(params_horizontal_file);
		
		System.out.println("Data: " + this.lines);
		System.out.println("Vertical parameters: " + params_vertical);
		System.out.println("Horizontal parameters: " + params_horizontal);
		
		fillMatrix(params_vertical, params_horizontal);
		System.out.println("Matrix file written.");
	}

	private List<String> readParameters(String file) {
		List<String> params = new ArrayList<String>();
		BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader(new File(file)));
	        String line = null;
	        
	        while((line = br.readLine()) != null) {
	           params.add(line); 	
	        }
	        
	    } catch(FileNotFoundException e) {
	        e.printStackTrace();
	    } catch(IOException e) {
	        e.printStackTrace();
	    } finally {
	        if(br != null) {
	            try {
	                br.close();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }	
		
		return params;
	}

	private void fillMatrix(List<String> params_vertical, List<String> params_horizontal) {
		File file = new File(this.outputMatrix);
		
		try {
			bw = new BufferedWriter(new FileWriter(file));
			
			// first line
			for (String param_h : this.params_horizontal){
				bw.write(this.seperator + param_h);
			}
			bw.newLine();
			
			// for each line
			for (String param_v : this.params_vertical){
				bw.write(param_v);
				// for each column
				for (String param_h : this.params_horizontal){
					// find value for this parameter combination
					String value = null;
					for (List<String> lineEntries : this.lines){
						if (lineEntries.get(0).equals(param_v) && lineEntries.get(1).equals(param_h)) {
							if (value == null) {
								value = lineEntries.get(2);
								bw.write(this.seperator + value);
							} else {
								throw new RuntimeException("Two values found for the parameter combination " + param_v + " and " + param_h + ". Aborting...");
							}
						}
					}
					if (value == null) {
						throw new RuntimeException("No value found for the parameter combination " + param_v + " and " + param_h + ". Aborting...");
					}
				}
				bw.newLine();
			}
			bw.close();
						
		} catch (IOException e) {
			e.printStackTrace();
		}
				
	}

	private void readInputTable() {

		BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader(new File(inputTable)));
	        String line = null;
	        
	        int lineCounter = 0;
	        while((line = br.readLine()) != null) {
	            if (lineCounter > 0) {	            	
	            	String[] parts = line.split(this.seperator);
	            		            	
	            	String a = parts[0];
	            	String b = parts[1];
	            	String c = parts[2];

	            	List<String> lineEntries = new ArrayList<String>();
	            	lineEntries.add(a);
	            	lineEntries.add(b);
	            	lineEntries.add(c);

	            	lines.add(lineEntries);
	            }  	
	            lineCounter++;
	        }
	    } catch(FileNotFoundException e) {
	        e.printStackTrace();
	    } catch(IOException e) {
	        e.printStackTrace();
	    } finally {
	        if(br != null) {
	            try {
	                br.close();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }	
		
	}
			 
}