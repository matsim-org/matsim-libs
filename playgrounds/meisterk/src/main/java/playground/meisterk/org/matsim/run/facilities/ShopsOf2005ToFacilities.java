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

package playground.meisterk.org.matsim.run.facilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.AbstractGeometryType;
import net.opengis.kml._2.BasicLinkType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.TimeSpanType;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.KMZWriter;

import playground.meisterk.org.matsim.facilities.ShopId;
import playground.meisterk.org.matsim.facilities.algorithms.FacilitiesOpentimesKTIYear2;


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
		MONDAY (DayType.mon, "Mo"),
		TUESDAY (DayType.tue, "Di"),
		WEDNESDAY (DayType.wed, "Mi"),
		THURSDAY (DayType.thu, "Do"),
		FRIDAY (DayType.fri, "Fr"),
		SATURDAY (DayType.sat, "Sa"),
		SUNDAY (DayType.sun, "So");

		private final DayType abbrevEnglish;
		private final String abbrevGerman;

		Day(final DayType abbrevEnglish, final String abbrevGerman) {
			this.abbrevEnglish = abbrevEnglish;
			this.abbrevGerman = abbrevGerman;
		}

		public String getAbbrevGerman() {
			return this.abbrevGerman;
		}

		public DayType getAbbrevEnglish() {
			return this.abbrevEnglish;
		}

		public static Day getDayByGermanAbbrev(final String germanAbbrev) {

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

	private static JAXBContext jaxbContext = null;
	private static ObjectFactory kmlObjectFactory = new ObjectFactory();
	private static KmlType myKML = null;
	private static DocumentType myKMLDocument = null;
	private static FolderType mainKMLFolder = null;

	private static String kmlFilename = "output" + FILE_SEPARATOR + "shopsOf2005.kml";

	private static HashMap<String, StyleType> styles = new HashMap<String, StyleType>();
	private static StyleType coopStyle = null;
	private static StyleType pickpayStyle = null;
	private static StyleType migrosStyle = null;
	private static StyleType dennerStyle = null;

	private static HashMap<String, String> icons = null;

	private static ShopId shopId = null;


	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		Config config = ConfigUtils.loadConfig(args[0]);

		ShopsOf2005ToFacilities.prepareRawDataForGeocoding();
//		ShopsOf2005ToFacilities.transformGeocodedKMLToFacilities(config);
//		ShopsOf2005ToFacilities.shopsToTXT(config);
//		ShopsOf2005ToFacilities.shopsToOpentimesKML();
//		ShopsOf2005ToFacilities.applyOpentimesToEnterpriseCensus(config);

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

	private static void transformGeocodedKMLToFacilities(Config config) {

		ActivityFacilitiesImpl shopsOf2005 = new ActivityFacilitiesImpl("shopsOf2005");

		JAXBElement<KmlType> kmlElement = null;

		try {

			JAXBContext jaxbContext = JAXBContext.newInstance("net.opengis.kml._2");
			Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
			kmlElement = (JAXBElement<KmlType>) unMarshaller.unmarshal(new FileInputStream(config.facilities().getInputFile()));
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
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
		facilities_writer.write(null /* filename not specified */);
		System.out.println("Writing facilities xml file...done.");

	}

	private static void setUp() {

		try {
			jaxbContext = JAXBContext.newInstance("net.opengis.kml._2");
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		myKML = kmlObjectFactory.createKmlType();
		myKMLDocument = kmlObjectFactory.createDocumentType();
		myKMLDocument.setName("the root document");
		myKML.setAbstractFeatureGroup(kmlObjectFactory.createDocument(myKMLDocument));

		mainKMLFolder = kmlObjectFactory.createFolderType();
		mainKMLFolder.setName("Shops of 2005");
		mainKMLFolder.setDescription("All revealed shops of 2005.");
		myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mainKMLFolder));

	}

	private static void setupStyles() {

		System.out.println("Setting up KML styles...");

		icons = new HashMap<String, String>();
		icons.put("coop", "icons/shopsOf2005/C.png");
		icons.put("pickpay", "icons/shopsOf2005/P.png");
		icons.put("migros", "icons/shopsOf2005/M.png");
		icons.put("denner", "icons/shopsOf2005/D.png");

		for (String retailer : icons.keySet()) {

			StyleType style = kmlObjectFactory.createStyleType();
			style.setId(retailer + "Style");
			myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(coopStyle));
			BasicLinkType basicLink = kmlObjectFactory.createBasicLinkType();
			basicLink.setHref(icons.get(retailer));
			IconStyleType icon = kmlObjectFactory.createIconStyleType();
			icon.setIcon(basicLink);
			style.setIconStyle(icon);

			styles.put(retailer, style);
			myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(style));

		}

		System.out.println("Setting up KML styles...done.");

	}

	private static void dennerTGZHAddressesToKML() {

		System.out.println("Setting up Denner shops...");

		FolderType aFolder = kmlObjectFactory.createFolderType();
		aFolder.setName("Denner TG ZH");
		aFolder.setDescription("Alle Denner TG ZH Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(aFolder));

		List<String> lines = null;
		String[] tokens = null;

		try {
			lines = FileUtils.readLines(new File(dennerTGZHFilename), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

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

				PlacemarkType aShop = kmlObjectFactory.createPlacemarkType();
				aShop.setName(shopId.getShopId());
				aShop.setDescription(shopId.getShopId());
				aShop.setAddress(shopId.getAddressForGeocoding());
				aShop.setStyleUrl(styles.get("denner").getId());
				aFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aShop));

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

		FolderType coopFolder = kmlObjectFactory.createFolderType();
		coopFolder.setName("Coop ZH");
		coopFolder.setDescription("Alle Coop ZH Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(coopFolder));

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

				PlacemarkType aCoop = kmlObjectFactory.createPlacemarkType();
				aCoop.setName(shopId.getShopId());
				aCoop.setDescription(shopId.getShopId());
				aCoop.setAddress(shopId.getAddressForGeocoding());
				aCoop.setStyleUrl(styles.get("coop").getId());
				coopFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aCoop));

			}

		}

		System.out.println("Setting up Coop Züri shops...done.");

	}

	private static void coopTGAddressesToKML() {

		System.out.println("Setting up Coop Thurgau shops...");

		FolderType coopFolder = kmlObjectFactory.createFolderType();
		coopFolder.setName("Coop TG");
		coopFolder.setDescription("Alle Coop TG Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(coopFolder));

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

			PlacemarkType aCoop = kmlObjectFactory.createPlacemarkType();
			aCoop.setName(shopId.getShopId());
			aCoop.setDescription(shopId.getShopId());
			aCoop.setAddress(shopId.getAddressForGeocoding());
			aCoop.setStyleUrl(styles.get("coop").getId());
			coopFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aCoop));

		}

		System.out.println("Setting up Coop Thurgau shops...done.");

	}


	private static void pickPayAddressesToKML() {

		System.out.println("Setting up Pick Pay shops...");

		FolderType pickpayFolder = kmlObjectFactory.createFolderType();
		pickpayFolder.setName("Pickpay");
		pickpayFolder.setDescription("Alle Pickpay Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(pickpayFolder));

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

			PlacemarkType aPickpay = kmlObjectFactory.createPlacemarkType();
			aPickpay.setName(shopId.getShopId());
			aPickpay.setDescription(shopId.getShopId());
			aPickpay.setAddress(shopId.getAddressForGeocoding());
			aPickpay.setStyleUrl(styles.get("pickpay").getId());
			pickpayFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aPickpay));

		}

		System.out.println("Setting up Pick Pay shops...done.");

	}

	private static void migrosZHAdressesToKML() {

		System.out.println("Setting up Migros ZH shops...");

		FolderType migrosFolder = kmlObjectFactory.createFolderType();
		migrosFolder.setName("Migros ZH");
		migrosFolder.setDescription("Alle Migros ZH Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(migrosFolder));

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

			PlacemarkType aMigros = kmlObjectFactory.createPlacemarkType();
			aMigros.setName(shopId.getShopId());
			aMigros.setDescription(shopId.getShopId());
			aMigros.setAddress(shopId.getAddressForGeocoding());
			aMigros.setStyleUrl(styles.get("migros").getId());
			migrosFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aMigros));

		}

		System.out.println("Setting up Migros ZH shops...");

	}

	private static void migrosOstschweizAdressesToKML() {

		System.out.println("Setting up Migros Ostschweiz shops...");

		FolderType migrosFolder = kmlObjectFactory.createFolderType();
		migrosFolder.setName("Migros Ostschweiz");
		migrosFolder.setDescription("Alle Migros Ostschweiz Läden");
		mainKMLFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(migrosFolder));

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

			PlacemarkType aMigros = kmlObjectFactory.createPlacemarkType();
			aMigros.setName(shopId.getShopId());
			aMigros.setDescription(shopId.getShopId());
			aMigros.setAddress(shopId.getAddressForGeocoding());
			aMigros.setStyleUrl(styles.get("migros").getId());
			migrosFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aMigros));

		}

		System.out.println("Setting up Migros Ostschweiz shops...");

	}

	private static void write() {

		System.out.println("Writing KML files out...");

		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(kmlObjectFactory.createKml(myKML), new FileOutputStream(kmlFilename));
		} catch (PropertyException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		KMZWriter writer;
		writer = new KMZWriter(kmlFilename);

		for (String icon : icons.values()) {
			try {
				writer.addNonKMLFile(MatsimResource.getAsInputStream(icon), icon);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		writer.writeMainKml(myKML);
		writer.close();

		System.out.println("done.");

	}

	private static void extractPlacemarks(final FolderType folderType, final ActivityFacilitiesImpl facilities) {

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
					Coord wgs84Coords = new CoordImpl(coordinates[0], coordinates[1]);
					WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
					Coord ch1903Coordinates = trafo.transform(wgs84Coords);

					// round coordinates to meters
					ch1903Coordinates.setXY((int) ch1903Coordinates.getX(), (int) ch1903Coordinates.getY());

//					// create facility
					if (name.equals("migrosZH_Glarus")) {
						System.out.println("There it is: " + name);
						System.out.flush();
					}
					/*ActivityFacility newFacility =*/ facilities.createFacility(new IdImpl(name), ch1903Coordinates);
				}
			}

		}

	}

	private static void addOpentimesToFacilities(final ActivityFacilitiesImpl facilities) {

		ShopsOf2005ToFacilities.processPickPayOpenTimes(facilities);
		ShopsOf2005ToFacilities.processMigrosZHOpenTimes(facilities);
		ShopsOf2005ToFacilities.processMigrosOstschweizOpenTimes(facilities);
		ShopsOf2005ToFacilities.processCoopZHOpenTimes(facilities);
		ShopsOf2005ToFacilities.processCoopTGOpenTimes(facilities);
		ShopsOf2005ToFacilities.processDennerOpenTimes(facilities);

	}

	private static void processPickPayOpenTimes(final ActivityFacilitiesImpl facilities) {

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
			ActivityFacilityImpl theCurrentPickpay = (ActivityFacilityImpl) facilities.getFacilities().get(new IdImpl(facilityId));
			if (theCurrentPickpay != null) {

				// yeah, we can use the open times

				ActivityOptionImpl shopping = theCurrentPickpay.createActivityOption(ACTIVITY_TYPE_SHOP);
				openNumbers.clear();
				closeNumbers.clear();

				// print out and extract numbers

				//System.out.print(OPEN + ":\t");
				for (String token : openTokens) {
					if (!token.equals("") && !token.equals(openTokens[0])) {
						openNumbers.add(Integer.valueOf(token));
						//System.out.print(token + "\t");
					}
				}
				//System.out.println();

				closeTokens = aPickpayOpentime.get(openLine).split(ANYTHING_BUT_DIGITS);
				//System.out.print(CLOSE + ":\t");
				for (String token : closeTokens) {
					if (!token.equals("")) {
						closeNumbers.add(Integer.valueOf(token));
						//System.out.print(token + "\t");
					}
				}
				//System.out.println();

				// now process numbers

				//String day = "wkday";
				Day[] days = Day.values();
				int dayPointer = 0;
				OpeningTimeImpl opentime = null;
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

						opentime = new OpeningTimeImpl(days[dayPointer].getAbbrevEnglish(), openSeconds, closeSeconds);

						shopping.addOpeningTime(opentime);

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

	private static void processMigrosZHOpenTimes(final ActivityFacilitiesImpl facilities) {

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

			ActivityFacility theCurrentMigrosZH = facilities.getFacilities().get(new IdImpl(facilityId));
			if (theCurrentMigrosZH != null) {
				ActivityOptionImpl shopping = ((ActivityFacilityImpl) theCurrentMigrosZH).createActivityOption(ACTIVITY_TYPE_SHOP);
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
				OpeningTimeImpl opentime = null;
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
										opentime = new OpeningTimeImpl(days[weekday].getAbbrevEnglish(), oldTime, time);
										shopping.addOpeningTime(opentime);
									}
									break;
								default:
									DayType englishDayString = day.getAbbrevEnglish();
								System.out.println("Adding times to " + englishDayString + "...");
								opentime = new OpeningTimeImpl(
										englishDayString,
										oldTime,
										time);
								shopping.addOpeningTime(opentime);
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

	private static void processMigrosOstschweizOpenTimes(final ActivityFacilitiesImpl facilities) {

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
		OpeningTimeImpl opentime = null;

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
			ActivityFacilityImpl theCurrentMigrosOstschweiz = (ActivityFacilityImpl) facilities.getFacilities().get(new IdImpl(facilityId));
			if (theCurrentMigrosOstschweiz != null) {

				ActivityOptionImpl shopping = theCurrentMigrosOstschweiz.createActivityOption(ACTIVITY_TYPE_SHOP);

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

									DayType englishDayString = days[dayPointer].getAbbrevEnglish();
									System.out.println("Adding times to " + englishDayString + "...");
									opentime = new OpeningTimeImpl(
											englishDayString,
											openingHour,
											time);
									shopping.addOpeningTime(opentime);

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

	private static void processCoopZHOpenTimes(final ActivityFacilitiesImpl facilities) {

		System.out.println("Setting up Coop ZH open times...");

		final int START_OPEN_TOKEN_INDEX = 14;
		final int END_OPEN_TOKEN_INDEX = START_OPEN_TOKEN_INDEX + (4 * 7) - 1;

		List<String> lines = null;
		String[] tokens = null;
		Day[] days = Day.values();
		boolean isOpen = true;
		String time = null;
		String openingHour = null;
		OpeningTimeImpl opentime = null;

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
			ActivityFacility theCurrentCoopZH = facilities.getFacilities().get(new IdImpl(facilityId));
			if (theCurrentCoopZH != null) {

				ActivityOptionImpl shopping = ((ActivityFacilityImpl) theCurrentCoopZH).createActivityOption(ACTIVITY_TYPE_SHOP);

				for (int tokenPos = START_OPEN_TOKEN_INDEX; tokenPos <= END_OPEN_TOKEN_INDEX; tokenPos++) {

					String token = tokens[tokenPos];

					if (!token.equals("")) {
						int dayIndex = (tokenPos - START_OPEN_TOKEN_INDEX) / 4;
						time = token;

						if (isOpen) {
							openingHour = time;
						} else {
							DayType englishDayString = days[dayIndex].getAbbrevEnglish();
							System.out.println("Open: " + openingHour);
							System.out.println("Close: " + time);
							System.out.println("Adding times to " + englishDayString + "...");
							opentime = new OpeningTimeImpl(
									englishDayString,
									Time.parseTime(openingHour),
									Time.parseTime(time));
							shopping.addOpeningTime(opentime);
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

	private static void processCoopTGOpenTimes(final ActivityFacilitiesImpl facilities) {

		System.out.println("Setting up Coop TG open times...");

		final int START_OPEN_TOKEN_INDEX = 4;
		final int END_OPEN_TOKEN_INDEX = 7;

		List<String> lines = null;
		String[] tokens = null;
		String[] openHourTokens = null;
		Day[] days = Day.values();
		double openingHour = 0;
		double time = 0;
		OpeningTimeImpl opentime = null;
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
			ActivityFacilityImpl theCurrentCoopTG = (ActivityFacilityImpl) facilities.getFacilities().get(new IdImpl(facilityId));
			if (theCurrentCoopTG != null) {
				ActivityOptionImpl shopping = theCurrentCoopTG.createActivityOption(ACTIVITY_TYPE_SHOP);

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
												opentime = new OpeningTimeImpl(
														days[weekday].getAbbrevEnglish(),
														openingHour,
														time);
												shopping.addOpeningTime(opentime);
											}
											break;
										case START_OPEN_TOKEN_INDEX + 2:
										case START_OPEN_TOKEN_INDEX + 3:
											System.out.println("Adding times to saturday...");
											opentime = new OpeningTimeImpl(
													Day.getDayByGermanAbbrev("Sa").getAbbrevEnglish(),
													openingHour,
													time);
											shopping.addOpeningTime(opentime);
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

	private static void processDennerOpenTimes(final ActivityFacilitiesImpl facilities) {

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
		OpeningTimeImpl opentime = null;
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

				ActivityFacility theCurrentDenner = facilities.getFacilities().get(new IdImpl(shopId.getShopId()));
				if (theCurrentDenner != null) {
					ActivityOptionImpl shopping = ((ActivityFacilityImpl) theCurrentDenner).createActivityOption(ACTIVITY_TYPE_SHOP);
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
												opentime = new OpeningTimeImpl(
														days[weekday].getAbbrevEnglish(),
														openingHour,
														time);
												shopping.addOpeningTime(opentime);
											}
										} else if (openTimeString.equals(saturdayToken)) {
											System.out.println("Adding times to saturday...");
											opentime = new OpeningTimeImpl(
													Day.getDayByGermanAbbrev("Sa").getAbbrevEnglish(),
													openingHour,
													time);
											shopping.addOpeningTime(opentime);
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

	private static void shopsToTXT(Config config) {

		ScenarioImpl scenario = new ScenarioImpl();
		ActivityFacilitiesImpl shopsOf2005 = scenario.getActivityFacilities();
		shopsOf2005.setName("shopsOf2005");
		ArrayList<String> txtLines = new ArrayList<String>();
		ShopId shopId = null;
		String aShopLine = null;
		String facilityId = null;

		Day[] days = Day.values();
		OpeningTimeImpl opentime = null;

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
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		facilities_reader.readFile(config.facilities().getInputFile());
		System.out.println("Reading facilities xml file...done.");

		Iterator<ActivityFacility> facilityIterator = shopsOf2005.getFacilities().values().iterator();

		while (facilityIterator.hasNext()) {

			ActivityFacilityImpl facility = (ActivityFacilityImpl) facilityIterator.next();
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
				(int) facility.getCoord().getY() + ShopsOf2005ToFacilities.FIELD_DELIM +
				(int) facility.getCoord().getX();
			;

			ActivityOptionImpl shopping = (ActivityOptionImpl) facility.getActivityOptions().get(ACTIVITY_TYPE_SHOP);
			if (shopping != null) {

				// open times (variable length)
				for (Day day : days) {

					Set<OpeningTime> dailyOpentime = shopping.getOpeningTimes(day.getAbbrevEnglish());

					if (dailyOpentime != null) {

						// what crappy code is that...but I had to get finished :-)
						opentime = (OpeningTimeImpl) ((TreeSet)dailyOpentime).last();
						aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
						aShopLine += Time.writeTime(opentime.getStartTime());
						aShopLine += ShopsOf2005ToFacilities.FIELD_DELIM;
						aShopLine += Time.writeTime(opentime.getEndTime());
						if (dailyOpentime.size() == 2) {
							opentime = (OpeningTimeImpl) ((TreeSet)dailyOpentime).first();
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
					new File((String) null /* filename not specified */),
					"UTF-8",
					txtLines
			);
		} catch (IOException e) {
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
		Coord northWestCH1903 = null;
		Coord northWestWGS84 = null;

		// as a start, let's use the week April, 21 to April 27, 2008 as THE week
		final int MONDAY_DAY = 21;

		// we produce two kml files:
		// 1, the shops of 2005
		// 2, the shops from enterprise census
		final Integer SHOPS_OF_2005 = Integer.valueOf(0);
		final Integer SHOPS_FROM_ENTERPRISE_CENSUS = Integer.valueOf(1);
		TreeMap<Integer, String> shopsNames = new TreeMap<Integer, String>();
		shopsNames.put(SHOPS_OF_2005, "shopsOf2005");
		shopsNames.put(SHOPS_FROM_ENTERPRISE_CENSUS, "shopsFromEnterpriseCensus2000");

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

		ActivityFacilitiesImpl facilities = null;
		for (int dataSetIndex : new int[]{SHOPS_OF_2005/*, SHOPS_FROM_ENTERPRISE_CENSUS*/}) {
			ScenarioImpl scenario = new ScenarioImpl();
			facilities = scenario.getActivityFacilities();
			facilities.setName(shopsNames.get(Integer.valueOf(dataSetIndex)));

			System.out.println("Reading facilities xml file... ");
			FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
			facilities_reader.readFile(facilitiesInputFilenames.get(new Integer(dataSetIndex)));
			System.out.println("Reading facilities xml file...done.");

			System.out.println("Initializing KML... ");

			// kml and document
			JAXBContext jaxbContext = null;
			try {
				jaxbContext = JAXBContext.newInstance("com.google.earth.kml._2");
			} catch (JAXBException e) {
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

			Iterator<ActivityFacility> facilityIterator = facilities.getFacilities().values().iterator();

			while (facilityIterator.hasNext()) {
				ActivityFacilityImpl facility = (ActivityFacilityImpl) facilityIterator.next();
				facilityId = facility.getId().toString();
//				System.out.println(facility.toString());
				//System.out.println(facilityId);

				aShop = factory.createFolderType();
				document.getAbstractFeatureGroup().add(factory.createFolder(aShop));
				aShop.setName(facilityId.split("_", 2)[0]);
				aShop.setDescription(facilityId);

				// transform coordinates incl. toggle easting and northing
				northWestCH1903 = new CoordImpl(facility.getCoord().getX(), facility.getCoord().getY());
				northWestWGS84 = trafo.transform(northWestCH1903);

				// have to iterate this over opening times
				int dayCounter = 0;
				for (Day day : days) {
					if (facility.getActivityOptions().get(ACTIVITY_TYPE_SHOP) != null) {
						Set<OpeningTime> dailyOpentimes = facility.getActivityOptions().get(ACTIVITY_TYPE_SHOP).getOpeningTimes(day.getAbbrevEnglish());
						if (dailyOpentimes != null) {
							for (OpeningTime opentime : dailyOpentimes) {

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

	private static void applyOpentimesToEnterpriseCensus(Config config) {

		ScenarioImpl scenario = new ScenarioImpl(config);
		ActivityFacilitiesImpl facilities_input = scenario.getActivityFacilities();
		facilities_input.setName("Switzerland based on Enterprise census 2000.");

		// init algorithms
		FacilitiesOpentimesKTIYear2 facilitiesOpentimesKTIYear2 = new FacilitiesOpentimesKTIYear2();
		facilitiesOpentimesKTIYear2.init();

		System.out.println("Reading Facilities KTI Year 2 file...");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		facilities_reader.setValidating(false);
		facilities_reader.readFile(config.facilities().getInputFile());
		System.out.println("Processing Facilities KTI Year 2 file...");
		facilitiesOpentimesKTIYear2.run(facilities_input);

		System.out.println("Writing Facilities KTI Year 2 file...");
		new FacilitiesWriter(facilities_input).write(null /* filename not specified */);

	}

}
