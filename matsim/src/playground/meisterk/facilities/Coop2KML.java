/* *********************************************************************** *
 * project: org.matsim.*
 * Coop2KML.java
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

package playground.meisterk.facilities;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.matsim.utils.misc.TimeFormatter;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Placemark;

public class Coop2KML {

	private static KML myKML;
	private static Document myKMLDocument;
	private static String inputFilename = "/Users/meisterk/Documents/workspace/matsimJ/input/coopzh.txt";
	private static String kmlFilename = "/Users/meisterk/Documents/workspace/matsimJ/input/coopzh.kmz";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		setUp();
		generateFacilities();
		write();
		
	}

	private static void setUp() {
		
		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);
		
	}
	
	private static void generateFacilities() {

		List<String> lines = null;
		String[] tokens = null;
		
		try {
			lines = FileUtils.readLines(new File(inputFilename), "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Folder wednesdayFolder = new Folder(
				"wednesdayFolder",
				"wednesdayFolder",
				"Wednesday",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(wednesdayFolder);
		
		tokens = lines.get(0).split("\t");
		System.out.println(tokens[7] + "\t" + tokens[9] + "\t" + tokens[10] + "\t" + tokens[11]);

		String wedOpen1 = tokens[22];
		TimeFormatter tf = new TimeFormatter(TimeFormatter.TIMEFORMAT_HHMM);
		System.out.println(tf.parseTime(wedOpen1) / 3600);
		
		
		String wedOpen2 = tokens[23];
		String wedOpen3 = tokens[24];
		String wedOpen4 = tokens[25];
		
		Placemark aCoop = new Placemark(
				tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11],
				tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11],
				tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11],
				tokens[9] + ", " + tokens[10] + " " + tokens[11],
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		
		
		
		wednesdayFolder.addFeature(aCoop);
		
		
		
//		for (String str : lines) {
//			
//			tokens = str.split("\t");
//			System.out.println(tokens[7] + "\t" + tokens[9] + "\t" + tokens[10] + "\t" + tokens[11]);
//			
//		}
		
	}
	
	private static void write() {
		
		System.out.println("    writing KML files out...");

		KMZWriter writer;
		writer = new KMZWriter(kmlFilename);
		writer.writeMainKml(myKML);
		writer.close();
		
		System.out.println("    done.");

	}
	
}
