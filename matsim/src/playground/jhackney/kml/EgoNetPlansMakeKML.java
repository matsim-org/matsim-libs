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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.misc.Time;
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
//	private static String coloredLinkKMLFilename;
	private static boolean useCompression = false;

	private static KML myKML;
//	, coloredLinkKML;
	private static Document myKMLDocument;
//	, coloredLinkKMLDocument;

	private static Style workStyle, leisureStyle, blueLineStyle,
	educStyle, shopStyle, homeStyle;//, agentLinkStyle;
	private static HashMap<String,Style> facStyle= new HashMap<String,Style>();
	private static CoordinateTransformation trafo;
	private static Config config = null;


	public static void setUp(Config config, NetworkLayer network) {
		EgoNetPlansMakeKML.config=config;
		if(config.getModule(KML21_MODULE)==null) return;
		
		System.out.println("    Set up...");

		trafo = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.CH1903_LV03, TransformationFactory.WGS84);
	
		mainKMLFilename =
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_MAIN_FILE);
		System.out.println(mainKMLFilename);
//		coloredLinkKMLFilename =
//		config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
//		SEP +
//		config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE);

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

//		coloredLinkKML = new KML();
//		coloredLinkKMLDocument = new Document("network main feature");
//		coloredLinkKML.setFeature(coloredLinkKMLDocument);

		System.out.println("    done.");

		///////////////////////////
		// display road network
		///////////////////////////
		Style linkStyle = new Style("defaultLinkStyle");
		myKMLDocument.addStyle(linkStyle);
		linkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "00", "00"), ColorStyle.DEFAULT_COLOR_MODE, 2));

		Folder networkFolder = new Folder(
				"used network",
				"used network",
				"used network",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(networkFolder);

		for (Link link : network.getLinks().values()) {
			networkFolder.addFeature(generateLinkPlacemark(link, linkStyle, trafo));
		}

	}

	public static void generateStyles() {

		if(config.getModule(KML21_MODULE)==null) return;
		
		System.out.println("    generating styles...");

//		agentLinkStyle = new Style("agentLinkStyle");
//		myKMLDocument.addStyle(agentLinkStyle);
//		agentLinkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "ff", "ff"), ColorStyle.DEFAULT_COLOR_MODE, 14));

		workStyle = new Style("workStyle");
		myKMLDocument.addStyle(workStyle);
		leisureStyle = new Style("leisureFacilityStyle");
		myKMLDocument.addStyle(leisureStyle);
		educStyle = new Style("educStyle");
		myKMLDocument.addStyle(educStyle);
		shopStyle=new Style("shopStyle");
		myKMLDocument.addStyle(shopStyle);
		homeStyle=new Style("homeStyle");
		myKMLDocument.addStyle(homeStyle);

		double labelScale = 1.0;
		workStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
		workStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		leisureStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/L.png")));
		leisureStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/E.png")));
		educStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		shopStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/S.png")));
		shopStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/H.png")));
		homeStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));


		facStyle.put("home",homeStyle);
		facStyle.put("shop",shopStyle);
		facStyle.put("education",educStyle);
		facStyle.put("leisure",leisureStyle);
		facStyle.put("work",workStyle);


//		blueLineStyle = new Style("blueLineStyle");
//		myKMLDocument.addStyle(blueLineStyle);
//		blueLineStyle.setLineStyle(new LineStyle(new Color("7f","ff","00","00"), ColorStyle.DEFAULT_COLOR_MODE, 5));

//		int intervals = 24*4;
//		int alpha = 255;

//		for (int ii = 0; ii < intervals; ii++) {

//		int r = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) + 1));
//		//System.out.println(r);
//		int g = (int)(127.0 * (Math.cos((ii * 2 * Math.PI) / intervals) + 1));
//		//System.out.println(g);
//		int b = (int)(127.0 * (Math.sin((ii * 2 * Math.PI) / intervals) * (-1) + 1));
//		//System.out.println(b);

//		String aStr = Integer.toHexString(alpha);
//		if (aStr.length() == 1) {
//		aStr = "0".concat(aStr);
//		}
//		String rStr = Integer.toHexString(r);
//		if (rStr.length() == 1) {
//		rStr = "0".concat(rStr);
//		}
//		String gStr = Integer.toHexString(g);
//		if (gStr.length() == 1) {
//		gStr = "0".concat(gStr);
//		}
//		String bStr = Integer.toHexString(b);
//		if (bStr.length() == 1) {
//		bStr = "0".concat(bStr);
//		}

