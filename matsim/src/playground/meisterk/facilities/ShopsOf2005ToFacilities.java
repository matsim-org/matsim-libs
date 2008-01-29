/* *********************************************************************** *
 * project: org.matsim.*
 * ProcessPickPayOpenTimes.java
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
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Style;

/**
 * In April 2005, I collected information on shop facilities of the major
 * Swiss retailers, incl. Migros, Coop, Pickpay and Denner. The information
 * includes addresses and opening times, as it is available on the respective
 * company websites.
 * 
 * This class fuses this information and formats it to a MATSim compatible
 * facility file.
 * 
 * @author meisterk
 *
 */
public class ShopsOf2005ToFacilities {

	private static final String HOME_DIR = System.getenv("HOME");
	private static final String SEP = System.getProperty("file.separator");

	private static final String SANDBOX_NAME = "sandbox00";
	private static final String SHOPS_CVS_MODULE = "ivt/studies/switzerland/facilities/shopsOf2005";
	private static final String SHOPS_PATH = HOME_DIR + SEP + SANDBOX_NAME + SEP + SHOPS_CVS_MODULE + SEP;
	
	private static String pickPayOpenTimesFilename = SHOPS_PATH + "pickpay_opentimes.txt";
	private static String pickPayAdressesFilename = SHOPS_PATH + "pickpay_addresses_ge_ready.csv";
	private static String coopZHFilename = SHOPS_PATH + "coop-zh.csv";
	private static String coopTGFilename = SHOPS_PATH + "coop-tg.csv";
	private static String migrosZHFilename = SHOPS_PATH + "migros-zh.csv";
	private static String migrosOstschweizFilename = SHOPS_PATH + "migros-ostschweiz-filialen.csv";
	private static String dennerTGZHFilename = SHOPS_PATH + "denner-tg-zh.csv";

	private static KML myKML = null;
	private static Document myKMLDocument = null;
	private static String kmlFilename = "output" + SEP + "shopsOf2005.kmz";

	private static Style coopStyle = null;
	private static Style pickpayStyle = null;
	private static Style migrosStyle = null;
	private static Style dennerStyle = null;

