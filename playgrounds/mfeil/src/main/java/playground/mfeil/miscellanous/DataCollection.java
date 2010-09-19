/* *********************************************************************** *
 * project: org.matsim.*
 * Datacollection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.miscellanous;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;




/**
 * Reads a *.txt file and converts content to different format. *
 * @author mfeil
 */
public class DataCollection {

	private static final Logger log = Logger.getLogger(DataCollection.class);

	public static void main (final String [] args){
		String inputFile = "F:/2010-09.txt";
		String outputFile = "F:/output.xls";
		new DataCollection(inputFile, outputFile);
	}
	
	public DataCollection(String inputFile, String outputFile){
		this.run(inputFile, outputFile);
	}
	
	public void run (final String inputFile, String outputFile){
		
		log.info("Reading input file \"inputFile\"...");
		double[][] morningshift = new double [31][24]; // Tage, Stunden
		double[][] afternoonshift = new double [31][24];
		double[][] nightshift = new double [31][24];
		double[][] external = new double [31][24];
		int zeile =1;
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String location = null;
			String shift1 = null;
			String shift2 = null;
			String date = null;
			
			while (line != null) {		
				zeile++;
				tokenizer = new StringTokenizer(line);		
				
				location = tokenizer.nextToken();
				if (!location.startsWith("F")) {
					line = br.readLine();
					continue;
				}
				
				shift1 = tokenizer.nextToken();
				shift2 = tokenizer.nextToken();
				date = tokenizer.nextToken();
				int pos = 0;
				
				if (date.startsWith("1.")) pos = 0;
				else if (date.startsWith("2.")) pos = 1;
				else if (date.startsWith("3.")) pos = 2;
				else if (date.startsWith("4.")) pos = 3;
				else if (date.startsWith("5.")) pos = 4;
				else if (date.startsWith("6.")) pos = 5;
				else if (date.startsWith("7.")) pos = 6;
				else if (date.startsWith("8.")) pos = 7;
				else if (date.startsWith("9.")) pos = 8;
				else if (date.startsWith("10.")) pos = 9;
				else if (date.startsWith("11.")) pos = 10;
				else if (date.startsWith("12.")) pos = 11;
				else if (date.startsWith("13.")) pos = 12;
				else if (date.startsWith("14.")) pos = 13;
				else if (date.startsWith("15.")) pos = 14;
				else if (date.startsWith("16.")) pos = 15;
				else if (date.startsWith("17.")) pos = 16;
				else if (date.startsWith("18.")) pos = 17;
				else if (date.startsWith("19.")) pos = 18;
				else if (date.startsWith("20.")) pos = 19;
				else if (date.startsWith("21.")) pos = 20;
				else if (date.startsWith("22.")) pos = 21;
				else if (date.startsWith("23.")) pos = 22;
				else if (date.startsWith("24.")) pos = 23;
				else if (date.startsWith("25.")) pos = 24;
				else if (date.startsWith("26.")) pos = 25;
				else if (date.startsWith("27.")) pos = 26;
				else if (date.startsWith("28.")) pos = 27;
				else if (date.startsWith("29.")) pos = 28;
				else if (date.startsWith("30.")) pos = 29;
				else if (date.startsWith("31.")) pos = 30;
				else log.warn("Day "+date+" not found!");
					
				if (shift1.equalsIgnoreCase("lager") && shift2.equalsIgnoreCase("1")) this.fillArray(tokenizer, morningshift, pos);
				else if (shift1.equalsIgnoreCase("lager") && shift2.equalsIgnoreCase("2")) this.fillArray(tokenizer, afternoonshift, pos);
				else if (shift1.equalsIgnoreCase("lager") && shift2.equalsIgnoreCase("3")) this.fillArray(tokenizer, nightshift, pos);
				else if (shift1.equalsIgnoreCase("extern"))this.fillArray(tokenizer, external, pos);
				else if (shift1.equalsIgnoreCase("")) {
					line = br.readLine();
					continue;
				}
				else log.warn("Shift "+shift1+" "+shift2+" not found!");
				
				line = br.readLine();
			}		
		} catch (Exception ex) {
			log.warn(ex +" at line "+ zeile);
		}
		String outputfile = outputFile;
		PrintStream stream;
		try {
			stream = new PrintStream (new File(outputfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		this.write(external,stream, new String ("external"));
		this.write(morningshift,stream, new String ("morningshift"));
		this.calculate(morningshift, external, stream, new String ("morningshift"));
		this.write(afternoonshift,stream, new String ("afternoonshift"));
		this.calculate(afternoonshift, external, stream, new String ("afternoonshift"));
		this.write(nightshift,stream, new String ("nightshift"));
		this.calculate(nightshift, external, stream, new String ("nightshift"));
		stream.close();

		log.info("done.");
	}	
	
	
	public void fillArray (StringTokenizer tokenizer, double[][] array, int pos){
		String token = null;
		for (int i = 0;i<24;i++){
			token = tokenizer.nextToken();	
			array[pos][i]= Double.parseDouble(token);
		}	
	}
	
	public void write (double [][] array, PrintStream stream, String shiftname){
		stream.print(shiftname+"\t");
		for (int x=0;x<24;x++)stream.print(x+"-"+(x+1)+"h\t");
		stream.println();
		for (int i=0;i<array.length;i++){
			stream.print("day "+(i+1)+"\t");
			for (int j=0;j<array[i].length;j++){
				stream.print(array[i][j]+"\t");
			}
			stream.println();
		}
		stream.println();
	}
	
	public void calculate (double [][] arrayinternal, double [][] arrayexternal, PrintStream stream, String shiftname){
		
		stream.print("day\t");
		for (int x=0;x<arrayinternal.length;x++)stream.print((x+1)+"\t");
		stream.println();
		
		int boundary = 0;
		if (shiftname.equalsIgnoreCase("morningshift")) boundary = 18;
		else if (shiftname.equalsIgnoreCase("afternoonshift")) boundary = 6;
		else if (shiftname.equalsIgnoreCase("nightshift")) boundary = 18;
		else log.warn("Couldn't recognize shiftname "+shiftname);
		
		double hours = 0.0;
		stream.print("hours internal\t");
		for (int i=0;i<arrayinternal.length;i++){
			hours = 0.0;
			if (shiftname.equalsIgnoreCase("morningshift") || shiftname.equalsIgnoreCase("nightshift")){
				if (i>0) {
					for (int j=boundary;j<arrayinternal[i].length;j++) hours+=arrayinternal[i-1][j];
				}
				for (int j=0;j<boundary;j++) hours+=arrayinternal[i][j];
			}
			else if (shiftname.equalsIgnoreCase("afternoonshift")){
				for (int j=boundary;j<arrayinternal[i].length;j++) hours+=arrayinternal[i][j];
				if (i<arrayinternal.length-1) {
					for (int j=0;j<boundary;j++) hours+=arrayinternal[i+1][j];
				}
			}
			else log.warn("Couldn't recognize shiftname "+shiftname);
			stream.print(hours+"\t");
		}
		stream.println();
		
		stream.print("hours external\t");
		for (int i=0;i<arrayexternal.length;i++){
			hours = 0.0;
			if (shiftname.equalsIgnoreCase("morningshift")){
				for (int j=6;j<14;j++) hours+=arrayexternal[i][j];
			}
			else if (shiftname.equalsIgnoreCase("afternoonshift")){
				for (int j=14;j<22;j++) hours+=arrayexternal[i][j];
			}
			else if (shiftname.equalsIgnoreCase("nightshift")){
				if (i>0) {
					for (int j=22;j<24;j++) hours+=arrayexternal[i-1][j];
				}
				for (int j=0;j<6;j++) hours+=arrayexternal[i][j];
			}
			else log.warn("Couldn't recognize shiftname "+shiftname);
			stream.print(hours+"\t");
		}
		stream.println();
		
		stream.println();
	}
}

