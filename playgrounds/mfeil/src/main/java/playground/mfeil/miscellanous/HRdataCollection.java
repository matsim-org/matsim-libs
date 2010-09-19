/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsHighestEducationAdder.java
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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mfeil.AgentsAttributesAdder;
import playground.mfeil.Counter;



/**
 * Reads Zurich 10% diluted agents' highest education from a given *.txt file.
 *
 * @author mfeil
 */
public class HRdataCollection {

	//private static final Logger log = Logger.getLogger(HRdataCollection.class);

	public static void main (final String [] args){
		String inputFile = "D:/Documents and Settings/Matthias Feil/My Documents/Engagements/MU1939/Improve/Baselining/NL Maintal - Anwesenheit 0-24h 2010-02.txt";
		String outputFile = "D:/Documents and Settings/Matthias Feil/My Documents/Engagements/MU1939/Improve/Baselining/output.xls";
		new HRdataCollection(inputFile, outputFile);
	}
	
	public HRdataCollection(String inputFile, String outputFile){
		this.run(inputFile, outputFile);
	}
	
	public void run (final String inputFile, String outputFile){
		
		log.info("Reading input file \"inputFile\"...");
		double[][] frühschicht = new double [31][24]; // Tage, Stunden
		double[][] spätschicht = new double [31][24];
		double[][] nachtschicht = new double [31][24];
		int zeile =1;
		try {

			FileReader fr = new FileReader(inputFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			String niederlassung = null;
			String schicht = null;
			String date = null;
			
			while (line != null) {		
				zeile++;
				tokenizer = new StringTokenizer(line);		
				
				niederlassung = tokenizer.nextToken();
				if (!niederlassung.startsWith("F")) {
					line = br.readLine();
					continue;
				}
				
				schicht = tokenizer.nextToken();
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
				else log.warn("Day not found!");
					
				if (schicht.equalsIgnoreCase("lager 1")) this.fillArray(tokenizer, frühschicht, pos);
				else if (schicht.equalsIgnoreCase("lager 2")) this.fillArray(tokenizer, spätschicht, pos);
				else if (schicht.equalsIgnoreCase("lager 3")) this.fillArray(tokenizer, nachtschicht, pos);
				else if (schicht.equalsIgnoreCase("")) {
					line = br.readLine();
					continue;
				}
				else log.warn("Schicht not found!");
				
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
		stream.println("Score\tnotNewInNeighbourhood\ttabuInNeighbourhood\tscoredInNeighbourhood\tActivity schedule");
		log.info("done.");
	}	
	
	
	public void fillArray (StringTokenizer tokenizer, double[][] array, int pos){
		String token = null;
		for (int i = 0;i<24;i++){
			token = tokenizer.nextToken();	
			array[pos][i]= Double.parseDouble(token);
		}	
	}
	
	public void write (double [][] array, PrintStream stream){
		for (int i=0;i<array.length;i++){
			for (int j=0;j<array[i].length;j++){
				stream.print(array[i][j]+"\t");
			}
			stream.println();
		}
	}
}

