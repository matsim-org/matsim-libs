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

public class MatrixConverter {

	private String inputTable = "/Users/ihab/Desktop/TC_SD.csv"; // data
	private String busNr_file = "/Users/ihab/Desktop/busNr.csv"; 
	private String fares_file = "/Users/ihab/Desktop/fares.csv"; 
	private String outputMatrix = "/Users/ihab/Desktop/TC_SD_matrix.csv";
	
	private List<List<String>> lines = new ArrayList<List<String>>();
	private List<String> busNr;
	private List<String> fares;
	
	public static void main(String[] args) {
		MatrixConverter matrixConverter = new MatrixConverter();
		matrixConverter.run();
	}

	private void run() {
		
		readInputTable();
		busNr = readParameters(busNr_file);
		fares = readParameters(fares_file);
		
		System.out.println(this.lines);
		System.out.println("Params1: " + busNr);
		System.out.println("Params2: " + fares);
		
		fillMatrix(busNr, fares);
		System.out.println("Done.");
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

	private void fillMatrix(List<String> params1, List<String> params2) {
		File file = new File(this.outputMatrix);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// first line
			for (String fare : this.fares){
				bw.write(";" + fare);
			}
			bw.newLine();
			
			// for each line
			for (String busNr : this.busNr){
				bw.write(busNr);
				// for each column
				for (String fare : this.fares){
					// find value for this parameter combination
					String value = null;
					System.out.println("BusNr: " + busNr);
					System.out.println("Fare: " + fare);
					for (List<String> lineEntries : this.lines){
						if (lineEntries.get(0).equals(busNr) && lineEntries.get(1).equals(fare)) {
							if (value == null) {
								value = lineEntries.get(2);
								bw.write(";" + value);
								System.out.println("Value: " + lineEntries.get(2));
							} else {
								throw new RuntimeException("Two values found for the parameter combination " + busNr + " and " + fare + ". Aborting...");
							}
						}
					}
					if (value == null) {
						throw new RuntimeException("No value found for the parameter combination " + busNr + " and " + fare + ". Aborting...");
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
	            	String[] parts = line.split(";");
	            	
//	            	for (int i = 0 ; i <= parts.length; i++){
//	            		if (parts[i].isEmpty()){
//	        				throw new RuntimeException("The input file is not complete. Aborting...");
//	            		}
//	            	}
	            		            	
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
		

