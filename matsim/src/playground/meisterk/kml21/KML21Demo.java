/* *********************************************************************** *
 * project: org.matsim.*
 * KML21Demo.java
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

package playground.meisterk.kml21;

import java.io.FileNotFoundException;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.shared.CoordWGS84;

import playground.meisterk.kml21.util.Util;

public class KML21Demo {

	// config parameters
	public static final String KML21_MODULE = "kml21";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_OUTPUT_KML_DEMO_MAIN_FILE = "outputKMLDemoMainFile";
	public static final String CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE = "outputKMLDemoColoredLinkFile";
	public static final String CONFIG_USE_COMPRESSION = "useCompression";

	public static final String SEP = System.getProperty("file.separator");
	
	public static String mainKMLFilename;
	public static String coloredLinkKMLFilename;
	public static boolean useCompression;
	
	public static JAXBContext jaxbContext;
	public static ObjectFactory factory;
	
	public static KmlType mainKML, coloredLinkKML;
	public static DocumentType mainKMLDocument, coloredLinkKMLDocument;
	public static StyleType workFacilityStyle, leisureFacilityStyle, blueLineStyle;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);
		
		System.out.println("  starting KML demo...");

		try {
			setUp();
			generateStyles();
			generateData();
			write();
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		System.out.println("  done.");
}

	private static void setUp() throws JAXBException {
		
		System.out.println("    Set up...");
		
		mainKMLFilename = 
			Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_MAIN_FILE);
		coloredLinkKMLFilename =
			Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			Gbl.getConfig().getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE);

		if (Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("true")) {
			useCompression = Boolean.TRUE;
		} else if(Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("false")) {
			useCompression = Boolean.FALSE;
		} else {
			Gbl.errorMsg(
					"Invalid value for config parameter " + CONFIG_USE_COMPRESSION + 
					" in module " + KML21_MODULE + 
					": \"" + Gbl.getConfig().getParam(KML21_MODULE, CONFIG_USE_COMPRESSION) + "\"");
		}
		
		jaxbContext = JAXBContext.newInstance("org.matsim.playground.meisterk.kml21");
		factory = new ObjectFactory();
		
		mainKML = factory.createKmlType();
		mainKMLDocument = factory.createDocumentType();
		mainKMLDocument.setName("root feature of main KML");
		mainKML.setFeature(factory.createDocument(mainKMLDocument));
		
		coloredLinkKML = factory.createKmlType();
		coloredLinkKMLDocument = factory.createDocumentType();
		coloredLinkKMLDocument.setName("root feature of colored link KML");
		coloredLinkKML.setFeature(factory.createDocument(coloredLinkKMLDocument));
		
		System.out.println("    done.");
		
//		System.out.println(mainKMLFilename);
//		System.out.println(coloredLinkKMLFilename);
		
	}
	
	private static void generateStyles() {
		
		System.out.println("    generating styles...");

		workFacilityStyle = factory.createStyleType();
		workFacilityStyle.setId("workFacilityStyle");
		IconStyleType workIconStyle = factory.createIconStyleType();
		IconStyleIconType workIcon = factory.createIconStyleIconType();
		workIcon.setHref("http://maps.google.com/mapfiles/kml/paddle/W.png");
		workIconStyle.setIcon(workIcon);
		workFacilityStyle.setIconStyle(workIconStyle);
		mainKMLDocument.getStyleSelector().add(factory.createStyle(workFacilityStyle));
		
		leisureFacilityStyle = factory.createStyleType();
		leisureFacilityStyle.setId("leisureFacilityStyle");
		IconStyleType leisureIconStyle = factory.createIconStyleType();
		IconStyleIconType leisureIcon = factory.createIconStyleIconType();
		leisureIcon.setHref("http://maps.google.com/mapfiles/kml/pal2/icon57.png");
		leisureIconStyle.setColor(new byte[]{(byte)255, 0, 0, (byte)255});
		leisureIconStyle.setScale(new Float(Math.sqrt(4.0)));
		leisureIconStyle.setIcon(leisureIcon);
		leisureFacilityStyle.setIconStyle(leisureIconStyle);
		mainKMLDocument.getStyleSelector().add(factory.createStyle(leisureFacilityStyle));	
		
		blueLineStyle = factory.createStyleType();
		blueLineStyle.setId("blueLineStyle");
		LineStyleType blueLine = factory.createLineStyleType();
		blueLine.setColor(new byte[]{(byte)127, (byte)255, 0, 0});
		blueLine.setWidth(new Float(5.0));
		blueLineStyle.setLineStyle(blueLine);
		mainKMLDocument.getStyleSelector().add(factory.createStyle(blueLineStyle));
		
		int intervals = 24*4;
		byte alpha = (byte)255;
		
		for (int ii = 0; ii < intervals; ii++) {
			
			byte r = new Double(127 * (Math.sin((ii * 2 * Math.PI) / intervals) + 1)).byteValue();
			//System.out.println(r);
			byte g = new Double(127 * (Math.cos((ii * 2 * Math.PI) / intervals) + 1)).byteValue();
			//System.out.println(g);
			byte b = new Double(127 * (Math.sin((ii * 2 * Math.PI) / intervals) * (-1) + 1)).byteValue();
			//System.out.println(b);
			
			StyleType st = factory.createStyleType();
			st.setId("networkStyle" + ii);
			LineStyleType colorfulLine = factory.createLineStyleType();
			colorfulLine.setColor(new byte[]{alpha, b, g, r});
			colorfulLine.setWidth(new Float(5.0));
			st.setLineStyle(colorfulLine);
			coloredLinkKMLDocument.getStyleSelector().add(factory.createStyle(st));
			
		}

		System.out.println("    done.");

	}
	
	private static void generateData() {
		
		System.out.println("    generating data...");

		FolderType facilitiesFolder = factory.createFolderType();
		facilitiesFolder.setName("facilities");
		facilitiesFolder.setDescription("Contains all the facilities.");
		facilitiesFolder.setVisibility(Boolean.FALSE);
		mainKMLDocument.getFeature().add(factory.createFolder(facilitiesFolder));
		
		// Hardturm
		CoordWGS84 hardturmCoord = CoordWGS84.createFromWGS84(8, 30, 17, 47, 23, 35);
		PointType hardturmPoint = factory.createPointType();
		hardturmPoint.getCoordinates().add(
				Util.getKMLCoordinateString(
						hardturmCoord.getLongitude(), 
						hardturmCoord.getLatitude(), 
						0.0));
		
		PlacemarkType hardturm = factory.createPlacemarkType();
		hardturm.setName("Hardturmstadion Zürich");
		hardturm.setDescription("Das ist das Hardturmstadion.");
		hardturm.setStyleUrl(Util.getStyleUrlString(leisureFacilityStyle));
		hardturm.setGeometry(factory.createPoint(hardturmPoint));
		facilitiesFolder.getFeature().add(factory.createPlacemark(hardturm));
		
		// Unterer Letten
		CoordWGS84 lettenCoord = CoordWGS84.createFromWGS84(8, 31, 46, 47, 23, 20);
		PointType lettenPoint = factory.createPointType();
		lettenPoint.getCoordinates().add(
				Util.getKMLCoordinateString(
						lettenCoord.getLongitude(), 
						lettenCoord.getLatitude(), 
						0.0));
		
		LookAtType lookAt = factory.createLookAtType();
		lookAt.setHeading(180.0);
		lookAt.setTilt(45.0);
		lookAt.setRange(10000.0);
		lookAt.setLongitude(lettenCoord.getLongitude());
		lookAt.setLatitude(lettenCoord.getLatitude());
		
		PlacemarkType letten = factory.createPlacemarkType();
		letten.setName("Badi Unterer Letten");
		letten.setDescription("Das ist eine Flussbadeanstalt.");
		letten.setLookAt(lookAt);
		letten.setGeometry(factory.createPoint(lettenPoint));
		letten.setStyleUrl(Util.getStyleUrlString(leisureFacilityStyle));
		facilitiesFolder.getFeature().add(factory.createPlacemark(letten));
		
		// ETH
		CoordWGS84 ethCoord = CoordWGS84.createFromWGS84(8, 30, 33, 47, 24, 27);
		PointType ethPoint = factory.createPointType();
		ethPoint.getCoordinates().add(
				Util.getKMLCoordinateString(
						ethCoord.getLongitude(), 
						ethCoord.getLatitude(), 
						0.0));
		
		PlacemarkType eth = factory.createPlacemarkType();
		eth.setName("ETH Hönggerberg");
		eth.setDescription("The ETH Zurich outpost.");
		eth.setStyleUrl(Util.getStyleUrlString(workFacilityStyle));
		eth.setGeometry(factory.createPoint(ethPoint));
		facilitiesFolder.getFeature().add(factory.createPlacemark(eth));
		
		// empty placemark
		PlacemarkType emptyPlacemark = factory.createPlacemarkType();
		emptyPlacemark.setName("empty placemark");
		facilitiesFolder.getFeature().add(factory.createPlacemark(emptyPlacemark));
		
		// lines folder
		FolderType networkLinksFolder = factory.createFolderType();
		networkLinksFolder.setName("Contains some lines.");
		mainKMLDocument.getFeature().add(factory.createFolder(networkLinksFolder));
		
		// blue 8 o'clock line
		LineStringType lst;
		
		lst = factory.createLineStringType();
		lst.getCoordinates().add(
				Util.getKMLCoordinateString(
						hardturmCoord.getLongitude(), 
						hardturmCoord.getLatitude(), 
						0.0));
		lst.getCoordinates().add(
				Util.getKMLCoordinateString(
						lettenCoord.getLongitude(), 
						lettenCoord.getLatitude(), 
						0.0));
		
		TimeStampType ts = factory.createTimeStampType();
		ts.setWhen(Util.getKMLDateString(new GregorianCalendar(1970, 0, 1, 8, 0, 0)));
		
		PlacemarkType blue8oClockLine = factory.createPlacemarkType();
		blue8oClockLine.setName("the 8:00 link");
		blue8oClockLine.setDescription("A line that exists only at 8:00 AM.");
		blue8oClockLine.setStyleUrl(Util.getStyleUrlString(blueLineStyle));
		blue8oClockLine.setTimePrimitive(factory.createTimeStamp(ts));
		blue8oClockLine.setGeometry(factory.createLineString(lst));
		networkLinksFolder.getFeature().add(factory.createPlacemark(blue8oClockLine));
		
		System.out.println("    done.");
		
		// colorful time dependent line
		GregorianCalendar gcBegin, gcEnd;
		PlacemarkType colorfulLine;
		TimeSpanType tSpan;
		lst = factory.createLineStringType();
		lst.getCoordinates().add(
				Util.getKMLCoordinateString(
						ethCoord.getLongitude(), 
						ethCoord.getLatitude(), 
						0.0));
		lst.getCoordinates().add(
				Util.getKMLCoordinateString(
						hardturmCoord.getLongitude(), 
						hardturmCoord.getLatitude(), 
						0.0));
		lst.setTessellate(Boolean.TRUE);
		
		for (int ii = 0; ii < 24*4; ii++) {
			
			gcBegin = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15, 0);
			gcEnd = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15 + 14, 59);
			
			tSpan = factory.createTimeSpanType();
			tSpan.setBegin(Util.getKMLDateString(gcBegin));
			tSpan.setEnd(Util.getKMLDateString(gcEnd));
			
			colorfulLine = factory.createPlacemarkType();
			colorfulLine.setName(new Integer(ii).toString());
			colorfulLine.setDescription("Link state in interval #" + new Integer(ii).toString());
			colorfulLine.setStyleUrl("networkStyle" + ii);
			colorfulLine.setTimePrimitive(factory.createTimeSpan(tSpan));
			colorfulLine.setGeometry(factory.createLineString(lst));
			coloredLinkKMLDocument.getFeature().add(factory.createPlacemark(colorfulLine));
			
		}

		// link the two documents
		LodType lod = factory.createLodType();
		lod.setMinLodPixels(new Float(256));
		
		LatLonAltBoxType llab = factory.createLatLonAltBoxType();
		llab.setNorth(Math.max(ethCoord.getLatitude(), hardturmCoord.getLatitude()));
		llab.setSouth(Math.min(ethCoord.getLatitude(), hardturmCoord.getLatitude()));
		llab.setWest(Math.min(ethCoord.getLongitude(), hardturmCoord.getLongitude()));
		llab.setEast(Math.max(ethCoord.getLongitude(), hardturmCoord.getLongitude()));

		RegionType region = factory.createRegionType();
		region.setLatLonAltBox(llab);
		region.setLod(lod);
		
		LinkType link = factory.createLinkType();
		link.setHref(coloredLinkKMLFilename);
		
		NetworkLinkType nl = factory.createNetworkLinkType();
		nl.setName("network link to the link");
		nl.setLink(link);
		nl.setRegion(region);
		networkLinksFolder.getFeature().add(factory.createNetworkLink(nl));
				
		System.out.println("    done.");
	
	}
	
	private static void write() throws JAXBException {
		
		System.out.println("    writing KML files out...");

		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		try {
			marshaller.marshal(factory.createKml(mainKML), Util.getOutputStream(mainKMLFilename, useCompression));
			marshaller.marshal(factory.createKml(coloredLinkKML), Util.getOutputStream(coloredLinkKMLFilename, useCompression));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		System.out.println("    done.");
		
	}
	
}
