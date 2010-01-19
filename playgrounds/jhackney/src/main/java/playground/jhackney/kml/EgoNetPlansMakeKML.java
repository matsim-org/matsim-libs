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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LineStringType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.LookAtType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.KMZWriter;

import playground.jhackney.socialnetworks.socialnet.EgoNet;

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

	private static ObjectFactory kmlObjectFactory = new ObjectFactory();

	private static KmlType myKML;
//	, coloredLinkKML;
	private static DocumentType myKMLDocument;
//	, coloredLinkKMLDocument;
	private static ActivityFacilities facilities; 

	private static StyleType workStyle, leisureStyle, blueLineStyle,
	educStyle, shopStyle, homeStyle;//, agentLinkStyle;
	private static LinkedHashMap<String,StyleType> facStyle= new LinkedHashMap<String,StyleType>();
	private static CoordinateTransformation trafo;
	private static Config config = null;


	public static void setUp(Config config, NetworkLayer network, ActivityFacilities facilities) {
		EgoNetPlansMakeKML.facilities = facilities;
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

		myKML = kmlObjectFactory.createKmlType();
		myKMLDocument = kmlObjectFactory.createDocumentType();
		myKML.setAbstractFeatureGroup(kmlObjectFactory.createDocument(myKMLDocument));

//		coloredLinkKML = new KML();
//		coloredLinkKMLDocument = new Document("network main feature");
//		coloredLinkKML.setFeature(coloredLinkKMLDocument);

		System.out.println("    done.");

		///////////////////////////
		// display road network
		///////////////////////////
		StyleType linkStyle = kmlObjectFactory.createStyleType();
		linkStyle.setId("defaultLinkStyle");

		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		byte[] color = new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00};
		lst.setColor(color);
		lst.setWidth(2.0);

		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(linkStyle));

		FolderType networkFolder = kmlObjectFactory.createFolderType();
		networkFolder.setName("used network");
		networkFolder.setDescription("used network");
		networkFolder.setVisibility(false);

		myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(networkFolder));

		for (LinkImpl link : network.getLinks().values()) {
			networkFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(generateLinkPlacemark(link, linkStyle, trafo)));
		}

	}

	public static void generateStyles() {

		LinkType link = null;
		IconStyleType icon = null;

		if(config.getModule(KML21_MODULE)==null) return;

		System.out.println("    generating styles...");

//		agentLinkStyle = new Style("agentLinkStyle");
//		myKMLDocument.addStyle(agentLinkStyle);
//		agentLinkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "ff", "ff"), ColorStyle.DEFAULT_COLOR_MODE, 14));

		double labelScale = 1.0;

		workStyle = kmlObjectFactory.createStyleType();
		workStyle.setId("workStyle");
		link = kmlObjectFactory.createLinkType();
		link.setHref("http://maps.google.com/mapfiles/kml/paddle/W.png");
		icon = kmlObjectFactory.createIconStyleType();
		icon.setIcon(link);
		icon.setScale(labelScale);
		workStyle.setIconStyle(icon);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(workStyle));

		leisureStyle = kmlObjectFactory.createStyleType();
		leisureStyle.setId("leisureFacilityStyle");
		link = kmlObjectFactory.createLinkType();
		link.setHref("http://maps.google.com/mapfiles/kml/paddle/L.png");
		icon = kmlObjectFactory.createIconStyleType();
		icon.setIcon(link);
		icon.setScale(labelScale);
		leisureStyle.setIconStyle(icon);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(leisureStyle));

		educStyle = kmlObjectFactory.createStyleType();
		educStyle.setId("educStyle");
		link = kmlObjectFactory.createLinkType();
		link.setHref("http://maps.google.com/mapfiles/kml/paddle/E.png");
		icon = kmlObjectFactory.createIconStyleType();
		icon.setIcon(link);
		icon.setScale(labelScale);
		educStyle.setIconStyle(icon);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(educStyle));

		shopStyle = kmlObjectFactory.createStyleType();
		shopStyle.setId("shopStyle");
		link = kmlObjectFactory.createLinkType();
		link.setHref("http://maps.google.com/mapfiles/kml/paddle/S.png");
		icon = kmlObjectFactory.createIconStyleType();
		icon.setIcon(link);
		icon.setScale(labelScale);
		shopStyle.setIconStyle(icon);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(shopStyle));

		homeStyle = kmlObjectFactory.createStyleType();
		homeStyle.setId("homeStyle");
		link = kmlObjectFactory.createLinkType();
		link.setHref("http://maps.google.com/mapfiles/kml/paddle/H.png");
		icon = kmlObjectFactory.createIconStyleType();
		icon.setIcon(link);
		icon.setScale(labelScale);
		homeStyle.setIconStyle(icon);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(homeStyle));


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


	public static void loadData(PersonImpl myPerson, Network network){

		if(config.getModule(KML21_MODULE)==null) return;

		System.out.println("    loading Plan data. Processing EgoNet ...");

		// load Data into KML folders for myPerson
		int i=0;
		loadData(myPerson, 0, 1, network);

		// Proceed to the EgoNet of myPerson
		ArrayList<Person> persons = ((EgoNet)myPerson.getCustomAttributes().get(EgoNet.NAME)).getAlters();
		Iterator<Person> altersIt= persons.iterator();

		while(altersIt.hasNext()){
			Person p = altersIt.next();
			i++;
			loadData(p,i,persons.size(), network);
		}

		System.out.println(" ... done.");
	}
	public static void loadData(Person myPerson, int i, int nColors, Network network) {

		System.out.println("    loading Plan data. Processing person ...");

		Plan myPlan = myPerson.getSelectedPlan();

		byte[] color = setColor(i, nColors+1);
//		setFacStyles(color);

		StyleType agentLinkStyle = kmlObjectFactory.createStyleType();
		agentLinkStyle.setId("agentLinkStyle"+myPerson.getId().toString());
		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		lst.setColor(color);
		lst.setWidth(14.0);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(agentLinkStyle));

		FolderType agentFolder = kmlObjectFactory.createFolderType();
		agentFolder.setName("agent "+myPlan.getPerson().getId().toString());
		agentFolder.setDescription("Contains one agent");
		myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(agentFolder));

		FolderType facilitiesFolder = kmlObjectFactory.createFolderType();
		facilitiesFolder.setName("facilities "+myPlan.getPerson().getId().toString());
		facilitiesFolder.setDescription("Contains all the facilities.");
		facilitiesFolder.setVisibility(false);
		myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(facilitiesFolder));

		Iterator<PlanElement> actLegIter = myPlan.getPlanElements().iterator();
		Activity act0 = (Activity) actLegIter.next(); // assumes first plan element is always an Activity
		makeActKML(myPerson, act0, agentFolder, agentLinkStyle, network);
		while(actLegIter.hasNext()){//alternates Act-Leg-Act-Leg and ends with Act
			Object o = actLegIter.next();
			if (o instanceof LegImpl) {
				LegImpl leg = (LegImpl) o;
	
				for (Id routeLinkId : ((NetworkRouteWRefs) leg.getRoute()).getLinkIds()) {
					Link routeLink = network.getLinks().get(routeLinkId);
					PlacemarkType agentLinkL = generateLinkPlacemark(routeLink, agentLinkStyle, trafo);
	
					boolean linkExists = false;
					ListIterator<JAXBElement<? extends AbstractFeatureType>> li = agentFolder.getAbstractFeatureGroup().listIterator();
					while (li.hasNext() && (linkExists == false)) {
	
						JAXBElement<? extends AbstractFeatureType> abstractFeature = li.next();
						if (abstractFeature.getName().equals(agentLinkL.getName())) {
							linkExists = true;
						}
	
					}
					if (!linkExists) {
						agentFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(agentLinkL));
					}
				}
			} else if (o instanceof Activity) {
				Activity act = (Activity) o;
				makeActKML(myPerson, act, agentFolder,agentLinkStyle, network);
			}
		}



		// Fill the facilities folder

		for (PlanElement pe : myPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl myAct = (ActivityImpl) pe;
				StyleType myStyle=facStyle.get(myAct.getType());
				
				PlacemarkType aFacility = kmlObjectFactory.createPlacemarkType();
				aFacility.setName(myAct.getType()+" facility");
				aFacility.setDescription(facilities.getFacilities().get(myAct.getFacilityId()).getActivityOptions().get(myAct.getType()).toString());
				aFacility.setAddress("address");
				aFacility.setStyleUrl(myStyle.getId());
				
				// Get the coordinates of the facility associated with the Act and transform
				// to WGS84 for GoogleEarth
				
				Coord geometryCoord = trafo.transform(myAct.getCoord());
				PointType myPoint = kmlObjectFactory.createPointType();
				myPoint.getCoordinates().add(Double.toString(geometryCoord.getX()) + "," + Double.toString(geometryCoord.getY()) + ",0.0");
				aFacility.setAbstractGeometryGroup(kmlObjectFactory.createPoint(myPoint));
				
				LookAtType lookAt = kmlObjectFactory.createLookAtType();
				lookAt.setLongitude(geometryCoord.getX());
				lookAt.setLatitude(geometryCoord.getY());
				aFacility.setAbstractViewGroup(kmlObjectFactory.createLookAt(lookAt));
				
				boolean facilityExists = false;
				ListIterator<JAXBElement<? extends AbstractFeatureType>> li = agentFolder.getAbstractFeatureGroup().listIterator();
				while (li.hasNext() && (facilityExists == false)) {
					
					JAXBElement<? extends AbstractFeatureType> abstractFeature = li.next();
					if (abstractFeature.getName().equals(aFacility.getName())) {
						facilityExists = true;
					}
					
				}
				if (!facilityExists) {
					agentFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aFacility));
				}
			}
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

	private static void makeActKML(Person myPerson, Activity act, FolderType agentFolder, StyleType agentLinkStyle, Network network) {
		// TODO Auto-generated method stub

		String styleUrl = null;
		String fullActName = null;
		char actType = act.getType().charAt(0);
		double actEndTime;
		switch(actType) {
		case 'h':
			styleUrl = homeStyle.getId();
			if (act.getStartTime() == 0.0) {
				fullActName = "morning home "+myPerson.getId();
			} else {
				fullActName = "evening home "+myPerson.getId();
				System.out.println(fullActName);
			}
			break;
		case 's':
			styleUrl = shopStyle.getId();
			fullActName = "shop"+myPerson.getId();
			break;
		case 'l':
			styleUrl = leisureStyle.getId();
			fullActName = "leisure"+myPerson.getId();
			break;
		case 'w':
			styleUrl = workStyle.getId();
			fullActName = "work"+myPerson.getId();
			break;
		case 'e':
			styleUrl = educStyle.getId();
			fullActName = "education"+myPerson.getId();
			break;
		}

		actEndTime = act.getEndTime();
		if (actEndTime == Time.UNDEFINED_TIME) {
			actEndTime = 24.0 * 60 * 60;
		}

		PlacemarkType pl = kmlObjectFactory.createPlacemarkType();
		pl.setName(fullActName+": "+Time.writeTime(act.getStartTime()) + " - " + Time.writeTime(actEndTime));
		pl.setDescription(fullActName + " activity");
		pl.setStyleUrl(styleUrl);

		Coord geometryCoord = trafo.transform(new CoordImpl(act.getCoord().getX(), act.getCoord().getY()));
		PointType actPoint = kmlObjectFactory.createPointType();
		actPoint.getCoordinates().add(Double.toString(geometryCoord.getX()) + "," + Double.toString(geometryCoord.getY()) + ",0.0");
		pl.setAbstractGeometryGroup(kmlObjectFactory.createPoint(actPoint));

		agentFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(pl));