//		Color color = new Color(aStr, bStr, gStr, rStr);

//		Style s = new Style("networkStyle" + ii);
//		coloredLinkKMLDocument.addStyle(s);
//		s.setLineStyle(new LineStyle(color, ColorMode.NORMAL, 5));

//		}

		System.out.println("    done.");
	}


	public static void loadData(Person myPerson){

		if(config.getModule(KML21_MODULE)==null) return;
		
		System.out.println("    loading Plan data. Processing EgoNet ...");

		// load Data into KML folders for myPerson
		int i=0;
		loadData(myPerson, 0, 1);

		// Proceed to the EgoNet of myPerson
		ArrayList<Person> persons = myPerson.getKnowledge().getEgoNet().getAlters();
		Iterator<Person> altersIt= persons.iterator();

		while(altersIt.hasNext()){
			Person p = altersIt.next();
			i++;
			loadData(p,i,persons.size());
		}

		System.out.println(" ... done.");
	}
	public static void loadData(Person myPerson, int i, int nColors) {

		System.out.println("    loading Plan data. Processing person ...");

		Plan myPlan = myPerson.getSelectedPlan();

		Color color = setColor(i, nColors+1);
//		setFacStyles(color);


		Style agentLinkStyle = new Style("agentLinkStyle"+myPerson.getId().toString());
		myKMLDocument.addStyle(agentLinkStyle);
		agentLinkStyle.setLineStyle(new LineStyle(color, ColorStyle.DEFAULT_COLOR_MODE, 14));


		//Put all the agents into one folder? or have one folder per agent, like the facilities above
		Folder agentFolder = new Folder(
				"agent "+myPlan.getPerson().getId().toString(),
				"agent "+myPlan.getPerson().getId().toString(),
				"Contains one agent",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				true,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(agentFolder);

		// put facilities in a folder, one for each Plan
		Folder facilitiesFolder = new Folder(
				"facilities "+myPlan.getPerson().getId().toString(),
				"facilities "+myPlan.getPerson().getId().toString(),
				"Contains all the facilities.",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(facilitiesFolder);

		ActLegIterator actLegIter = myPlan.getIterator();
		Act act0 = (Act) actLegIter.nextAct();
		makeActKML(myPerson, act0, agentFolder, agentLinkStyle);
		while(actLegIter.hasNextLeg()){//alternates Act-Leg-Act-Leg and ends with Act
			
				Leg leg = (Leg) actLegIter.nextLeg();

				Link[] routeLinks = (leg).getRoute().getLinkRoute();
				for (Link routeLink : routeLinks) {
					Placemark agentLinkL = generateLinkPlacemark(routeLink, agentLinkStyle, trafo);
					if(!agentFolder.containsFeature(agentLinkL.getId())){
						agentFolder.addFeature(agentLinkL);
					}
			}
				Act act = (Act) actLegIter.nextAct();
				makeActKML(myPerson, act, agentFolder,agentLinkStyle);
		}



		// Fill the facilities folder

		ActIterator aIter = myPlan.getIteratorAct();
		while(aIter.hasNext()){
			Act myAct = (Act) aIter.next();
			Style myStyle=facStyle.get(myAct.getType());
			Placemark aFacility = new Placemark(
					myAct.getType()+" facility",
					myAct.getType()+" facility",
					myAct.getFacility().getActivity(myAct.getType()).toString(),
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

			Coord geometryCoord = trafo.transform(myAct.getCoord());
			Point myPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
			aFacility.setGeometry(myPoint);
			aFacility.setLookAt(new LookAt(geometryCoord.getX(),geometryCoord.getY()));
		}



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

	private static void makeActKML(Person myPerson, Act act, Folder agentFolder, Style agentLinkStyle) {
		// TODO Auto-generated method stub

		String styleUrl = null;
		String fullActName = null;
		char actType = act.getType().charAt(0);
		double actEndTime;
		switch(actType) {
		case 'h':
			styleUrl = homeStyle.getStyleUrl();
			if (act.getStartTime() == 0.0) {
				fullActName = "morning home "+myPerson.getId();
			} else {
				fullActName = "evening home "+myPerson.getId();
				System.out.println(fullActName);
			}
			break;
		case 's':
			styleUrl = shopStyle.getStyleUrl();
			fullActName = "shop"+myPerson.getId();
			break;
		case 'l':
			styleUrl = leisureStyle.getStyleUrl();
			fullActName = "leisure"+myPerson.getId();
			break;
		case 'w':
			styleUrl = workStyle.getStyleUrl();
			fullActName = "work"+myPerson.getId();
			break;
		case 'e':
			styleUrl = educStyle.getStyleUrl();
			fullActName = "education"+myPerson.getId();
			break;
		}

		actEndTime = act.getEndTime();
		if (actEndTime == Time.UNDEFINED_TIME) {
			actEndTime = 24.0 * 60 * 60;
		}


		Placemark pl = new Placemark(
				fullActName,
				fullActName+": "+Time.writeTime(act.getStartTime()) + " - " + Time.writeTime(actEndTime),
				fullActName + " activity",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				styleUrl,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		agentFolder.addFeature(pl);

		Coord geometryCoord = trafo.transform(new CoordImpl(act.getCoord().getX(), act.getCoord().getY()));
		Point actPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
		pl.setGeometry(actPoint);

//		if (!fullActName.equals("evening home")) {
		Link actLink = act.getLink();
		Placemark agentLink = generateLinkPlacemark(actLink, agentLinkStyle, trafo);
		if(!agentFolder.containsFeature(agentLink.getId())){
			agentFolder.addFeature(agentLink);
		}
//		}

	}

	private static void setFacStyles(Color color) {

		// Generates facility styles specific to agent

		double labelScale = 1.0;
		workStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
		workStyle.setLabelStyle(
				new LabelStyle(
						color,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		leisureStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/L.png")));
		leisureStyle.setLabelStyle(
				new LabelStyle(
						color,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/E.png")));
		educStyle.setLabelStyle(
				new LabelStyle(
						color,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		shopStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/S.png")));
		shopStyle.setLabelStyle(
				new LabelStyle(
						color,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
		homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/H.png")));
		homeStyle.setLabelStyle(
				new LabelStyle(
						color,
						ColorStyle.DEFAULT_COLOR_MODE,
						labelScale));
	}

	private static Color setColor(int i, int intervals) {
		// returns a color as a function of integer i
		int alpha = 255;

		int r = (int)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) + 1));
		System.out.println(r);
		int g = (int)(127.0 * (Math.cos((i * 2 * Math.PI) / intervals) + 1));
		System.out.println(g);
		int b = (int)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) * (-1) + 1));
		System.out.println(b);

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
		return color;
	}

	public static void write() {

		if(config.getModule(KML21_MODULE)==null) return;
		System.out.println("    writing KML files out...");

		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(myKML, mainKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
		myKMLDocumentWriter.write();
//		myKMLDocumentWriter = new KMLWriter(coloredLinkKML, coloredLinkKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
//		myKMLDocumentWriter.write();

		System.out.println("    done.");

	}

	private static Placemark generateLinkPlacemark(Link link, Style style, CoordinateTransformation trafo) {

		Placemark linkPlacemark = null;

		Node fromNode = link.getFromNode();
		org.matsim.utils.geometry.CoordImpl fromNodeWorldCoord = fromNode.getCoord();
		org.matsim.utils.geometry.CoordImpl fromNodeGeometryCoord = (CoordImpl) trafo.transform(new CoordImpl(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		Point fromPoint = new Point(fromNodeGeometryCoord.getX(), fromNodeGeometryCoord.getY(), 0.0);

		Node toNode = link.getToNode();
		org.matsim.utils.geometry.CoordImpl toNodeWorldCoord = toNode.getCoord();
		org.matsim.utils.geometry.CoordImpl toNodeGeometryCoord = (CoordImpl) trafo.transform(new CoordImpl(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));
		Point toPoint = new Point(toNodeGeometryCoord.getX(), toNodeGeometryCoord.getY(), 0.0);

		linkPlacemark = new Placemark(
				"link" + link.getId(),
				Feature.DEFAULT_NAME,
				Feature.DEFAULT_DESCRIPTION,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				style.getStyleUrl(),
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		linkPlacemark.setGeometry(new LineString(fromPoint, toPoint));

		return linkPlacemark;
	}

}

