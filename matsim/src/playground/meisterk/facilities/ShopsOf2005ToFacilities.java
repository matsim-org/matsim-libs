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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.facilities.algorithms.FacilitiesWriterAlgorithm;
import org.matsim.facilities.algorithms.FacilityAlgorithmI;
import org.matsim.facilities.filters.FacilitiesActTypeFilter;
import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Style;

import com.google.earth.kml._2.AbstractFeatureType;
import com.google.earth.kml._2.AbstractGeometryType;
import com.google.earth.kml._2.BasicLinkType;
import com.google.earth.kml._2.BoundaryType;
import com.google.earth.kml._2.DocumentType;
import com.google.earth.kml._2.FolderType;
import com.google.earth.kml._2.IconStyleType;
import com.google.earth.kml._2.KmlType;
import com.google.earth.kml._2.LinearRingType;
import com.google.earth.kml._2.ObjectFactory;
import com.google.earth.kml._2.PlacemarkType;
import com.google.earth.kml._2.PointType;
import com.google.earth.kml._2.PolyStyleType;
import com.google.earth.kml._2.PolygonType;
import com.google.earth.kml._2.StyleType;
import com.google.earth.kml._2.TimeSpanType;

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

	public enum Day {
		MONDAY ("mon", "Mo"),
		TUESDAY ("tue", "Di"),
		WEDNESDAY ("wed", "Mi"),
		THURSDAY ("thu", "Do"),
		FRIDAY ("fri", "Fr"),
		SATURDAY ("sat", "Sa"),
		SUNDAY ("sun", "So");

		private final String abbrevEnglish;
		private final String abbrevGerman;

		Day(String abbrevEnglish, String abbrevGerman) {
			this.abbrevEnglish = abbrevEnglish;
			this.abbrevGerman = abbrevGerman;
		}

		public String getAbbrevGerman() {
			return abbrevGerman;
		}

		public String getAbbrevEnglish() {
			return abbrevEnglish;
		}

		public static Day getDayByGermanAbbrev(String germanAbbrev) {

			Day theDay = null;

			Day[] days = Day.values();
			for (Day day : days) {
				if (day.getAbbrevGerman().equals(germanAbbrev)) {
					theDay = day;
				}
			}

			return theDay;

		}

	}

	private static final String HOME_DIR = System.getenv("HOME");
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final String FIELD_DELIM = ";";

	private static final String SANDBOX_NAME = "sandbox00";
	private static final String SHOPS_CVS_MODULE = "ivt/studies/switzerland/facilities/shopsOf2005";
	private static final String SHOPS_PATH = HOME_DIR + FILE_SEPARATOR + SANDBOX_NAME + FILE_SEPARATOR + SHOPS_CVS_MODULE + FILE_SEPARATOR;

	private static final String PICKPAY = "Pick Pay";
	private static String pickPayOpenTimesFilename = SHOPS_PATH + "pickpay_opentimes.txt";
	private static String pickPayAdressesFilename = SHOPS_PATH + "pickpay_addresses.csv";

	private static final String COOP = "Coop";
	private static final String COOP_ZH = "Coop Zürich";
	private static final String COOP_TG = "Coop Thurgau";
	private static String coopZHFilename = SHOPS_PATH + "coop-zh.csv";
	private static String coopTGFilename = SHOPS_PATH + "coop-tg.csv";

	private static final String MIGROS = "Migros";
	private static final String MIGROS_ZH = MIGROS + " Zürich";
	private static String migrosZHFilename = SHOPS_PATH + "migros-zh.csv";
	private static final String MIGROS_OSTSCHWEIZ = MIGROS + " Ostschweiz";
	private static String migrosOstschweizAdressesFilename = SHOPS_PATH + "migros-ostschweiz-filialen.csv";
	private static String migrosOstschweizOpenTimesFilename = SHOPS_PATH + "migros-ostschweiz-oeffnungszeiten.csv";

	private static final String DENNER = "Denner";
	private static String dennerTGZHFilename = SHOPS_PATH + "denner-tg-zh.csv";

	private static final String ACTIVITY_TYPE_SHOP = "shop";

	private static final String ANYTHING_BUT_DIGITS = "[^0-9]";
	private static final String ANYTHING_BUT_LETTERS = "[^a-zA-Z]";
	private static final String BEGINS_WITH_3_DIGITS = "^[0-9]{3}.*$";

	private static final String FAX = "FAX";
	private static final String CONTAINS_FAX = ".*" + FAX + ".*";
	private static final String SATURDAY = ";SA    ";
	private static final String CONTAINS_SATURDAY = ".*" + SATURDAY + ".*";
	private static final String PALETTE = "PALETTE";
	private static final String CONTAINS_PALETTE = ".*" + PALETTE + ".*";

	private static KML myKML = null;
	private static Document myKMLDocument = null;
	private static Folder mainKMLFolder = null;

	private static String kmlFilename = "output" + FILE_SEPARATOR + "shopsOf2005.kml";

	private static Style coopStyle = null;
	private static Style pickpayStyle = null;
	private static Style migrosStyle = null;
	private static Style dennerStyle = null;

	private static ShopId shopId = null;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.createConfig(args);

//		ShopsOf2005ToFacilities.prepareRawDataForGeocoding();
//		ShopsOf2005ToFacilities.transformGeocodedKMLToFacilities();
//		ShopsOf2005ToFacilities.shopsToTXT();
		ShopsOf2005ToFacilities.shopsToOpentimesKML();
