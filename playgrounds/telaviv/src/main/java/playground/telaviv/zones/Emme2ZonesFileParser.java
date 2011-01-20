/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2ZonesFileParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.zones;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.gbl.Gbl;

public class Emme2ZonesFileParser {

	private String inFile;
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");
	
	public Emme2ZonesFileParser(String inFile) {
		this.inFile = inFile;
	}
	
	public Map<Integer, Emme2Zone> readFile() {
		Map<Integer, Emme2Zone> zones = new TreeMap<Integer, Emme2Zone>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	       
    	try {
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
//			br.readLine();
			 
			String line;
			while((line = br.readLine()) != null) {
				Emme2Zone zone = new Emme2Zone();
				
				String[] cols = line.split(separator);
				
				zone.TAZ = parseInteger(cols[0]);
				zone.AREA = parseDouble(cols[1]);
				zone.TYPE = parseInteger(cols[2]);
				zone.CULTURAL = parseInteger(cols[3]);
				zone.EDUCATION = parseInteger(cols[4]);
				zone.OFFICE = parseInteger(cols[5]);
				zone.SHOPPING = parseInteger(cols[6]);
				zone.HEALTH = parseInteger(cols[7]);
				zone.RELIGIOSIT = parseInteger(cols[8]);
				zone.URBAN = parseInteger(cols[9]);
				zone.TRANSPORTA = parseInteger(cols[10]);
				zone.EMPL_INDU = parseInteger(cols[11]);
				zone.EMPL_COMM = parseInteger(cols[12]);
				zone.EMPL_SERV = parseInteger(cols[13]);
				zone.EMPL_TOT = parseInteger(cols[14]);
				zone.STUDENTS = parseInteger(cols[15]);
				zone.POPULATION = parseInteger(cols[16]);
				zone.HOUSEHOLDS = parseInteger(cols[17]);
				zone.PARKCOST = parseInteger(cols[18]);
				zone.PARKWALK = parseInteger(cols[19]);
				zone.POPDENS = parseDouble(cols[20]);
				zone.GA21 = parseInteger(cols[21]);
				zone.GA22 = parseInteger(cols[22]);
				zone.GA23 = parseInteger(cols[23]);
				zone.GA24 = parseInteger(cols[24]);
				zone.GA25 = parseInteger(cols[25]);
				zone.GA26 = parseInteger(cols[26]);
				zone.GA11 = parseInteger(cols[27]);
				zone.GA12 = parseInteger(cols[28]);
				zone.GA13 = parseInteger(cols[29]);
				zone.GA14 = parseInteger(cols[30]);
				zone.GA15 = parseInteger(cols[31]);
				zone.GA16 = parseInteger(cols[32]);
				zone.WORKERS = parseInteger(cols[33]);
				zone.WORKPERC = parseDouble(cols[34]);
				zone.AVGHH = parseDouble(cols[35]);
				zone.SOCECO = parseInteger(cols[36]);
				zone.MLIC2 = parseDouble(cols[37]);
				zone.MLIC3 = parseDouble(cols[38]);
				zone.MLIC4 = parseDouble(cols[39]);
				zone.MLIC5 = parseDouble(cols[40]);
				zone.MLIC6 = parseDouble(cols[41]);
				zone.FLIC2 = parseDouble(cols[42]);
				zone.FLIC3 = parseDouble(cols[43]);
				zone.FLIC4 = parseDouble(cols[44]);
				zone.FLIC5 = parseDouble(cols[45]);
				zone.FLIC6 = parseDouble(cols[46]);
				zone.EMP2POP = parseDouble(cols[47]);
				zone.PARKCAP = parseInteger(cols[48]);
				zone.PARKAM = parseDouble(cols[49]);
				zone.PARKPM = parseDouble(cols[50]);
				zone.PARKOP = parseDouble(cols[51]);
				zone.INTLSUME = parseDouble(cols[52]);
				zone.INTLSUMO = parseDouble(cols[53]);
				zone.INTLSUMS = parseDouble(cols[54]);
				zone.INTLSUMW = parseDouble(cols[55]);
				zone.LSIZESEC = parseDouble(cols[56]);
				zone.LSIZEINTS = parseDouble(cols[57]);
				zone.LSUMSIZE0 = parseDouble(cols[58]);
				zone.LSUMSIZES = parseDouble(cols[59]);
				zone.SUPERZONE = parseInteger(cols[60]);
				
				zones.put(zone.TAZ, zone);
			}
			
			br.close();
			isr.close();
			fis.close();
    	} catch (FileNotFoundException e)  {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		return zones;
	}
	
	private int parseInteger(String string) {
		if (string == null) return 0;
		else if (string.trim().equals("")) return 0;
		else return Integer.valueOf(string);
	}
	
	private double parseDouble(String string) {
		if (string == null) return 0.0;
		else if (string.trim().equals("")) return 0.0;
		else return Double.valueOf(string);
	}
}
