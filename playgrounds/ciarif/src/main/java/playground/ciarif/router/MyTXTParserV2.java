package playground.ciarif.router;

/* *********************************************************************** *
 * project: org.matsim.*
 * TXTParserV2.java
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

import playground.christoph.tools.Geocoder;

/*
 * Vorgehensweise:
 * Das Excel File via OpenOffice in ein CSV File konvertieren
 * und als Zeichensatz UTF-8 wählen.
 * 
 * Format	/	Beispiel
 * SQL_IDEgo	/	625
 * Subsample	/	J
 * Excel_IDEgo	/	20031
 * Position_NG	/	84967X55X143A1
 * NameAlter	/	Max Mustermann
 * Adresszeile3	/	Musterstrasse 99
 * Adresszeile4	/	8050 Zürich
 * Adresszeile5	/	Schweiz
 * Adresse_angegeben	/	"Musterstrasse 12, 8050 Zürich, Schweiz"
 * Lebensalter	/	54	
 * Sprache	/	deutsch
 * Auswahl	/	1
 * Telefon	/	043 222 10 00
 * 
 */
public class MyTXTParserV2 {

	private List<Data> lines = null;
	private String header;
	private String separator = "\t";
//	private String separator = ",";
	private Charset charset = Charset.forName("ISO-8859-1");
	//private Charset charset = Charset.forName("UTF-8");
	
	private File inFile = new File("../../matsim/input/input.txt");
	private File outFile = new File("../../matsim/output/output.txt");
	//private String shapeFile = "../../matsim/mysimulations/snowball/G1K08.shp";
	
//	private File inFile = new File("infile.csv");
//	private File outFile = new File("outFile.csv");
//	private String shapeFile = "G1K08.shp";
	
	public static void main(String[] args)
	{
		MyTXTParserV2 parser = new MyTXTParserV2();
		parser.readFile();
		parser.writeFile();
	}
	
	public List<Data> readFile()
	{
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	       
    	try 
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			
			// skip first Line
			header = br.readLine();
						
			lines = new ArrayList<Data>(); 
			String line;
			while((line = br.readLine()) != null)
			{
				String[] cols = line.split(separator);
				Data data = new Data();
				
				data.line = line;
				
//				System.out.println(line);
				
//				data.egoId = cols[0].trim();
//				data.subSample = cols[1].trim();
//				data.excel_egoId = cols[2].trim();
//				data.position_ng = cols[3].trim();
//				data.name = cols[4].trim();
				data.address3 = cols[0].trim();
				data.address4 = cols[1].trim();
				data.address5 = cols[2].trim();
//				data.addressEntered = cols[8].trim();
//				data.age = cols[9].trim();
//				data.language = cols[10].trim();
//				data.choice = cols[11].trim();
//				data.phone = cols[12].trim();
				
				String address = "";
				if (!data.address3.equalsIgnoreCase("NA")) address = address + data.address3 + " ";
				if (!data.address4.equalsIgnoreCase("NA")) address = address + data.address4 + " ";
				if (!data.address5.equalsIgnoreCase("NA")) address = address + data.address5;
				
				if (!data.address3.equalsIgnoreCase("NA") || !data.address4.equalsIgnoreCase("NA") || !data.address5.equalsIgnoreCase("NA"))
				{
				
					/*
					 *  Google does not allow to many connections at a time.
					 *  Therefore we wait for 250 ms before we geocode an address.
					 */
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Geocoder.Location location = Geocoder.getLocation(address);
					if (location != null)
					{
						data.lat = location.lat;
						data.lon = location.lon;
												
						System.out.println("use address: " + address);
					}
					else System.out.println("could not geocode address: " + address);
				}
				lines.add(data);
//				else System.out.println("skip address: " + address);
			}
			
			br.close();
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
			bw.write("lat" + separator + "lon" + separator);
			bw.write(header);
			bw.write("\n");
			
			// write Values
			for (Data data : lines)
			{	
				bw.write(data.lat + separator);
				bw.write(data.lon + separator);
				bw.write(data.line + separator);
//				bw.write(data.egoId + separator);
//				bw.write(data.excel_egoId + separator);
//				bw.write(data.position_ng + separator);
//				bw.write(data.name + separator);
//				bw.write(data.address3 + separator);
//				bw.write(data.address4 + separator);
//				bw.write(data.address5 + separator);
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
		public String lon = "";
		public String lat = "";
		
		public String line;
		
		public String egoId;
		public String subSample;
		public String excel_egoId;
		public String position_ng;
		public String name;
		public String address3;
		public String address4;
		public String address5;
		public String addressEntered;
		public String age;
		public String language;
		public String choice;
		public String phone;	
	}
}