//		ShopsOf2005ToFacilities.applyOpentimesToEnterpriseCensus();

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

		Facilities shopsOf2005 = new Facilities("shopsOf2005", Facilities.FACILITIES_NO_STREAMING);

		JAXBElement<KmlType> kmlElement = null;

		try {

			JAXBContext jaxbContext = JAXBContext.newInstance("com.google.earth.kml._2");
			Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
			kmlElement = (JAXBElement<KmlType>) unMarshaller.unmarshal(new FileInputStream(Gbl.getConfig().facilities().getInputFile()));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		ShopsOf2005ToFacilities.addOpentimesToFacilities(shopsOf2005);

		System.out.println("Writing facilities xml file... ");
		FacilitiesWriter facilities_writer = new FacilitiesWriter(shopsOf2005);
		facilities_writer.write();
		System.out.println("Writing facilities xml file...done.");

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

		System.out.println("Setting up Denner shops...");

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
//		String beginsWith3Digits = "^[0-9]{3}.*$";
		boolean nextLineIsTheAddressLine = false;
		String city = null;
		String street = null;
		String postcode = null;

		for (String line : lines) {

			if (nextLineIsTheAddressLine) {

				nextLineIsTheAddressLine = false;

				//System.out.println(line);
				tokens = line.split(FIELD_DELIM);
				street = tokens[9];
				shopId = new ShopId(DENNER, "", "", "", postcode, city, street);

				aShop = new Placemark(
						shopId.getShopId(),
						shopId.getShopId(),
						shopId.getShopId(),
						shopId.getAddressForGeocoding(),
						Feature.DEFAULT_LOOK_AT,
						dennerStyle.getStyleUrl(),
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);

				aFolder.addFeature(aShop);

			}

			if (Pattern.matches(BEGINS_WITH_3_DIGITS, line)) {

				nextLineIsTheAddressLine = true;

				//System.out.println(line);
				tokens = line.split(FIELD_DELIM);
				postcode = tokens[9].split(" ")[0];
				city = tokens[9].split(" ")[1];

			}

		}

		System.out.println("Setting up Denner shops...done.");

	}	

	private static void coopZHAddressesToKML() {

		System.out.println("Setting up Coop Züri shops...");

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

				shopId = new ShopId(COOP, VSTTyp, tokens[8], COOP_ZH, tokens[43], tokens[44], tokens[42]);

				aCoop = new Placemark(
						shopId.getShopId(),
						shopId.getShopId(),
						shopId.getShopId(),
						shopId.getAddressForGeocoding(),
						Feature.DEFAULT_LOOK_AT,
						coopStyle.getStyleUrl(),
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);

				coopFolder.addFeature(aCoop);

			}

		}

		System.out.println("Setting up Coop Züri shops...done.");

	}

	private static void coopTGAddressesToKML() {

		System.out.println("Setting up Coop Thurgau shops...");

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

			shopId = new ShopId(
					COOP, 
					"", 
					tokens[0], 
					COOP_TG, 
					tokens[2].split(" ")[0], 
					tokens[2].split(" ")[1], 
					tokens[1]);

			aCoop = new Placemark(
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getAddressForGeocoding(),
					Feature.DEFAULT_LOOK_AT,
					coopStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			coopFolder.addFeature(aCoop);

		}

		System.out.println("Setting up Coop Thurgau shops...done.");

	}


	private static void pickPayAddressesToKML() {

		System.out.println("Setting up Pick Pay shops...");

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

			shopId = new ShopId(PICKPAY, "", tokens[1], "", tokens[4], tokens[5], tokens[2]);

			aPickpay = new Placemark(
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getAddressForGeocoding(),
					Feature.DEFAULT_LOOK_AT,
					pickpayStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			pickpayFolder.addFeature(aPickpay);

		}

		System.out.println("Setting up Pick Pay shops...done.");

	}

	private static void migrosZHAdressesToKML() {

		System.out.println("Setting up Migros ZH shops...");

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

			shopId = new ShopId(MIGROS, "", tokens[1], MIGROS_ZH, tokens[3].split(" ")[0], tokens[3].split(" ")[1], tokens[2]);

			aMigrosZH = new Placemark(
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getAddressForGeocoding(),
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosZHFolder.addFeature(aMigrosZH);

		}

		System.out.println("Setting up Migros ZH shops...");

	}

	private static void migrosOstschweizAdressesToKML() {

		System.out.println("Setting up Migros Ostschweiz shops...");

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

			lines = FileUtils.readLines(new File(migrosOstschweizAdressesFilename), "UTF-8");

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

			shopId = new ShopId(
					MIGROS, 
					tokens[0].trim(), 
					tokens[1].trim(),
					MIGROS_OSTSCHWEIZ,
					tokens[8].trim().split(" ")[0],
					tokens[8].trim().split(" ")[1],
					tokens[7].trim()
			);

			aMigrosOstschweiz = new Placemark(
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getShopId(),
					shopId.getAddressForGeocoding(),
					Feature.DEFAULT_LOOK_AT,
					migrosStyle.getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);

			migrosOstschweizFolder.addFeature(aMigrosOstschweiz);

		}

		System.out.println("Setting up Migros Ostschweiz shops...");

	}

	private static void write() {

		System.out.print("Writing KML files out...");

		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(myKML, kmlFilename, KMLWriter.DEFAULT_XMLNS, false);
		myKMLDocumentWriter.write();
//		KMZWriter writer;
//		writer = new KMZWriter(kmlFilename);
//		writer.writeMainKml(myKML);
//		writer.close();

		System.out.println("done.");

	}

	private static void extractPlacemarks(FolderType folderType, Facilities facilities) {

		List<JAXBElement<? extends AbstractFeatureType>> featureGroup = folderType.getAbstractFeatureGroup();
		Iterator it = featureGroup.iterator();
		while (it.hasNext()) {
			JAXBElement<AbstractFeatureType> feature = (JAXBElement<AbstractFeatureType>) (it.next());
			if (feature.getValue().getClass().equals(FolderType.class)) {
				//System.out.println("Going into folder...");
				ShopsOf2005ToFacilities.extractPlacemarks((FolderType) feature.getValue(), facilities);
			} else if (feature.getValue().getClass().equals(PlacemarkType.class)) {
				//System.out.println("There is a placemark!");

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

					// round coordinates to meters
					ch1903Coordinates.setXY((int) ch1903Coordinates.getX(), (int) ch1903Coordinates.getY()); 

//					// create facility
					if (name.equals("migrosZH_Glarus")) {
						System.out.println("There it is: " + name);
						System.out.flush();
					}
					Facility newFacility = facilities.createFacility(new IdImpl(name), ch1903Coordinates);
				}
			}

		}

	}

	private static void addOpentimesToFacilities(Facilities facilities) {

		ShopsOf2005ToFacilities.processPickPayOpenTimes(facilities);
		ShopsOf2005ToFacilities.processMigrosZHOpenTimes(facilities);
		ShopsOf2005ToFacilities.processMigrosOstschweizOpenTimes(facilities);
		ShopsOf2005ToFacilities.processCoopZHOpenTimes(facilities);
		ShopsOf2005ToFacilities.processCoopTGOpenTimes(facilities);
		ShopsOf2005ToFacilities.processDennerOpenTimes(facilities);

	}

	private static void processPickPayOpenTimes(Facilities facilities) {

		System.out.println("Setting up Pickpay open times...");

		List<String> openTimeLines = null;
		List<String> addressLines = null;
		String[] openTokens = null, closeTokens = null;
		String[] addressTokens = null;
		Vector<Integer> openNumbers = new Vector<Integer>();
		Vector<Integer> closeNumbers = new Vector<Integer>();
		TreeMap<String, String> aPickpayOpentime = new TreeMap<String, String>();
		String facilityId = null;
		int addressLinePointer = 1; // ignore header line

		final String OPEN = "Auf";
		final String CLOSE = "Zu";

		String openLinePattern = ".*\\s" + OPEN + "\\s.*";
		String closeLinePattern = ".*\\s" + CLOSE + "\\s.*";

		try {
			openTimeLines = FileUtils.readLines(new File(pickPayOpenTimesFilename), "UTF-8");
			addressLines = FileUtils.readLines(new File(pickPayAdressesFilename), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// remember relevant lines only
		String key = null;
		for (String line : openTimeLines) {

			if (line.matches(openLinePattern)) {
				key = line;
			} else if (line.matches(closeLinePattern)) {
				if (!aPickpayOpentime.containsKey(key)) {
					aPickpayOpentime.put(key, line);
				}
			}

		}

		for (String openLine : aPickpayOpentime.keySet()) {

			openTokens = openLine.split(ANYTHING_BUT_DIGITS);
			addressTokens = addressLines.get(addressLinePointer).split(FIELD_DELIM);
			shopId = new ShopId(PICKPAY, "", addressTokens[1], "", addressTokens[4], addressTokens[5], addressTokens[2]);
			addressLinePointer++;

			facilityId = shopId.getShopId();
			//System.out.println(facilityId);
			Facility theCurrentPickpay = (Facility) facilities.getLocation(facilityId);
			if (theCurrentPickpay != null) {

				// yeah, we can use the open times

				Activity shopping = theCurrentPickpay.createActivity(ACTIVITY_TYPE_SHOP);
				openNumbers.clear();
				closeNumbers.clear();

				// print out and extract numbers

				//System.out.print(OPEN + ":\t");
				for (String token : openTokens) {
					if (!token.equals("") && !token.equals(openTokens[0])) {
						openNumbers.add(Integer.parseInt(token));
						//System.out.print(token + "\t");
					}
				}
				//System.out.println();

				closeTokens = aPickpayOpentime.get(openLine).split(ANYTHING_BUT_DIGITS);
				//System.out.print(CLOSE + ":\t");
				for (String token : closeTokens) {
					if (!token.equals("")) {
						closeNumbers.add(Integer.parseInt(token));
						//System.out.print(token + "\t");
					}
				}
				//System.out.println();

				// now process numbers

				//String day = "wkday";
				Day[] days = Day.values();
				int dayPointer = 0;
				Opentime opentime = null;
				int openSeconds = 0; 
				int closeSeconds = 0;
				int previousOpenSeconds = 0;

				if (openNumbers.size() == closeNumbers.size()) {
					for (int ii=0; ii < openNumbers.size(); ii += 2) {

						openSeconds = openNumbers.get(ii) * 3600 + openNumbers.get(ii + 1) * 60;
						closeSeconds = closeNumbers.get(ii) * 3600 + closeNumbers.get(ii + 1) * 60;

						// check if a new day starts
						if (openSeconds <= previousOpenSeconds) {
							dayPointer++;
						}

						previousOpenSeconds = openSeconds;
						String openTimeString = Time.writeTime(openSeconds);
						String closeTimeString = Time.writeTime(closeSeconds);

						opentime = new Opentime(days[dayPointer].getAbbrevEnglish(), openTimeString, closeTimeString);

						shopping.addOpentime(opentime);

					}
				} else {
					Gbl.errorMsg("openNumbers[] and closeNumbers[] have different size. Aborting...");
				}

				System.out.flush();

			} else {
				System.out.println("A pickpay with id " + facilityId + " does not exist.");
			}

		}

		System.out.println("Setting up Pickpay open times...done.");

	}

	private static void processMigrosZHOpenTimes(Facilities facilities) {

		System.out.println("Setting up Migros ZH open times...");

		List<String> lines = null;
		String[] tokens = null;
		String[] openHourTokens = null;
		String[] openDayTokens = null;
		Vector<Integer> numbers = new Vector<Integer>();

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

			shopId = new ShopId(MIGROS, "", tokens[1], MIGROS_ZH, tokens[3].split(" ")[0], tokens[3].split(" ")[1], tokens[2]);
			String facilityId = shopId.getShopId();
			System.out.println(facilityId);
			System.out.flush();

			Facility theCurrentMigrosZH = (Facility) facilities.getLocation(facilityId);
			if (theCurrentMigrosZH != null) {
				Activity shopping = theCurrentMigrosZH.createActivity(ACTIVITY_TYPE_SHOP);
				String openTimeString = tokens[6];
				openHourTokens = openTimeString.split(ANYTHING_BUT_DIGITS);
				openDayTokens = openTimeString.split(ANYTHING_BUT_LETTERS);
				numbers.clear();
				// print open time strings
				for (String token : openDayTokens) {
					if (token.equals("")) {

					} else {
						System.out.print(token + "\t");
					}
				}
				System.out.println();
				for (String token : openHourTokens) {
					if (token.equals("")) {

					} else {
						System.out.print(token + "\t");
					}
				}
				System.out.println();
				System.out.flush();

				// process numbers
				int openDayTokenPointer = 1;
				double time = 0;
				double oldTime = 0;
				boolean isHour = true;
				boolean isOpen = true;
				Opentime opentime = null;
				Day[] days = Day.values();

				for (String openHourToken : openHourTokens) {
					if (!openHourToken.equals("")) {
						if (isHour) {
							time = Integer.parseInt(openHourToken) * 3600;
						} else {

							time += Integer.parseInt(openHourToken) * 60;

//							System.out.println(Time.writeTime(time));

							if (isOpen) {

								System.out.println("Open: " + Time.writeTime(time));


//								// check if we have to go to the next day
								if (time < oldTime) {

									openDayTokenPointer++;
									while (
											openDayTokens[openDayTokenPointer].equals("Ausn") || 
											openDayTokens[openDayTokenPointer].equals("")) {
										openDayTokenPointer++;
									}

								}								

							} else {

								System.out.println("Close: " + Time.writeTime(time));

								Day day = Day.getDayByGermanAbbrev(openDayTokens[openDayTokenPointer]);

								switch(day) {
								case FRIDAY:
									System.out.println("Adding times to weekdays...");
									for (int weekday = 0; weekday <= 4; weekday++) {
										opentime = new Opentime(days[weekday]
										                             .getAbbrevEnglish(), Time
										                             .writeTime(oldTime), Time
										                             .writeTime(time));
										shopping.addOpentime(opentime);
									}
									break;
								default:
									String englishDayString = day.getAbbrevEnglish();
								System.out.println("Adding times to " + englishDayString + "...");
								opentime = new Opentime(
										englishDayString, 
										Time.writeTime(oldTime), 
										Time.writeTime(time));
								shopping.addOpentime(opentime);
								break;
								}
							}

							isOpen = !isOpen;

							oldTime = time;

						}

						isHour = !isHour;


					}

				}
			}

		}


		System.out.println("Setting up Migros ZH open times...done.");

	}

	private static void processMigrosOstschweizOpenTimes(Facilities facilities) {

		System.out.println("Setting up Migros Ostschwiiz open times...");

		List<String> openTimeLines = null;
		List<String> addressLines = null;
		String[] openTimeTokens = null;
		String[] openHourTokens = null;
		String[] addressTokens = null;
		int addressLinePointer = 1;

		Day[] days = Day.values();
		boolean isHour = true;
		boolean isOpen = true;
		double time = 0;
		double openingHour = 0;
		Opentime opentime = null;

		try {

			openTimeLines = FileUtils.readLines(new File(migrosOstschweizOpenTimesFilename), "UTF-8");
			addressLines = FileUtils.readLines(new File(migrosOstschweizAdressesFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : openTimeLines) {

			openTimeTokens = line.split(FIELD_DELIM);

			// ignore empty lines
			if (openTimeTokens.length == 0) {
				continue;
			}
			// ignore header line
			if (openTimeTokens[0].equals(" Kst. Nr.  ")) {
				continue;
			}
			// ignore lines without data
			if (openTimeTokens[0].equals(" ") || openTimeTokens[0].equals("")) {
				continue;
			}
			//System.out.println(line);

			// modify facility description in order to get correct ID
			openTimeTokens[1] = openTimeTokens[1].split(" ", 3)[2];

			addressTokens = addressLines.get(addressLinePointer).split(FIELD_DELIM);

			shopId = new ShopId(
					MIGROS, 
					addressTokens[0].trim(), 
					addressTokens[1].trim(),
					MIGROS_OSTSCHWEIZ,
					addressTokens[8].trim().split(" ")[0],
					addressTokens[8].trim().split(" ")[1],
					addressTokens[7].trim()
			);

			String facilityId = shopId.getShopId();
			addressLinePointer++;

			//System.out.println(facilityId);
			Facility theCurrentMigrosOstschweiz = (Facility) facilities.getLocation(facilityId);
			if (theCurrentMigrosOstschweiz != null) {

				Activity shopping = theCurrentMigrosOstschweiz.createActivity(ACTIVITY_TYPE_SHOP);

				// extract numbers
				for (int tokenPos = 2; tokenPos < openTimeTokens.length; tokenPos++) {

					String token = openTimeTokens[tokenPos];
					int dayPointer = (tokenPos / 2) - 1;

					openHourTokens = token.split(ANYTHING_BUT_DIGITS);
					for (String openHourToken : openHourTokens) {
						if (!openHourToken.equals("")) {

							if (isHour) {
								time = Integer.parseInt(openHourToken) * 3600;
							} else {
								time += Integer.parseInt(openHourToken) * 60;

								if (isOpen) {
									openingHour = time;
								} else {

									String englishDayString = days[dayPointer].getAbbrevEnglish();
									System.out.println("Adding times to " + englishDayString + "...");
									opentime = new Opentime(
											englishDayString, 
											Time.writeTime(openingHour), 
											Time.writeTime(time));
									shopping.addOpentime(opentime);

								}

								isOpen = !isOpen;

							}

							isHour = !isHour;

						}
					}
				}

				// process numbers


			} else {
				System.out.println("Not found: " + facilityId);
			}

			System.out.flush();

		}

		System.out.println("Setting up Migros Ostschwiiz open times...done!");

	}

	private static void processCoopZHOpenTimes(Facilities facilities) {

		System.out.println("Setting up Coop ZH open times...");

		final int START_OPEN_TOKEN_INDEX = 14;
		final int END_OPEN_TOKEN_INDEX = START_OPEN_TOKEN_INDEX + (4 * 7) - 1;

		List<String> lines = null;
		String[] tokens = null;
		Day[] days = Day.values();
		boolean isOpen = true;
		String time = null;
		String openingHour = null;
		Opentime opentime = null;

		try {

			lines = FileUtils.readLines(new File(coopZHFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			tokens = line.split(FIELD_DELIM);

			// ignore header line
			if (tokens[3].equals("KST-Nr")) {
				continue;
			}

			shopId = new ShopId(COOP, tokens[7], tokens[8], COOP_ZH, tokens[43], tokens[44], tokens[42]);
			String facilityId = shopId.getShopId();
			System.out.println(facilityId);
			Facility theCurrentCoopZH = (Facility) facilities.getLocation(facilityId);
			if (theCurrentCoopZH != null) {

				Activity shopping = theCurrentCoopZH.createActivity(ACTIVITY_TYPE_SHOP);

				for (int tokenPos = START_OPEN_TOKEN_INDEX; tokenPos <= END_OPEN_TOKEN_INDEX; tokenPos++) {

					String token = tokens[tokenPos];

					if (!token.equals("")) {
						int dayIndex = (tokenPos - START_OPEN_TOKEN_INDEX) / 4;
						time = token;

						if (isOpen) {
							openingHour = time;
						} else {									
							String englishDayString = days[dayIndex].getAbbrevEnglish();
							System.out.println("Open: " + openingHour);
							System.out.println("Close: " + time);
							System.out.println("Adding times to " + englishDayString + "...");
							opentime = new Opentime(
									englishDayString, 
									openingHour, 
									time);
							shopping.addOpentime(opentime);
						}

						isOpen = !isOpen;
					}
				}

			} else {
				System.out.println("Not in the facilities file: " + facilityId);				
			}
			System.out.flush();

		}

		System.out.println("Setting up Coop ZH open times...done.");

	}

	private static void processCoopTGOpenTimes(Facilities facilities) {

		System.out.println("Setting up Coop TG open times...");

		final int START_OPEN_TOKEN_INDEX = 4;
		final int END_OPEN_TOKEN_INDEX = 7;

		List<String> lines = null;
		String[] tokens = null;
		String[] openHourTokens = null;
		Day[] days = Day.values();
		double openingHour = 0;
		double time = 0;
		Opentime opentime = null;
		boolean isHour = true;
		boolean isOpen = true;

		try {

			lines = FileUtils.readLines(new File(coopTGFilename), "UTF-8");

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			tokens = line.split(FIELD_DELIM);

			shopId = new ShopId(
					COOP, 
					"", 
					tokens[0], 
					COOP_TG, 
					tokens[2].split(" ")[0], 
					tokens[2].split(" ")[1], 
					tokens[1]);

			String facilityId = shopId.getShopId();
			System.out.println(facilityId);
			Facility theCurrentCoopTG = (Facility) facilities.getLocation(facilityId);
			if (theCurrentCoopTG != null) {
				Activity shopping = theCurrentCoopTG.createActivity(ACTIVITY_TYPE_SHOP);

				for (int tokenPos = START_OPEN_TOKEN_INDEX; tokenPos <= END_OPEN_TOKEN_INDEX; tokenPos++) {

					String token = tokens[tokenPos];

					if (!token.equals("")) {
						openHourTokens = token.split(ANYTHING_BUT_DIGITS);
						// hier gehts weita!
						for (String openHourToken : openHourTokens) {

							if (!openHourToken.equals("")) {

								if (isHour) {
									time = Integer.parseInt(openHourToken) * 3600;
								} else {
									time += Integer.parseInt(openHourToken) * 60;

									if (isOpen) {
										openingHour = time;
									} else {

										System.out.println("Open: " + Time.writeTime(openingHour));
										System.out.println("Close: " + Time.writeTime(time));

										switch(tokenPos) {
										case START_OPEN_TOKEN_INDEX:
										case START_OPEN_TOKEN_INDEX + 1:
											System.out.println("Adding times to weekdays...");
											for (int weekday = 0; weekday <= 4; weekday++) {
												opentime = new Opentime(
														days[weekday].getAbbrevEnglish(), 
														Time.writeTime(openingHour), 
														Time.writeTime(time));
												shopping.addOpentime(opentime);
											}
											break;
										case START_OPEN_TOKEN_INDEX + 2:
										case START_OPEN_TOKEN_INDEX + 3:
											System.out.println("Adding times to saturday...");
											opentime = new Opentime(
													Day.getDayByGermanAbbrev("Sa").getAbbrevEnglish(), 
													Time.writeTime(openingHour), 
													Time.writeTime(time));
											shopping.addOpentime(opentime);
											break;
										}
									}

									isOpen = !isOpen;

								}

								isHour = !isHour;

							}

						}
					}
				}
			} else {
				System.out.println("Not in the facilities file: " + facilityId);				
			}

		}
		System.out.println("Setting up Coop TG open times...done.");
	}

	private static void processDennerOpenTimes(Facilities facilities) {

		System.out.println("Setting up Denner open times...");

		List<String> lines = null;
		String[] tokens = null;
		String street = null;
		String city = null;
		String postcode = null;
		String weekDayToken = null;
		String saturdayToken = null;
//		String facilityId = null;
		String[] openHourTokens = null;

		Day[] days = Day.values();
		double openingHour = 0;
		double time = 0;
		Opentime opentime = null;
		boolean isHour = true;
		boolean isOpen = true;


		final int UNINTERESTING_LINE = -1;
		final int STREET_LINE = 0;
		final int WEEKDAY_LINE = 1;
		final int SATURDAY_LINE = 2;
		final int CITY_LINE = 3;
		int lineType = UNINTERESTING_LINE;

		try {
			lines = FileUtils.readLines(new File(dennerTGZHFilename), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String line : lines) {

			// find out what line it is
			lineType = UNINTERESTING_LINE;
			if (Pattern.matches(BEGINS_WITH_3_DIGITS, line)) {
				lineType = CITY_LINE;
			} else if (Pattern.matches(CONTAINS_PALETTE, line)) {
				lineType = STREET_LINE;
			} else if (Pattern.matches(CONTAINS_FAX, line)) {
				lineType = WEEKDAY_LINE;
			} else if (Pattern.matches(CONTAINS_SATURDAY, line)) {
				lineType = SATURDAY_LINE;
			}

			tokens = line.split(FIELD_DELIM);

			// extract interesting data by line type
			switch(lineType) {
			case CITY_LINE:
				postcode = tokens[9].split(" ")[0];
				city = tokens[9].split(" ")[1];
				break;
			case STREET_LINE:
				street = tokens[9];
				break;
			case WEEKDAY_LINE:
				weekDayToken = tokens[7];
				break;
			case SATURDAY_LINE:
				saturdayToken = tokens[7];
				break;
			case UNINTERESTING_LINE:
				continue;
			default:
				System.out.println("You should not come here...");
			break;	
			}

			// now process information
			switch(lineType) {
			case SATURDAY_LINE:

				// reset switches in case of lines such as
				// SA    09.00-17.00 / FR   -20.00
				// where process only the SA part
				isHour = true;
				isOpen = true;


				shopId = new ShopId(DENNER, "", "", "", postcode, city, street);
				System.out.println(shopId.getShopId());
				System.out.println(weekDayToken);
				System.out.println(saturdayToken);
				System.out.println();

				Facility theCurrentDenner = (Facility) facilities.getLocation(shopId.getShopId());
				if (theCurrentDenner != null) {
					Activity shopping = theCurrentDenner.createActivity(ACTIVITY_TYPE_SHOP);
					for (String openTimeString : new String[]{weekDayToken, saturdayToken}) {

						openHourTokens = openTimeString.split(ANYTHING_BUT_DIGITS);
						for (String openHourToken : openHourTokens) {
							if (!openHourToken.equals("")) {

								if (isHour) {
									time = Integer.parseInt(openHourToken) * 3600;
								} else {
									time += Integer.parseInt(openHourToken) * 60;

									if (isOpen) {
										openingHour = time;
									} else {

										System.out.println("Open: " + Time.writeTime(openingHour));
										System.out.println("Close: " + Time.writeTime(time));

										if (openTimeString.equals(weekDayToken)) {
											System.out.println("Adding times to weekdays...");
											for (int weekday = 0; weekday <= 4; weekday++) {
												opentime = new Opentime(
														days[weekday].getAbbrevEnglish(), 
														Time.writeTime(openingHour), 
														Time.writeTime(time));
												shopping.addOpentime(opentime);
											}
										} else if (openTimeString.equals(saturdayToken)) {
											System.out.println("Adding times to saturday...");
											opentime = new Opentime(
													Day.getDayByGermanAbbrev("Sa").getAbbrevEnglish(), 
													Time.writeTime(openingHour), 
													Time.writeTime(time));
											shopping.addOpentime(opentime);
										}
									}
									isOpen = !isOpen;
								}
								isHour = !isHour;
							}
						}

					}
				} else {
					System.out.println("Not in the facilities file: " + shopId.getShopId());				
				}
				break;
			default:
				continue;
			}

		}

		System.out.println("Setting up Denner open times...done.");

	}

	private static void shopsToTXT() {

		Facilities shopsOf2005 = new Facilities("shopsOf2005", Facilities.FACILITIES_NO_STREAMING);
		ArrayList<String> txtLines = new ArrayList<String>();
		ShopId shopId = null;
		String aShopLine = null;
		String facilityId = null;

		Day[] days = Day.values();
		Opentime opentime = null;

		// write header line
		aShopLine =
			"retailer" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"businessRegion" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"shopType" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"shopDescription" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"street" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"postcode" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"city" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"CH1903_X" + ShopsOf2005ToFacilities.FIELD_DELIM +
			"CH1903_Y";

		for (Day day : days) {

			aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
			aShopLine += day.getAbbrevEnglish();
			for (int ii=1; ii<=3; ii++) {
				aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
			}
		}	

		txtLines.add(aShopLine);

		System.out.println("Reading facilities xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(shopsOf2005);
		facilities_reader.readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("Reading facilities xml file...done.");

		Iterator facilityIterator = shopsOf2005.getFacilities().values().iterator();

		while (facilityIterator.hasNext()) {

			Facility facility = (Facility) facilityIterator.next();
			facilityId = facility.getId().toString();
			System.out.println(facilityId);

			try {
				shopId = new ShopId(facility.getId().toString());
			} catch (ArrayIndexOutOfBoundsException e) {
				continue;
			}

			// name, coordinates etc. (fixed length)
			aShopLine = 
				shopId.getRetailer() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getBusinessRegion() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getShopType() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getShopDescription() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getStreet() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getPostcode() + ShopsOf2005ToFacilities.FIELD_DELIM +
				shopId.getCity() + ShopsOf2005ToFacilities.FIELD_DELIM +
				(int) facility.getCenter().getY() + ShopsOf2005ToFacilities.FIELD_DELIM +
				(int) facility.getCenter().getX();
			;

			Activity shopping = facility.getActivity(ACTIVITY_TYPE_SHOP);
			if (shopping != null) {

				// open times (variable length)
				for (Day day : days) {

					TreeSet<Opentime> dailyOpentime = shopping.getOpentimes(day.getAbbrevEnglish());

					if (dailyOpentime != null) {

						// what crappy code is that...but I had to get finished :-)
						opentime = dailyOpentime.last();
						aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
						aShopLine += Time.writeTime(opentime.getStartTime());
						aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
						aShopLine += Time.writeTime(opentime.getEndTime());
						if (dailyOpentime.size() == 2) {
							opentime = dailyOpentime.first();
							aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
							aShopLine += Time.writeTime(opentime.getStartTime());
							aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
							aShopLine += Time.writeTime(opentime.getEndTime());
						} else 	if (dailyOpentime.size() == 1) {
							aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
							aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
						}

					}
				}

			}
			txtLines.add(aShopLine);

		}

		System.out.println("Writing txt file...");
		try {
			FileUtils.writeLines(
					new File(Gbl.getConfig().facilities().getOutputFile()), 
					"UTF-8", 
					txtLines
			);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Writing txt file...done.");

	}

	private static void shopsToOpentimesKML() {

		// variables used
		Day[] days = Day.values();

		String facilityId = null;
		CH1903LV03toWGS84 trafo = new CH1903LV03toWGS84();
		FolderType aShop = null;
		PlacemarkType aShopOpeningPeriod = null;
		PointType aPointType = null;
		TimeSpanType aTimeSpanType = null;
		CoordI northWestCH1903 = null;
		CoordI northWestWGS84 = null;

		// as a start, let's use the week April, 21 to April 27, 2008 as THE week
		final int MONDAY_DAY = 21;

		// we produce two kml files:
		// 1, the shops of 2005
		// 2, the shops from enterprise census
		final int SHOPS_OF_2005 = 0;
		final int SHOPS_FROM_ENTERPRISE_CENSUS = 1;
		TreeMap<Integer, String> shopsNames = new TreeMap<Integer, String>();
		shopsNames.put(new Integer(SHOPS_OF_2005), "shopsOf2005");
		shopsNames.put(new Integer(SHOPS_FROM_ENTERPRISE_CENSUS), "shopsFromEnterpriseCensus2000");

		TreeMap<Integer, String> facilitiesInputFilenames = new TreeMap<Integer, String>();
		facilitiesInputFilenames.put(SHOPS_OF_2005, "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/shopsOf2005/facilities_shopsOf2005.xml");
		facilitiesInputFilenames.put(SHOPS_FROM_ENTERPRISE_CENSUS, "/home/meisterk/workspace/MATSim/output/facilities_KTIYear2.xml.gz");

		TreeMap<Integer, String> kmlOutputFilenames = new TreeMap<Integer, String>();
		kmlOutputFilenames.put(SHOPS_OF_2005, "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/shopsOf2005/shopsOf2005.kml");
		kmlOutputFilenames.put(SHOPS_FROM_ENTERPRISE_CENSUS, "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/facilities_KTIYear2.kml");

		TreeMap<Integer, String> shopStyleNames = new TreeMap<Integer, String>();
		shopStyleNames.put(SHOPS_OF_2005, "shopsOf2005Style");
		shopStyleNames.put(SHOPS_FROM_ENTERPRISE_CENSUS, "shopsEC2000Style");

		TreeMap<Integer, Double> shopIconScales = new TreeMap<Integer, Double>();
		shopIconScales.put(SHOPS_OF_2005, 1.0);
		shopIconScales.put(SHOPS_FROM_ENTERPRISE_CENSUS, 2.0);
		
		Facilities facilities = null;
		for (int dataSetIndex : new int[]{SHOPS_OF_2005/*, SHOPS_FROM_ENTERPRISE_CENSUS*/}) {
			facilities = new Facilities(shopsNames.get(new Integer(dataSetIndex)), Facilities.FACILITIES_NO_STREAMING);

			System.out.println("Reading facilities xml file... ");
			FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilities);
			facilities_reader.readFile(facilitiesInputFilenames.get(new Integer(dataSetIndex)));
			System.out.println("Reading facilities xml file...done.");

			System.out.println("Initializing KML... ");

			// kml and document
			JAXBContext jaxbContext = null;
			try {
				jaxbContext = JAXBContext.newInstance("com.google.earth.kml._2");
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ObjectFactory factory = new ObjectFactory();

			KmlType kml = factory.createKmlType();
			DocumentType document = factory.createDocumentType();
			document.setName(shopsNames.get(new Integer(dataSetIndex)));
			kml.setAbstractFeatureGroup(factory.createDocument(document));

			// styles
			StyleType shopStyle = factory.createStyleType();
			document.getAbstractStyleSelectorGroup().add(factory.createStyle(shopStyle));
			shopStyle.setId(shopStyleNames.get(new Integer(dataSetIndex)));
			IconStyleType shopIconStyle = factory.createIconStyleType();
			shopStyle.setIconStyle(shopIconStyle);
			BasicLinkType shopIconLink = factory.createBasicLinkType();
			shopIconStyle.setIcon(shopIconLink);
			shopIconStyle.setScale(shopIconScales.get(new Integer(dataSetIndex)));
			shopIconLink.setHref("http://maps.google.com/mapfiles/kml/paddle/S.png");
			System.out.println("Initializing KML...done.");

			Iterator facilityIterator = facilities.getFacilities().values().iterator();

			while (facilityIterator.hasNext()) {
				Facility facility = (Facility) facilityIterator.next();
				facilityId = facility.getId().toString();
//				System.out.println(facility.toString());
				//System.out.println(facilityId);

				aShop = factory.createFolderType();
				document.getAbstractFeatureGroup().add(factory.createFolder(aShop));
				aShop.setName(facilityId.split("_", 2)[0]);
				aShop.setDescription(facilityId);
				
				// transform coordinates incl. toggle easting and northing
				northWestCH1903 = new Coord(facility.getCenter().getX(), facility.getCenter().getY());
				northWestWGS84 = trafo.transform(northWestCH1903);

				// have to iterate this over opening times
				int dayCounter = 0;
				for (Day day : days) {
					if (facility.getActivity(ACTIVITY_TYPE_SHOP) != null) {
						TreeSet<Opentime> dailyOpentimes = facility.getActivity(ACTIVITY_TYPE_SHOP).getOpentimes(day.getAbbrevEnglish());
						if (dailyOpentimes != null) {
							for (Opentime opentime : dailyOpentimes) {

								// build up placemark structure
								aShopOpeningPeriod = factory.createPlacemarkType();
								aShop.getAbstractFeatureGroup().add(factory.createPlacemark(aShopOpeningPeriod));
								aShopOpeningPeriod.setStyleUrl(shopStyleNames.get(new Integer(dataSetIndex)));
								aShopOpeningPeriod.setName(facilityId.split("_", 2)[0]);
								aShopOpeningPeriod.setDescription(facilityId);

								aPointType = factory.createPointType();
								aShopOpeningPeriod.setAbstractGeometryGroup(factory.createPoint(aPointType));
								aPointType.getCoordinates().add(northWestWGS84.getX() + "," + northWestWGS84.getY() + ",0.0");

								// transform opening times to GE time primitives
								aTimeSpanType = factory.createTimeSpanType();
								aShopOpeningPeriod.setAbstractTimePrimitiveGroup(factory.createAbstractTimePrimitiveGroup(aTimeSpanType));
								aTimeSpanType.setBegin("2008-04-" + Integer.toString(MONDAY_DAY + dayCounter) + "T" + Time.writeTime(opentime.getStartTime()) + "+01:00");
								aTimeSpanType.setEnd("2008-04-" + Integer.toString(MONDAY_DAY + dayCounter) + "T" + Time.writeTime(opentime.getEndTime()) + "+01:00");
							}
						}
					}
					dayCounter++;
				}

			}

			System.out.println("Writing out KML...");
			try {
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				marshaller.marshal(factory.createKml(kml), new FileOutputStream(kmlOutputFilenames.get(new Integer(dataSetIndex))));
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Writing out KML...done.");
		}
	}

	private static void applyOpentimesToEnterpriseCensus() {

		Facilities facilities_KTIYear1 = new Facilities("Switzerland based on Enterprise census 2000.", Facilities.FACILITIES_USE_STREAMING);

		Facilities facilities_Dummy_KTIYear2 = new Facilities("Facilities KTI Year 2", Facilities.FACILITIES_NO_STREAMING);

		// init algorithms
		FacilitiesOpentimesKTIYear2 facilitiesOpentimesKTIYear2 = new FacilitiesOpentimesKTIYear2();
		facilitiesOpentimesKTIYear2.init();

		FacilitiesWriterAlgorithm writerAlgo = new FacilitiesWriterAlgorithm(facilities_Dummy_KTIYear2);

		// let algorithms work for "shop" facilities only
		FacilitiesActTypeFilter shopFilter = null;
		for (FacilitiesAlgorithm facilitiesAlgorithm : new FacilitiesAlgorithm[]{facilitiesOpentimesKTIYear2, writerAlgo}) {
			shopFilter = new FacilitiesActTypeFilter((FacilityAlgorithmI) facilitiesAlgorithm);
			shopFilter.addActTypePattern("shop");
			facilities_KTIYear1.addAlgorithm((FacilitiesAlgorithm) shopFilter);
		}

		System.out.println("Streaming Facilities KTI Year 2 file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilities_KTIYear1);
		facilities_reader.readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("Streaming Facilities KTI Year 2 file...done.");

		writerAlgo.finish();

	}

}
