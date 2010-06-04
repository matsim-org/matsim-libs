/* *********************************************************************** *
 * project: org.matsim.*
 * TXTParser
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

package playground.christoph.snowball;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/*
 * Vorgehensweise:
 * Das Excel File via OpenOffice in ein CSV File konvertieren
 * und als Zeichensatz UTF-8 wählen.
 * 
 * Format:
 * ID,	Name,					Adresse1,				Adresse2,		Adresse3,		Adresse4
 * 213,	Dr.phil. Tildy Hanhart,	Limmatstrasse 181/10,	8005 Zürich, 	Switzerland,	044/272 26 17
 */
public class TXTParser {

	private List<Data> lines = null;
	private String header;
	private String separator = "\t";
//	private Charset charset = Charset.forName("ISO-8859-1");
	private Charset charset = Charset.forName("UTF-8");
	
//	private File inFile = new File("../../matsim/mysimulations/snowball/Netzwerkgrafik -Alteri_Subsample2_10.02.2010.txt");
	private File inFile = new File("../../matsim/mysimulations/snowball/Rekrutierung28042010.txt");
	private File outFile = new File("../../matsim/mysimulations/snowball/geocoded.csv");
	private File coordinatesFile = new File("../../matsim/mysimulations/snowball/coordinates.txt");
	
	public static void main(String[] args)
	{
		TXTParser parser = new TXTParser();
		parser.readFile();
		parser.writeFile();
		parser.writeCoordinatesFile();
	}
	
	public List<Data> readFile()
	{
//	    FileReader fr = null;
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	       
    	try 
    	{
//			fr = new FileReader(inFile);
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			header = br.readLine();
			
//			SQL_IDEgo	Excel_IDEgo	Position_NG	NameAlter	Adresszeile3	Adresszeile4	Adresszeile5	Adresse_angegeben	Lebensalter	Sprache	Lfn
			
			lines = new ArrayList<Data>(); 
			String line;
			while((line = br.readLine()) != null)
			{
				String[] cols = line.split(separator);
				Data data = new Data();		
				
				data.egoId = Integer.valueOf(cols[0].trim());
				data.excel_egoId = cols[1].trim();
				data.position_ng = cols[2].trim();
				data.name = cols[3].trim();
				data.address3 = cols[4].trim();
				data.address4 = cols[5].trim();
				data.address5 = cols[5].trim();

//				String address = data.address3 + "\t" + data.address4 + "\t" + data.address5;
				String address = "";
				if (!data.address3.equalsIgnoreCase("NA")) address = address + data.address3 + " ";
				if (!data.address4.equalsIgnoreCase("NA")) address = address + data.address4 + " ";
				if (!data.address5.equalsIgnoreCase("NA")) address = address + data.address5;
				
				if (!data.address3.equalsIgnoreCase("NA") || !data.address4.equalsIgnoreCase("NA") || !data.address5.equalsIgnoreCase("NA"))
				{
				
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Geocoder.Location location = Geocoder.getLocation(address);
					if (location != null)
					{
						data.lat = location.lat;
						data.lon = location.lon;
						lines.add(data);
						
						System.out.println("use address: " + address);
					}
					else System.out.println("could not geocode address: " + address);
				}
				else System.out.println("skip address: " + address);
			}
			
			br.close();
//			fr.close();
			isr.close();
			fis.close();
    	}
    	catch (FileNotFoundException e) 
    	{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public void writeFile()
	{
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
	    
	    try 
	    {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write(header);
			bw.write("\t" + "lat" + "\t" + "lon");
			bw.write("\n");
			
			// write Values
			for (Data data : lines)
			{				
				bw.write(data.egoId + separator);
				bw.write(data.excel_egoId + separator);
				bw.write(data.position_ng + separator);
				bw.write(data.name + separator);
				bw.write(data.address3 + separator);
				bw.write(data.address4 + separator);
				bw.write(data.address5 + separator);
				bw.write(data.lat + separator);
				bw.write(data.lon + separator);
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void writeCoordinatesFile()
	{
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
	    
	    try 
	    {
			fos = new FileOutputStream(coordinatesFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Values
			for (Data data : lines)
			{				
				bw.write(data.egoId + "&" + data.position_ng + separator);
//				bw.write(data.excel_egoId + separator);
//				bw.write(data.position_ng + separator);
//				bw.write(data.name + separator);
//				bw.write(data.address3 + separator);
//				bw.write(data.address4 + separator);
//				bw.write(data.address5 + separator);
				bw.write(data.lat + separator);
				bw.write(data.lon + separator);
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static class Data
	{
		/* 
		 * Example:
		 * SQL_IDEgo	Excel_IDEgo	Position_NG		NameAlter		Adresszeile3	Adresszeile4	Adresszeile5	Adresse_angegeben				Lebensalter		Sprache		Lfn
		 * 600			J 2185		84967X55X143A1	Agnes Fluetsch	Hinterdorf 108	7220 Schiers	Schweiz			7220 schiers bundtistr 129t		51				deutsch		20001
		 * 
		 * If address is "NA" -> skip it!			
		 */  
		public int egoId;
		public String excel_egoId;
		public String position_ng;
		public String name;
		public String address3;
		public String address4;
		public String address5;
		
		public String lon;
		public String lat;
	}
}
