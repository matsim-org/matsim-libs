/* *********************************************************************** *
 * project: org.matsim.*
 * KMLDemo.java
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

package org.matsim.utils.vis.kml;

import java.util.GregorianCalendar;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.shared.CoordWGS84;
import org.matsim.utils.vis.kml.ColorStyle.ColorMode;
import org.matsim.utils.vis.kml.fields.Color;

public class KMLDemo {

	// config parameters
	public static final String KML21_MODULE = "kml21";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_OUTPUT_KML_DEMO_MAIN_FILE = "outputKMLDemoMainFile";
	public static final String CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE = "outputKMLDemoColoredLinkFile";
	public static final String CONFIG_USE_COMPRESSION = "useCompression";

	public static final String SEP = System.getProperty("file.separator");

	private static String mainKMLFilename;
	private static String coloredLinkKMLFilename;
	private static boolean useCompression = false;

	private static KML myKML, coloredLinkKML;
	private static Document myKMLDocument, coloredLinkKMLDocument;

	private static Style work10Style, leisureFacilityStyle, blueLineStyle;

	private static Config config = null;
	
	public static void main(final String[] args) {

		config = Gbl.createConfig(args);

		System.out.println("  starting KML demo...");

		setUp();
		generateStyles();
		generateData();
		write();

		System.out.println("  done.");
	}

	public static void setUp() {

		System.out.println("    Set up...");

		mainKMLFilename =
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_MAIN_FILE);
		coloredLinkKMLFilename =
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE);

		if (config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("true")) {
			useCompression = true;
		} else if(config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("false")) {
			useCompression = false;
		} else {
			Gbl.errorMsg(
					"Invalid value for config parameter " + CONFIG_USE_COMPRESSION +
					" in module " + KML21_MODULE +
					": \"" + config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION) + "\"");
		}

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);

		coloredLinkKML = new KML();
		coloredLinkKMLDocument = new Document("network main feature");
		coloredLinkKML.setFeature(coloredLinkKMLDocument);

		System.out.println("    done.");

	}

	public static void generateStyles() {

		System.out.println("    generating styles...");

		work10Style = new Style("work10Style");
		myKMLDocument.addStyle(work10Style);
		leisureFacilityStyle = new Style("leisureFacilityStyle");
		myKMLDocument.addStyle(leisureFacilityStyle);
		work10Style.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
		leisureFacilityStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/pal2/icon57.png"),
						new Color("ff","00","00","ff"),
						ColorStyle.DEFAULT_COLOR_MODE,
						Math.sqrt(4.0)));

		blueLineStyle = new Style("blueLineStyle");
		myKMLDocument.addStyle(blueLineStyle);
		blueLineStyle.setLineStyle(new LineStyle(new Color("7f","ff","00","00"), ColorStyle.DEFAULT_COLOR_MODE, 5));

		int intervals = 24*4;
		int alpha = 255;

		for (int ii = 0; ii < intervals; ii++) {

			int r = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) + 1));
			//System.out.println(r);
			int g = (int)(127.0 * (Math.cos((ii * 2 * Math.PI) / intervals) + 1));
			//System.out.println(g);
			int b = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) * (-1) + 1));
			//System.out.println(b);

			String aStr = Integer.toHexString(alpha);
			if (aStr.length() == 1) {
				aStr = "0".concat(aStr);
			}
			String rStr = Integer.toHexString(r);
			if (rStr.length() == 1) {
				rStr = "0".concat(rStr);
			}
			String gStr = Integer.toHexString(g);
			if (gStr.length() == 1) {
				gStr = "0".concat(gStr);
			}
			String bStr = Integer.toHexString(b);
			if (bStr.length() == 1) {
				bStr = "0".concat(bStr);
			}

			Color color = new Color(aStr, bStr, gStr, rStr);

			Style s = new Style("networkStyle" + ii);
			coloredLinkKMLDocument.addStyle(s);
			s.setLineStyle(new LineStyle(color, ColorMode.NORMAL, 5));

		}

		System.out.println("    done.");
	}

	public static void generateData() {

		System.out.println("    generating data...");

		// generate facilities in a folder
		Folder facilitiesFolder = new Folder(
				"facilities",
				"facilities",
				"Contains all the facilities.",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(facilitiesFolder);

		Placemark aFacility = new Placemark(
				"hardturm",
				"Hardturmstadion Zürich",
				"Das ist das Hardturmstadion.",
				"Hardturmstrasse, Zürich",
				Feature.DEFAULT_LOOK_AT,
				leisureFacilityStyle.getStyleUrl(),
				true,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		facilitiesFolder.addFeature(aFacility);

		CoordWGS84 hardturmCoord = CoordWGS84.createFromWGS84(8, 30, 17, 47, 23, 35);
		Point hardturmPoint = new Point(hardturmCoord.getLongitude(), hardturmCoord.getLatitude(), 0.0);
		aFacility.setGeometry(hardturmPoint);
		aFacility.setLookAt(new LookAt(hardturmCoord.getLongitude(), hardturmCoord.getLatitude()));

		Placemark bFacility = new Placemark(
				"unterer Letten",
				"Badi Unterer Letten",
				"Das ist eine Flussbadeanstalt.",
				"Wasserwerkstrasse 131, Zürich",
				Feature.DEFAULT_LOOK_AT,
				leisureFacilityStyle.getStyleUrl(),
				true,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		facilitiesFolder.addFeature(bFacility);
		CoordWGS84 lettenCoord = CoordWGS84.createFromWGS84(8, 31, 46, 47, 23, 20);
		Point lettenPoint = new Point(lettenCoord.getLongitude(), lettenCoord.getLatitude(), 0.0);
		bFacility.setGeometry(lettenPoint);
		bFacility.setLookAt(new LookAt(lettenCoord.getLongitude(), lettenCoord.getLatitude(), 10000.0, 45.0, 180.0));

		Placemark ethHoenggerberg = new Placemark(
				"eth",
				"ETH Hönggerberg",
				"The ETH Zurich outpost.",
				"8093 Zurich",
				Feature.DEFAULT_LOOK_AT,
				work10Style.getStyleUrl(),
				true,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		facilitiesFolder.addFeature(ethHoenggerberg);

		CoordWGS84 ethCoord = CoordWGS84.createFromWGS84(8, 30, 33, 47, 24, 27);
		Point ethPoint = new Point(ethCoord.getLongitude(), ethCoord.getLatitude(), 0.0);
		ethHoenggerberg.setGeometry(ethPoint);

		facilitiesFolder.addFeature(new Placemark("empty placemark"));

		Folder agentsFolder = new Folder(
				"agents",
				"agents",
				"Contains all the agents",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				true,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(agentsFolder);

		Folder networkLinksFolder = new Folder("networkLinks");
		myKMLDocument.addFeature(networkLinksFolder);

		Placemark aLineString = new Placemark(
				"8link",
				"the 8:00 link",
				"A line that exists only at 8:00 AM.",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				blueLineStyle.getStyleUrl(),
				true,
				Feature.DEFAULT_REGION,
				new TimeStamp(new GregorianCalendar(1970, 0, 1, 8, 0, 0)));
		aLineString.setGeometry(new LineString(lettenPoint, hardturmPoint));
		networkLinksFolder.addFeature(aLineString);

		Region linkRegion = new Region(
				Math.max(ethPoint.getLatitude(), hardturmPoint.getLatitude()),
				Math.min(ethPoint.getLatitude(), hardturmPoint.getLatitude()),
				Math.min(ethPoint.getLongitude(), hardturmPoint.getLongitude()),
				Math.max(ethPoint.getLongitude(), hardturmPoint.getLongitude()),
				256,
				Region.DEFAULT_MAX_LOD_PIXELS
		);

		for (int ii = 0; ii < 24*4; ii++) {

			GregorianCalendar gcBegin = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15, 0);
			GregorianCalendar gcEnd = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15 + 14, 59);

			String styleId = "networkStyle" + ii;

			Placemark aTemporaryLineString = new Placemark(
					Integer.toString(ii),
					Integer.toString(ii),
					"Link state in interval #" + Integer.toString(ii),
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					coloredLinkKMLDocument.getStyle(styleId).getStyleUrl(),
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					new TimeSpan(gcBegin, gcEnd));
			aTemporaryLineString.setGeometry(new LineString(ethPoint, hardturmPoint, Geometry.DEFAULT_EXTRUDE, true, Geometry.DEFAULT_ALTITUDE_MODE));

			coloredLinkKMLDocument.addFeature(aTemporaryLineString);

		}

		NetworkLink nl = new NetworkLink(
				"network link to the link",
				new Link(coloredLinkKMLFilename),
				"network link to the link",
				"network link to the link",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				linkRegion,
				Feature.DEFAULT_TIME_PRIMITIVE
		);

		networkLinksFolder.addFeature(nl);

		System.out.println("    done.");

	}

	public static void write() {

		System.out.println("    writing KML files out...");

		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(myKML, mainKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
		myKMLDocumentWriter.write();
		myKMLDocumentWriter = new KMLWriter(coloredLinkKML, coloredLinkKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
		myKMLDocumentWriter.write();

		System.out.println("    done.");

	}

}