	private static void setUp() {

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);

	}

	private static void setupStyles() {

		System.out.print("Setting up KML styles...");
		
		coopStyle = new Style("coopStyle");
		myKMLDocument.addStyle(coopStyle);
		coopStyle.setIconStyle(
				new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/C.png")));

		pickpayStyle = new Style("pickpayStyle");
		myKMLDocument.addStyle(pickpayStyle);
		pickpayStyle.setIconStyle(
				new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/P.png")));

		migrosStyle = new Style("migrosStyle");
		myKMLDocument.addStyle(migrosStyle);
		migrosStyle.setIconStyle(
				new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/M.png")));

		dennerStyle = new Style("dennerStyle");
		myKMLDocument.addStyle(dennerStyle);
		dennerStyle.setIconStyle(
				new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/D.png")));

		System.out.println("done.");
		
	}

	private static void dennerTGZHAddressesToKML() {
		
		System.out.print("Setting up Denner shops...");

		Folder aFolder = null;
		Placemark aShop = null;

		aFolder = new Folder(
				"dennerFolder",
				"Denner TG ZH",
				"Alle Denner TG ZH Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(aFolder);	
		
		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(dennerTGZHFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}
	
		// Java regex has to match ENTIRE string rather than "quick match" in most libraries
		// for a discussion see
		// http://www.regular-expressions.info/java.html
		String beginsWith3Digits = "^[0-9]{3}.*$";
		boolean nextLineIsTheAddressLine = false;
		String city = null;
		String street = null;
		String id = null;
		
		for (String line : lines) {

			if (nextLineIsTheAddressLine) {
				
				nextLineIsTheAddressLine = false;

				//System.out.println(line);
				tokens = line.split(",");
				street = tokens[9];

				aShop = new Placemark(
						"denner_" + id,
						"Denner " + city + " " + street,
						Feature.DEFAULT_DESCRIPTION,
						street + ", " + city + ", Schweiz",
						Feature.DEFAULT_LOOK_AT,
						dennerStyle.getStyleUrl(),
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);

				aFolder.addFeature(aShop);

			}

			if (Pattern.matches(beginsWith3Digits, line)) {

				nextLineIsTheAddressLine = true;
				
				//System.out.println(line);
				tokens = line.split(",");
				id = tokens[0];
				city = tokens[9];

			}
			
		}

		System.out.println("done.");

	}	
	
	private static void coopZHAddressesToKML() {

		System.out.print("Setting up Coop Züri shops...");

		Folder coopFolder = null;
		Placemark aCoop = null;

		coopFolder = new Folder(
				"coopZHFolder",
				"Coop ZH",
				"Alle Coop ZH Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(coopFolder);	

		List<String> lines = null;
		String[] tokens = null;
		String VSTTyp = null;

		try {

			lines = FileUtils.readLines(new File(coopZHFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(",");

			VSTTyp = tokens[7];
			if (
					VSTTyp.equals("CC") || 
					VSTTyp.equals("CL") ||
					VSTTyp.equals("CSC") ||
					VSTTyp.equals("M")) {

				aCoop = new Placemark(
						"coop_" + VSTTyp + "_" + tokens[8],
						"Coop " + VSTTyp + " " + tokens[8],
						Feature.DEFAULT_DESCRIPTION,
						tokens[9] + ", " + tokens[11] + ", Schweiz",
						Feature.DEFAULT_LOOK_AT,
						coopStyle.getStyleUrl(),
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);

				coopFolder.addFeature(aCoop);

			}

		}
		
		System.out.println("done.");

	}

	private static void coopTGAddressesToKML() {

		System.out.print("Setting up Coop Thurgau shops...");

		Folder coopFolder = null;
		Placemark aCoop = null;

		coopFolder = new Folder(
				"coopTGFolder",
				"Coop TG",
				"Alle Coop TG Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(coopFolder);	

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(coopTGFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(",");

			aCoop = new Placemark(
					"coop_" + tokens[0],
					"Coop " + tokens[0],
					Feature.DEFAULT_DESCRIPTION,
					tokens[1] + ", " + tokens[2] + ", Schweiz",
					Feature.DEFAULT_LOOK_AT,
					coopStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			coopFolder.addFeature(aCoop);

		}
		
		System.out.println("done.");

	}

	private static void pickPayAddressesToKML() {

		Folder pickpayFolder = null;
		Placemark aPickpay = null;

		pickpayFolder = new Folder(
				"pickpayFolder",
				"Pickpay",
				"Alle Pickpay Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(pickpayFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(pickPayAdressesFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

//			System.out.println(line);
			tokens = line.split(",");

			aPickpay = new Placemark(
					"pickpay_" + tokens[0],
					"Pickpay " + tokens[1],
					Feature.DEFAULT_DESCRIPTION,
					tokens[2] + ", " + tokens[5] + ", Schweiz",
					Feature.DEFAULT_LOOK_AT,
					pickpayStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			pickpayFolder.addFeature(aPickpay);

		}

		System.out.println("done.");

	}

	private static void migrosZHAdressesToKML() {
		
		System.out.print("Setting up Migros ZH shops...");

		Folder migrosZHFolder = null;
		Placemark aMigrosZH = null;

		migrosZHFolder = new Folder(
				"migrosZHFolder",
				"Migros ZH",
				"Alle Migros ZH Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(migrosZHFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(migrosZHFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(",");

			aMigrosZH = new Placemark(
					"migrosZH_" + tokens[1],
					"Migros " + tokens[1],
					Feature.DEFAULT_DESCRIPTION,
					tokens[2] + ", " + tokens[3] + ", Schweiz",
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosZHFolder.addFeature(aMigrosZH);

		}

		System.out.println("done.");

	}
	
	private static void migrosOstschweizAdressesToKML() {
		
		System.out.print("Setting up Migros Ostschweiz shops...");

		Folder migrosOstschweizFolder = null;
		Placemark aMigrosOstschweiz = null;

		migrosOstschweizFolder = new Folder(
				"migrosOstschweizFolder",
				"Migros Ostschweiz",
				"Alle Migros Ostschweiz Läden",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(migrosOstschweizFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(migrosOstschweizFilename), null);

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(",");

			aMigrosOstschweiz = new Placemark(
					"migrosOstschweiz_" + tokens[1],
					tokens[0] + " " + tokens[1],
					Feature.DEFAULT_DESCRIPTION,
					tokens[5] + ", " + tokens[6] + ", Schweiz",
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosOstschweizFolder.addFeature(aMigrosOstschweiz);

		}

		System.out.println("done.");

		
	}

	private static void readPickPayOpenTimes() {

		boolean nextLineIsACloseLine = false;

		List<String> lines = null;
		String[] tokens = null;

		final String OPEN = "Auf";
		final String CLOSE = "Zu";

		String openLinePattern = "\t" + OPEN + "\t";
		String closeLinePattern = "\t" + CLOSE + "\t";
		String anythingButDigits = "[^0-9]";

		try {
			lines = FileUtils.readLines(new File(pickPayOpenTimesFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			if (line.contains(openLinePattern)) {

				tokens = line.split(anythingButDigits);
				System.out.println(tokens[0]);
				System.out.print("Auf:\t");
				for (String token : tokens) {
					if (!token.equals("") && !token.equals(tokens[0])) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();

			} else if (line.contains(closeLinePattern)) {

				tokens = line.split(anythingButDigits);
				System.out.print("Zu:\t");
				for (String token : tokens) {
					if (!token.equals("")) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();

			}

		}

		System.out.println("done.");

	}

	private static void write() {

		System.out.print("Writing KML files out...");

		KMZWriter writer;
		writer = new KMZWriter(kmlFilename);
		writer.writeMainKml(myKML);
		writer.close();

		System.out.println("done.");

	}

	private static void prepareRawDataForGeocoding() {
		
		ShopsOf2005ToFacilities.setUp();
		ShopsOf2005ToFacilities.setupStyles();
		ShopsOf2005ToFacilities.dennerTGZHAddressesToKML();
		ShopsOf2005ToFacilities.pickPayAddressesToKML();
		ShopsOf2005ToFacilities.coopZHAddressesToKML();
		ShopsOf2005ToFacilities.coopTGAddressesToKML();
		ShopsOf2005ToFacilities.migrosZHAdressesToKML();
		ShopsOf2005ToFacilities.migrosOstschweizAdressesToKML();
		ShopsOf2005ToFacilities.write();		
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ShopsOf2005ToFacilities.prepareRawDataForGeocoding();

//		ShopsOf2005ToFacilities.readPickPayOpenTimes();

	}

}
