/* *********************************************************************** *
 * project: org.matsim.*
 * ShopsOf2005ToFacilities.java
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Style;

import com.google.earth.kml._2.AbstractFeatureType;
import com.google.earth.kml._2.AbstractGeometryType;
import com.google.earth.kml._2.DocumentType;
import com.google.earth.kml._2.FolderType;
import com.google.earth.kml._2.KmlType;
import com.google.earth.kml._2.PlacemarkType;
import com.google.earth.kml._2.PointType;

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
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final String FIELD_DELIM = ";";

	private static final String SANDBOX_NAME = "sandbox00";
	private static final String SHOPS_CVS_MODULE = "ivt/studies/switzerland/facilities/shopsOf2005";
	private static final String SHOPS_PATH = HOME_DIR + FILE_SEPARATOR + SANDBOX_NAME + FILE_SEPARATOR + SHOPS_CVS_MODULE + FILE_SEPARATOR;

	private static String pickPayOpenTimesFilename = SHOPS_PATH + "pickpay_opentimes.txt";
	private static String pickPayAdressesFilename = SHOPS_PATH + "pickpay_addresses.csv";
	private static String coopZHFilename = SHOPS_PATH + "coop-zh.csv";
	private static String coopTGFilename = SHOPS_PATH + "coop-tg.csv";
	private static String migrosZHFilename = SHOPS_PATH + "migros-zh.csv";
	private static String migrosOstschweizFilename = SHOPS_PATH + "migros-ostschweiz-filialen.csv";
	private static String dennerTGZHFilename = SHOPS_PATH + "denner-tg-zh.csv";

	private static final String ACTIVITY_TYPE_SHOP = "shop";

	private static KML myKML = null;
	private static Document myKMLDocument = null;
	private static Folder mainKMLFolder = null;

	private static String kmlFilename = "output" + FILE_SEPARATOR + "shopsOf2005.kmz";

	private static Style coopStyle = null;
	private static Style pickpayStyle = null;
	private static Style migrosStyle = null;
	private static Style dennerStyle = null;

	public enum Day {
		MONDAY ("mon"),
		TUESDAY ("tue"),
		WEDNESDAY ("wed"),
		THURSDAY ("thu"),
		FRIDAY ("fri"),
		SATURDAY ("sat"),
		SUNDAY ("sun");

		private final String abbrev;

		Day(String abbrev) {
			this.abbrev = abbrev;
		}

		public String getAbbrev() {
			return abbrev;
		}
	}

	private static void setUp() {

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);
		mainKMLFolder = new Folder(
				"main shops KML folder", 
				"Shops of 2005", 
				"All revealed shops of 2005.", 
				Feature.DEFAULT_ADDRESS, 
				Feature.DEFAULT_LOOK_AT, 
				Feature.DEFAULT_STYLE_URL, 
				Feature.DEFAULT_VISIBILITY, 
				Feature.DEFAULT_REGION, 
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(mainKMLFolder);

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

		mainKMLFolder.addFeature(aFolder);	

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(dennerTGZHFilename), "UTF-8");

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
				tokens = line.split(FIELD_DELIM);
				street = tokens[9];

				aShop = new Placemark(
						buildDennerId(city, street),
						buildDennerId(city, street),
						buildDennerDescription(city, street),
						buildAddress(street, "", city),
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
				tokens = line.split(FIELD_DELIM);
				id = tokens[0];
				city = tokens[9];

			}

		}

		System.out.println("done.");

	}	

	private static String buildDennerId(String city, String street) {
		return "Denner_" + city + "_" + street;
	}

	private static String buildDennerDescription(String city, String street) {
		return "Denner " + city + " " + street;
	}

	private static String buildAddress(String street, String postcode, String city) {
		return street + ", " + postcode + " " + city + ", Schweiz";
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

		mainKMLFolder.addFeature(coopFolder);	

		List<String> lines = null;
		String[] tokens = null;
		String VSTTyp = null;

		try {

			lines = FileUtils.readLines(new File(coopZHFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[0].equals("OB")) {
				continue;
			}

			VSTTyp = tokens[7];
			if (
					VSTTyp.equals("CC") || 
					VSTTyp.equals("CL") ||
					VSTTyp.equals("CSC") ||
					VSTTyp.equals("M")) {

				aCoop = new Placemark(
						buildCoopZHId(tokens),
						buildCoopZHId(tokens),
						buildCoopZHDescription(tokens),
						buildAddress(tokens[42], tokens[43], tokens[44]),
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

	private static String buildCoopZHId(String[] tokens) {
		String VSTTyp = tokens[7];
		String name = tokens[8];

		return "coop_" + VSTTyp + "_" + name;
	}

	private static String buildCoopZHDescription(String[] tokens) {
		String VSTTyp = tokens[7];
		String name = tokens[8];

		return "Coop " + VSTTyp + " " + name;
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

		mainKMLFolder.addFeature(coopFolder);	

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(coopTGFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[0].equals("Verkaufsstelle")) {
				continue;
			}

			aCoop = new Placemark(
					buildCoopTGId(tokens),
					buildCoopTGId(tokens),
					buildCoopTGDescription(tokens),
					buildAddress(tokens[1], "", tokens[2]),
					Feature.DEFAULT_LOOK_AT,
					coopStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			coopFolder.addFeature(aCoop);

		}

		System.out.println("done.");

	}

	private static String buildCoopTGId(String[] tokens) {
		String name = tokens[0];

		return "coop_" + name;
	}

	private static String buildCoopTGDescription(String[] tokens) {
		String name = tokens[0];

		return "Coop " + name;
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

		mainKMLFolder.addFeature(pickpayFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(pickPayAdressesFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

//			System.out.println(line);
			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[0].equals("Filialnummer")) {
				continue;
			}

			aPickpay = new Placemark(
					buildPickpayId(tokens),
					buildPickpayId(tokens),
					buildPickpayDescription(tokens),
					buildAddress(tokens[2], tokens[4], tokens[5]),
					Feature.DEFAULT_LOOK_AT,
					pickpayStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			pickpayFolder.addFeature(aPickpay);

		}

		System.out.println("done.");

	}

	private static String buildPickpayId(String[] tokens) {
		String number = tokens[0];
		// remove leading 0's
		while (number.charAt(0) == '0') {
			number = number.substring(1);
		}

		return "pickpay_" + number;
	}

	private static String buildPickpayDescription(String[] tokens) {
		String name = tokens[1];

		return "Pickpay " + name;
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

		mainKMLFolder.addFeature(migrosZHFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(migrosZHFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[0].equals("KST")) {
				continue;
			}

			aMigrosZH = new Placemark(
					buildMigrosZHId(tokens),
					buildMigrosZHId(tokens),
					buildMigrosZHDescription(tokens),
					buildAddress(tokens[2], "", tokens[3]),
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosZHFolder.addFeature(aMigrosZH);

		}

		System.out.println("done.");

	}

	private static String buildMigrosZHId(String[] tokens) {
		String name = tokens[1];

		return "migrosZH_" + name;
	}

	private static String buildMigrosZHDescription(String[] tokens) {
		String name = tokens[1];

		return "Migros (ZH) " + name;
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

		mainKMLFolder.addFeature(migrosOstschweizFolder);

		List<String> lines = null;
		String[] tokens = null;

		try {

			lines = FileUtils.readLines(new File(migrosOstschweizFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			//System.out.println(line);
			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[0].equals("VST-Typ")) {
				continue;
			}

			aMigrosOstschweiz = new Placemark(
					buildMigrosOstschweizId(tokens),
					buildMigrosOstschweizId(tokens),
					buildMigrosOstschweizDescription(tokens),
					buildAddress(tokens[7].trim(), "", tokens[8].trim()),
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosOstschweizFolder.addFeature(aMigrosOstschweiz);

		}

		System.out.println("done.");


	}

	private static String buildMigrosOstschweizId(String[] tokens) {
		String name = tokens[1].trim();

		return "migrosOstschweiz_" + name;
	}

	private static String buildMigrosOstschweizDescription(String[] tokens) {
		String name = tokens[1].trim();

		return "Migros (Ostschweiz) " + name;
	}

	private static void processPickPayOpenTimes(Facilities facilities) {

		List<String> lines = null;
		String[] openTokens = null, closeTokens = null;
		TreeMap<String, String> aPickpayOpentime = new TreeMap<String, String>();
		String facilityId = null; 

		final String OPEN = "Auf";
		final String CLOSE = "Zu";

		//String openLinePattern = "\t" + OPEN + "\t";
		String openLinePattern = ".*\\s" + OPEN + "\\s.*";
		String closeLinePattern = ".*\\s" + CLOSE + "\\s.*";
		String anythingButDigits = "[^0-9]";

		try {
			lines = FileUtils.readLines(new File(pickPayOpenTimesFilename), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// remember relevant lines only
		String key = null;
		for (String line : lines) {
			
//			if (line.contains(openLinePattern)) {
			if (line.matches(openLinePattern)) {
				key = line;
//				System.out.println(line);
			} else if (line.matches(closeLinePattern)) {
				if (!aPickpayOpentime.containsKey(key)) {
					aPickpayOpentime.put(key, line);
				}
//				System.out.println(line);
			}
			
		}
		
		for (String openLine : aPickpayOpentime.keySet()) {
			
			openTokens = openLine.split(anythingButDigits);
//			System.out.println(openLine);
			facilityId = ShopsOf2005ToFacilities.buildPickpayId(openTokens);
			System.out.println(facilityId);
			Facility theCurrentPickpay = (Facility) facilities.getLocation(facilityId);
			if (theCurrentPickpay != null) {
				theCurrentPickpay.createActivity(ACTIVITY_TYPE_SHOP);

				System.out.print(OPEN + ":\t");
				for (String token : openTokens) {
					if (!token.equals("") && !token.equals(openTokens[0])) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();
				
				//System.out.println(aPickpayOpentime.get(openLine));
				closeTokens = aPickpayOpentime.get(openLine).split(anythingButDigits);
				System.out.print(CLOSE + ":\t");
				for (String token : closeTokens) {
					if (!token.equals("")) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();
				
			} else {
				System.out.println("A pickpay with id " + facilityId + " does not exist.");
			}
			
		}
		
//		for (String line : lines) {
//
//			if (line.contains(openLinePattern)) {
//
//				tokens = line.split(anythingButDigits);
//				//System.out.println(tokens[0]);
//				facilityId = ShopsOf2005ToFacilities.buildPickpayId(tokens);
//				System.out.println(facilityId);
//				Facility theCurrentPickpay = (Facility) facilities.getLocation(facilityId);
//				theCurrentPickpay.createActivity(ACTIVITY_TYPE_SHOP);
//
//				System.out.print(OPEN + ":\t");
//				for (String token : tokens) {
//					if (!token.equals("") && !token.equals(tokens[0])) {
//						System.out.print(token + "\t");
//					}
//				}
//				System.out.println();
//
//			} else if (line.contains(closeLinePattern)) {
//
//				tokens = line.split(anythingButDigits);
//				System.out.print(CLOSE + ":\t");
//				for (String token : tokens) {
//					if (!token.equals("")) {
//						System.out.print(token + "\t");
//					}
//				}
//				System.out.println();
//
//			}
//
//		}

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

	private static void transformGeocodedKMLToFacilities() {

		Facilities shopsOf2005 = new Facilities("shopsOf2005");

		try {

			JAXBContext jaxbContext = JAXBContext.newInstance("com.google.earth.kml._2");
			Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
			JAXBElement<KmlType> kmlElement = (JAXBElement<KmlType>) unMarshaller.unmarshal(new FileInputStream("/home/konrad/workspace/MATSim/input/shopsOf2005_geocoded.kml"));

			KmlType kml = kmlElement.getValue();
			DocumentType document = (DocumentType) kml.getAbstractFeatureGroup().getValue();
			System.out.println(document.getName());

			// recursively search the KML for placemarks, transform it into a matsim facility 
			// and place it in the list of facilities or in the quadtree, respectively

			List<JAXBElement<? extends AbstractFeatureType>> featureGroup = document.getAbstractFeatureGroup();
			Iterator<JAXBElement<? extends AbstractFeatureType>> it = featureGroup.iterator();
			while (it.hasNext()) {
				JAXBElement<AbstractFeatureType> feature = (JAXBElement<AbstractFeatureType>) (it.next());
				if (feature.getValue().getClass().equals(FolderType.class)) {
					//System.out.println("Going into folder...");
					ShopsOf2005ToFacilities.extractPlacemarks((FolderType) feature.getValue(), shopsOf2005);
				} else if (feature.getValue().getClass().equals(PlacemarkType.class)) {
					//System.out.println("There is a placemark!");
				}

			}

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ShopsOf2005ToFacilities.addOpentimesToFacilities(shopsOf2005);

		System.out.println("Writing facilities xml file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(shopsOf2005);
		facilities_writer.write();
		System.out.println("Writing facilities xml file...done.");

	}

	private static void extractPlacemarks(FolderType folderType, Facilities facilities) {

		List<JAXBElement<? extends AbstractFeatureType>> featureGroup = folderType.getAbstractFeatureGroup();
		Iterator it = featureGroup.iterator();
		while (it.hasNext()) {
			JAXBElement<AbstractFeatureType> feature = (JAXBElement<AbstractFeatureType>) (it.next());
			if (feature.getValue().getClass().equals(FolderType.class)) {
				System.out.println("Going into folder...");
				ShopsOf2005ToFacilities.extractPlacemarks((FolderType) feature.getValue(), facilities);
			} else if (feature.getValue().getClass().equals(PlacemarkType.class)) {
				System.out.println("There is a placemark!");

				PlacemarkType placemark = (PlacemarkType) feature.getValue();
				JAXBElement<? extends AbstractGeometryType> geometry = placemark.getAbstractGeometryGroup();
				// only process if coordinates exist
				if (geometry != null) {
					PointType point = (PointType) geometry.getValue();
					String name = placemark.getName();

					// transform coordinates
					String[] coordinates = point.getCoordinates().get(0).split(",");
					//System.out.println(point.getCoordinates().get(0));
					CoordI wgs84Coords = new Coord(coordinates[0], coordinates[1]);
					WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
					CoordI ch1903Coordinates = trafo.transform(wgs84Coords);

//					// create facility
					Facility newFacility = facilities.createFacility(new Id(name), ch1903Coordinates);
				}
			}

		}

	}

	private static void addOpentimesToFacilities(Facilities facilities) {

		ShopsOf2005ToFacilities.processPickPayOpenTimes(facilities);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.createConfig(args);

//		ShopsOf2005ToFacilities.prepareRawDataForGeocoding();
		ShopsOf2005ToFacilities.transformGeocodedKMLToFacilities();
		
	}

}