//		if (!fullActName.equals("evening home")) {
		Link actLink = network.getLinks().get(act.getLinkId());

		PlacemarkType agentLink = generateLinkPlacemark(actLink, agentLinkStyle, trafo);

		boolean linkExists = false;
		ListIterator<JAXBElement<? extends AbstractFeatureType>> li = agentFolder.getAbstractFeatureGroup().listIterator();
		while (li.hasNext() && (linkExists == false)) {

			JAXBElement<? extends AbstractFeatureType> abstractFeature = li.next();
			if (abstractFeature.getName().equals(agentLink.getName())) {
				linkExists = true;
			}

		}
		if (!linkExists) {
			agentFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(agentLink));
		}
//		}

	}

//	private static void setFacStyles(Color color) {

//	// Generates facility styles specific to agent

//	double labelScale = 1.0;
//	workStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
//	workStyle.setLabelStyle(
//	new LabelStyle(
//	color,
//	ColorStyle.DEFAULT_COLOR_MODE,
//	labelScale));
//	leisureStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/L.png")));
//	leisureStyle.setLabelStyle(
//	new LabelStyle(
//	color,
//	ColorStyle.DEFAULT_COLOR_MODE,
//	labelScale));
//	educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/E.png")));
//	educStyle.setLabelStyle(
//	new LabelStyle(
//	color,
//	ColorStyle.DEFAULT_COLOR_MODE,
//	labelScale));
//	shopStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/S.png")));
//	shopStyle.setLabelStyle(
//	new LabelStyle(
//	color,
//	ColorStyle.DEFAULT_COLOR_MODE,
//	labelScale));
//	homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/H.png")));
//	homeStyle.setLabelStyle(
//	new LabelStyle(
//	color,
//	ColorStyle.DEFAULT_COLOR_MODE,
//	labelScale));
//	}

	private static byte[] setColor(int i, int intervals) {
		// returns a color as a function of integer i
		byte alpha = (byte) 255;

		byte r = (byte)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) + 1));
		System.out.println(r);
		byte g = (byte)(127.0 * (Math.cos((i * 2 * Math.PI) / intervals) + 1));
		System.out.println(g);
		byte b = (byte)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) * (-1) + 1));
		System.out.println(b);

		byte[] color = new byte[]{alpha, b, g, r};

		return color;
	}

	public static void write() {

		if(config.getModule(KML21_MODULE)==null) return;
		System.out.println("    writing KML files out...");

		if (!useCompression) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance("net.opengis.kml._2");

				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				marshaller.marshal(kmlObjectFactory.createKml(myKML), new FileOutputStream(mainKMLFilename));
			} catch (PropertyException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		} else {
			
			KMZWriter kmzWriter = new KMZWriter(mainKMLFilename);
			kmzWriter.writeMainKml(myKML);
			kmzWriter.close();
			
		}
