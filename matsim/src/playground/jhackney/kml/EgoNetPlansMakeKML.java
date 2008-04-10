/* *********************************************************************** *
 * project: org.matsim.*
 * EgoNetPlansMakeKML.java
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

package playground.jhackney.kml;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.CoordWGS84;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.vis.kml.ColorStyle.ColorMode;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.utils.vis.kml.*;

public class EgoNetPlansMakeKML {

	// config parameters
	public static final String KML21_MODULE = "kml21";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_OUTPUT_KML_DEMO_MAIN_FILE = "outputEgoNetPlansKMLMainFile";
	public static final String CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE = "outputKMLDemoColoredLinkFile";
	public static final String CONFIG_USE_COMPRESSION = "useCompression";

	public static final String SEP = System.getProperty("file.separator");

	private static String mainKMLFilename;
	private static String coloredLinkKMLFilename;
	private static boolean useCompression = false;

	private static KML myKML, coloredLinkKML;
	private static Document myKMLDocument, coloredLinkKMLDocument;

	private static Style work10Style, leisureFacilityStyle, blueLineStyle,
	educStyle, shopStyle, homeStyle;
	private static HashMap<String,Style> facStyle= new HashMap<String,Style>();

//	private static Config config = null;


//	public static void main(final String[] args) {
//
//		config = Gbl.createConfig(args);
//
//		System.out.println("  starting KML demo...");
//
//		setUp();
//		generateStyles();
//		loaData(plan);
//		write();
//
//		System.out.println("  done.");
//	}

	public static void setUp(Config config) {

		System.out.println("    Set up...");

		mainKMLFilename =
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_MAIN_FILE);
		System.out.println(mainKMLFilename);
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
		educStyle = new Style("educStyle");
		myKMLDocument.addStyle(educStyle);
		shopStyle=new Style("shopStyle");
		myKMLDocument.addStyle(shopStyle);
		homeStyle=new Style("homeStyle");
		myKMLDocument.addStyle(homeStyle);

		work10Style.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
//		leisureFacilityStyle.setIconStyle(
//		new IconStyle(
//		new Icon("http://maps.google.com/mapfiles/kml/pal2/icon57.png"),
//		new Color("ff","00","00","ff"),
//		ColorStyle.DEFAULT_COLOR_MODE,
//		Math.sqrt(4.0)));
		leisureFacilityStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/L.png")));
//		educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/pal3/icon21.png")));
		educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/E.png")));
		shopStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/S.png")));
//		homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/pal3/icon23.png")));
		homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/H.png")));

		System.out.println("here i am");
		facStyle.put("home",homeStyle);
		facStyle.put("shop",shopStyle);
		facStyle.put("education",educStyle);
		facStyle.put("leisure",leisureFacilityStyle);
		facStyle.put("work",work10Style);
		

//		blueLineStyle = new Style("blueLineStyle");
//		myKMLDocument.addStyle(blueLineStyle);
//		blueLineStyle.setLineStyle(new LineStyle(new Color("7f","ff","00","00"), ColorStyle.DEFAULT_COLOR_MODE, 5));
//
//		int intervals = 24*4;
//		int alpha = 255;
//
//		for (int ii = 0; ii < intervals; ii++) {
//
//			int r = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) + 1));
//			//System.out.println(r);
//			int g = (int)(127.0 * (Math.cos((ii * 2 * Math.PI) / intervals) + 1));
//			//System.out.println(g);
//			int b = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) * (-1) + 1));
//			//System.out.println(b);
//
//			String aStr = Integer.toHexString(alpha);
//			if (aStr.length() == 1) {
//				aStr = "0".concat(aStr);
//			}
//			String rStr = Integer.toHexString(r);
//			if (rStr.length() == 1) {
//				rStr = "0".concat(rStr);
//			}
//			String gStr = Integer.toHexString(g);
//			if (gStr.length() == 1) {
//				gStr = "0".concat(gStr);
//			}
//			String bStr = Integer.toHexString(b);
//			if (bStr.length() == 1) {
//				bStr = "0".concat(bStr);
//			}
//
//			Color color = new Color(aStr, bStr, gStr, rStr);
//
//			Style s = new Style("networkStyle" + ii);
//			coloredLinkKMLDocument.addStyle(s);
//			s.setLineStyle(new LineStyle(color, ColorMode.NORMAL, 5));
//
//		}

		System.out.println("    done.");
	}

	public static void loadData(Plan myPlan) {

		System.out.println("    generating data...");

		// generate facilities in a folder, one for each Plan
		Folder facilitiesFolder = new Folder(
				"facilities"+myPlan.getPerson().getId().toString(),
				"facilities"+myPlan.getPerson().getId().toString(),
				"Contains all the facilities.",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(facilitiesFolder);

		// Fill the facilities folder

		ActIterator aIter = (ActIterator) myPlan.getIteratorAct();
		while(aIter.hasNext()){
			Act myAct = (Act) aIter.next();
			Style myStyle=facStyle.get(myAct.getType());
			Placemark aFacility = new Placemark(
					myAct.getType()+myPlan.getPerson().getId().toString(),
					myAct.getType()+myPlan.getPerson().getId().toString(),
					"descr",
					"address",
					Feature.DEFAULT_LOOK_AT,
					myStyle.getStyleUrl(),
					true,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);
			if(!facilitiesFolder.containsFeature(aFacility.getId())){
			facilitiesFolder.addFeature(aFacility);
			}

			// Get the coordinates of the facility associated with the Act and transform
			// to WGS84 for GoogleEarth
			CoordI myActCoordCH1903=myPlan.getPerson().getKnowledge().map.getActivity(myAct).getFacility().getCenter();
			CoordI myActCoordWGS84 = new CH1903LV03toWGS84().transform(myActCoordCH1903);
//			double latdeg=(double) (int) myActCoordWGS84.getY();
//			double latmin=(double) (int)((myActCoordWGS84.getY()-latdeg)*60);
//			double latsec=myActCoordWGS84.getY()-latdeg-latmin*3600.;
//			double londeg=(double) (int) myActCoordWGS84.getX();
//			double lonmin=(double) (int)((myActCoordWGS84.getX()-latdeg)*60);
//			double lonsec=myActCoordWGS84.getX()-latdeg-latmin*3600.;
			CoordWGS84 myCoord = CoordWGS84.createFromWGS84(myActCoordWGS84.getX(), 0,0,myActCoordWGS84.getY(),0,0);
			Point myPoint = new Point(myCoord.getLongitude(), myCoord.getLatitude(), 0.0);
			aFacility.setGeometry(myPoint);
			aFacility.setLookAt(new LookAt(myCoord.getLongitude(),myCoord.getLatitude()));
	}
		
//Put all the agents into one folder? or have one folder per agent, like the facilities above
//		Folder agentsFolder = new Folder(
//				"agents",
//				"agents",
//				"Contains all the agents",
//				Feature.DEFAULT_ADDRESS,
//				Feature.DEFAULT_LOOK_AT,
//				Feature.DEFAULT_STYLE_URL,
//				true,
//				Feature.DEFAULT_REGION,
//				Feature.DEFAULT_TIME_PRIMITIVE);
//		myKMLDocument.addFeature(agentsFolder);
//
//		Folder networkLinksFolder = new Folder("networkLinks");
//		myKMLDocument.addFeature(networkLinksFolder);

//		Placemark aLineString = new Placemark(
//		"8link",
//		"the 8:00 link",
//		"A line that exists only at 8:00 AM.",
//		Feature.DEFAULT_ADDRESS,
//		Feature.DEFAULT_LOOK_AT,
//		blueLineStyle.getStyleUrl(),
//		true,
//		Feature.DEFAULT_REGION,
//		new TimeStamp(new GregorianCalendar(1970, 0, 1, 8, 0, 0)));
//		aLineString.setGeometry(new LineString(lettenPoint, hardturmPoint));
//		networkLinksFolder.addFeature(aLineString);

//		Region linkRegion = new Region(
//		Math.max(ethPoint.getLatitude(), hardturmPoint.getLatitude()),
//		Math.min(ethPoint.getLatitude(), hardturmPoint.getLatitude()),
//		Math.min(ethPoint.getLongitude(), hardturmPoint.getLongitude()),
//		Math.max(ethPoint.getLongitude(), hardturmPoint.getLongitude()),
//		256,
//		Region.DEFAULT_MAX_LOD_PIXELS
//		);

//		for (int ii = 0; ii < 24*4; ii++) {

//		GregorianCalendar gcBegin = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15, 0);
//		GregorianCalendar gcEnd = new GregorianCalendar(1970, 0, 1, ii / 4, (ii % 4) * 15 + 14, 59);

//		String styleId = "networkStyle" + ii;

//		Placemark aTemporaryLineString = new Placemark(
//		Integer.toString(ii),
//		Integer.toString(ii),
//		"Link state in interval #" + Integer.toString(ii),
//		Feature.DEFAULT_ADDRESS,
//		Feature.DEFAULT_LOOK_AT,
//		coloredLinkKMLDocument.getStyle(styleId).getStyleUrl(),
//		Feature.DEFAULT_VISIBILITY,
//		Feature.DEFAULT_REGION,
//		new TimeSpan(gcBegin, gcEnd));
//		aTemporaryLineString.setGeometry(new LineString(ethPoint, hardturmPoint, Geometry.DEFAULT_EXTRUDE, true, Geometry.DEFAULT_ALTITUDE_MODE));

//		coloredLinkKMLDocument.addFeature(aTemporaryLineString);

//		}

//		NetworkLink nl = new NetworkLink(
//		"network link to the link",
//		new Link(coloredLinkKMLFilename),
//		"network link to the link",
//		"network link to the link",
//		Feature.DEFAULT_ADDRESS,
//		Feature.DEFAULT_LOOK_AT,
//		Feature.DEFAULT_STYLE_URL,
//		Feature.DEFAULT_VISIBILITY,
//		linkRegion,
//		Feature.DEFAULT_TIME_PRIMITIVE
//		);

//		networkLinksFolder.addFeature(nl);

		System.out.println("    done.");

	}

	public static void write() {

		System.out.println("    writing KML files out...");

		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(myKML, mainKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
		myKMLDocumentWriter.write();
//		myKMLDocumentWriter = new KMLWriter(coloredLinkKML, coloredLinkKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
//		myKMLDocumentWriter.write();

		System.out.println("    done.");

	}

}