//		myKMLDocumentWriter = new KMLWriter(coloredLinkKML, coloredLinkKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);
//		myKMLDocumentWriter.write();

		System.out.println("    done.");

	}

	private static PlacemarkType generateLinkPlacemark(Link link, StyleType style, CoordinateTransformation trafo) {

		PlacemarkType linkPlacemark = kmlObjectFactory.createPlacemarkType();
		linkPlacemark.setName("link" + link.getId());

		LineStringType lst = kmlObjectFactory.createLineStringType();

		Node fromNode = link.getFromNode();
		Coord fromNodeWorldCoord = fromNode.getCoord();
		Coord fromNodeGeometryCoord = trafo.transform(new CoordImpl(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		lst.getCoordinates().add(Double.toString(fromNodeGeometryCoord.getX()) + "," + Double.toString(fromNodeGeometryCoord.getY()) + ",0.0");

		Node toNode = link.getToNode();
		Coord toNodeWorldCoord = toNode.getCoord();
		Coord toNodeGeometryCoord = trafo.transform(new CoordImpl(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));
		lst.getCoordinates().add(Double.toString(toNodeGeometryCoord.getX()) + "," + Double.toString(toNodeGeometryCoord.getY()) + ",0.0");

		linkPlacemark.setStyleUrl(style.getId());
		linkPlacemark.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lst));

		return linkPlacemark;
	}

}

