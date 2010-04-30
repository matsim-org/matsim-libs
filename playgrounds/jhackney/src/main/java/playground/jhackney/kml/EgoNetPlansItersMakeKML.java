/* *********************************************************************** *
 * project: org.matsim.*
 * EgoNetPlansItersMakeKML2.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.TreeMap;

import javax.xml.bind.JAXBElement;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.AbstractStyleSelectorType;
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
import net.opengis.kml._2.TimeSpanType;
import net.opengis.kml._2.TimeStampType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.knowledges.Knowledges;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

import playground.jhackney.activitySpaces.ActivitySpace;
import playground.jhackney.activitySpaces.ActivitySpaceEllipse;
import playground.jhackney.activitySpaces.ActivitySpaces;
import playground.jhackney.activitySpaces.PersonCalcActivitySpace;
import playground.jhackney.algorithms.PersonCalcEgoSpace;
import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class EgoNetPlansItersMakeKML {

	// config parameters
	public static final String KML21_MODULE = "kml21";
	public static final String CONFIG_OUTPUT_DIRECTORY = "outputDirectory";
	public static final String CONFIG_OUTPUT_KML_DEMO_MAIN_FILE = "outputEgoNetPlansKMLMainFile";
	public static final String CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE = "outputKMLDemoColoredLinkFile";
	public static final String CONFIG_USE_COMPRESSION = "useCompression";

	public static final String SEP = System.getProperty("file.separator");

	private static String mainKMLFilename;
//	private static String coloredLinkKMLFilename;
//	private static boolean useCompression = false;

	private static ObjectFactory kmlObjectFactory = new ObjectFactory();

	private static KmlType myKML;
//	, coloredLinkKML;
	private static DocumentType myKMLDocument;
//	, coloredLinkKMLDocument;

	private static FolderType networkFolder=null;

	private static StyleType workStyle, leisureStyle,
	educStyle, shopStyle, homeStyle;//, agentLinkStyle;
	private static LinkedHashMap<String,StyleType> facStyle= new LinkedHashMap<String,StyleType>();
	private static CoordinateTransformation trafo;
	private static Config config = null;
	private static final Logger log = Logger.getLogger(EgoNetPlansItersMakeKML.class);

	private static TreeMap<String,FolderType> agentMap = new TreeMap<String,FolderType>();
	private static Person ego;
//	private static TreeMap<Id,Color> colors = new TreeMap<Id,Color>();
	private static TreeMap<Id,Byte[]> colors = new TreeMap<Id,Byte[]>();
	private static TimeStampType timeStamp;
	private static TimeSpanType timeSpan;
	private static int nColors=13;//36
	private static Person ai;
	private static Knowledges knowledges;
	private static ActivityFacilities facilities;

	public static void setUp(Config config, Network network, ActivityFacilities facilities) {
		EgoNetPlansItersMakeKML.facilities = facilities;
		EgoNetPlansItersMakeKML.config=config;
		if(config.getModule(KML21_MODULE)==null) return;

		log.info("    Set up...");

		trafo = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.CH1903_LV03, TransformationFactory.WGS84);

		mainKMLFilename =
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
			SEP +
			config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_MAIN_FILE);
		log.info(mainKMLFilename);
//		coloredLinkKMLFilename =
//		config.getParam(KML21_MODULE, CONFIG_OUTPUT_DIRECTORY) +
//		SEP +
//		config.getParam(KML21_MODULE, CONFIG_OUTPUT_KML_DEMO_COLORED_LINK_FILE);

//		if (config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("true")) {
//		useCompression = true;
//		} else if(config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION).equals("false")) {
//		useCompression = false;
//		} else {
//		Gbl.errorMsg(
//		"Invalid value for config parameter " + CONFIG_USE_COMPRESSION +
//		" in module " + KML21_MODULE +
//		": \"" + config.getParam(KML21_MODULE, CONFIG_USE_COMPRESSION) + "\"");
//		}

		myKML = kmlObjectFactory.createKmlType();
		myKMLDocument = kmlObjectFactory.createDocumentType();
		myKML.setAbstractFeatureGroup(kmlObjectFactory.createDocument(myKMLDocument));

//		coloredLinkKML = new KML();
//		coloredLinkKMLDocument = new Document("network main feature");
//		coloredLinkKML.setFeature(coloredLinkKMLDocument);

		log.info("    done.");

		///////////////////////////
		// display road network
		///////////////////////////
		StyleType linkStyle = kmlObjectFactory.createStyleType();
		linkStyle.setId("defaultLinkStyle");
		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		byte[] color = new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00};
		lst.setColor(color);
		lst.setWidth(1.0);
		linkStyle.setLineStyle(lst);
		myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(linkStyle));

		networkFolder = kmlObjectFactory.createFolderType();
		networkFolder.setName("network used");
//		networkFolder.setDescription(network.getName());
		networkFolder.setVisibility(false);
		myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(networkFolder));

		for (Link link : network.getLinks().values()) {
			networkFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(generateLinkPlacemark(link, linkStyle, trafo)));
		}

	}

	public static void generateStyles() {

		LinkType link = null;
		IconStyleType icon = null;

		if(config.getModule(KML21_MODULE)==null) return;

		log.info("    generating styles...");

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

		log.info("    done.");

		log.info("Setting nColors = "+ nColors);
	}


	public static void loadData(Person myPerson, int iter, Knowledges kn, Network network){

		ego=myPerson;
		knowledges = kn;
		ai=myPerson;
		if(config.getModule(KML21_MODULE)==null) return;

		log.info("    loading Plan data. Processing EgoNet ...");

		// load Data into KML folders for myPerson
		int i=0;
		ArrayList<Person> persons = ((EgoNet)myPerson.getCustomAttributes().get(EgoNet.NAME)).getAlters();

		// more colors than in current egonet to allow for adding agents to egonet without repeating colors
//		nColors=persons.size()*2;

		loadData(ego, 0, iter, network);

		Iterator<Person> altersIt= persons.iterator();

		while(altersIt.hasNext()){
			Person p = altersIt.next();
			i++;
			log.info("CALLING KML FOR EGONET PERSON "+p.getId());
			loadData(p,i, iter, network);

			// Two persons in EgoNet are compared, agent i and agent j>i, to see if they know each other
			// Update ai each iteration
			ai=p;
		}

		log.info(" ... done.");
	}


	public static void loadData(Person alter, int i, int iter, Network network) {

		log.info("    loading Plan data. Processing person ...");
//		TODO make one file per agent and put in the routes and acts each iteration
//		TODO ensure that each agent has a unique color by using the P_ID (?how to quickly find min/max for scaling the colors?)

		Plan myPlan = alter.getSelectedPlan();

//		Color color = setColor(i, nColors+1);
		Byte[] color = new Byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0};
//		Color color = new Color("0","0","0","0");
		if(colors.keySet().contains(alter.getId())){
			color=colors.get(alter.getId());
		}else{
			color = setColor(i, nColors + 1);
			colors.put(alter.getId(), color);
		}

		timeStamp = kmlObjectFactory.createTimeStampType();
		timeStamp.setWhen("1970-01-01T" + Time.writeTime(iter * 3600));
		timeSpan = kmlObjectFactory.createTimeSpanType();
		timeSpan.setBegin("1970-01-01T" + Time.writeTime(iter * 3600));
		timeSpan.setEnd("1970-01-01T" + Time.writeTime(iter * 3600 + 59 * 60 + 59));

		StyleType agentLinkStyle = null;
		boolean styleExists = false;
		ListIterator<JAXBElement<? extends AbstractStyleSelectorType>> stylesIterator = myKMLDocument.getAbstractStyleSelectorGroup().listIterator();
		while(stylesIterator.hasNext() && !styleExists) {

			JAXBElement<? extends AbstractStyleSelectorType> style = stylesIterator.next();
			if (style.getValue().getId().equals("agentLinkStyle"+alter.getId().toString())) {
				styleExists = true;
				agentLinkStyle = (StyleType) style.getValue();
			}

		}
		if (!styleExists) {
			agentLinkStyle = kmlObjectFactory.createStyleType();
			agentLinkStyle.setId("agentLinkStyle"+alter.getId().toString());
			myKMLDocument.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(agentLinkStyle));
		}


		FolderType agentFolder = null;
		boolean featureExists = false;
		ListIterator<JAXBElement<? extends AbstractFeatureType>> featureIterator = myKMLDocument.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !featureExists) {
			JAXBElement<? extends AbstractFeatureType> abstractFeature = featureIterator.next();
			if (abstractFeature.getValue().getId().equals("agent "+myPlan.getPerson().getId().toString())) {
				featureExists = true;
				agentFolder = (FolderType) abstractFeature.getValue();
			}
		}
		if (!featureExists) {
			agentFolder = kmlObjectFactory.createFolderType();
			agentFolder.setId("agent "+myPlan.getPerson().getId().toString());
			agentFolder.setName("agent "+myPlan.getPerson().getId().toString());
			agentFolder.setDescription("Contains one agent");

//			new TimeStamp(new GregorianCalendar(1970, 0, 1, 0, 0, iter)));
			log.info("MAKING NEW KML AGENT FOLDER FOR "+ agentFolder.getId());
			myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(agentFolder));
			agentMap.put(agentFolder.getId(),agentFolder);
		}

		// route folder for each agent and iteration (this is the plan)
		FolderType planFolder = kmlObjectFactory.createFolderType();
		planFolder.setId("plan "+myPlan.getPerson().getId().toString()+"-"+iter);
		planFolder.setName("plan "+myPlan.getPerson().getId().toString()+"-"+iter);
		planFolder.setDescription("Contains one agent's route and its activities in one iteration");
		planFolder.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));
		log.info("MAKING NEW KML PLAN FOLDER "+planFolder.getId()+" FOR AGENT "+agentFolder.getId());
		agentFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(agentFolder));

		// put the activity space polygon into the planFolder
//		makeActivitySpaceKML_Poly(myPerson, iter, planFolder, color);
		makeActivitySpaceKML_Line(alter,iter,planFolder,agentLinkStyle);

		makeCoreSocialLinkKML(alter,iter,planFolder,agentLinkStyle);

		// Proceed to the EgoNet of myPerson
		if(i==0){
			makeEgoSpaceKML_Line(iter, planFolder, color);
		}
		log.info("");
		if(!(ai.equals(ego))){
			if(!(ai.equals(alter))){
				log.info("Making tangential social link in grey");
				makeTangentialSocialLinkKML(alter,iter,planFolder);
			}
		}

		// put facilities in a folder, one facilities folder for each Plan
		FolderType facilitiesFolder = kmlObjectFactory.createFolderType();
		facilitiesFolder.setId("facilities "+myPlan.getPerson().getId().toString()+"-"+iter);
		facilitiesFolder.setName("facilities "+myPlan.getPerson().getId().toString()+"-"+iter);
		facilitiesFolder.setDescription("Contains all the facilities of one agent's plan in one iteration.");
		facilitiesFolder.setVisibility(false);
		facilitiesFolder.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		featureExists = false;
		featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !featureExists) {
			AbstractFeatureType abstractFeature = featureIterator.next().getValue();
			if (abstractFeature.getId().equals(facilitiesFolder.getId())) {
				featureExists = true;
			}
		}
		if (!featureExists) {
			planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(facilitiesFolder));
		}

		Iterator<PlanElement> actLegIter = myPlan.getPlanElements().iterator();
		ActivityImpl act0 = (ActivityImpl) actLegIter.next(); // assume first is always an Activity
		makeActKML(alter, act0, 0, planFolder, agentLinkStyle, iter, network);
		int actNumber=0;
		while(actLegIter.hasNext()) {
			Object o = actLegIter.next();
			if (o instanceof Leg) {
				Leg leg = (Leg) o;

				for (Id routeLinkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					Link routeLink = network.getLinks().get(routeLinkId);
					PlacemarkType agentLinkL = generateLinkPlacemark(routeLink, agentLinkStyle, trafo, iter);

					featureExists = false;
					featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
					while (featureIterator.hasNext() && !featureExists) {
						AbstractFeatureType abstractFeature = featureIterator.next().getValue();
						if (abstractFeature.getId().equals(agentLinkL.getId())) {
							featureExists = true;
						}
					}
					if (!featureExists) {
						planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(agentLinkL));
					}
				}
			} else if (o instanceof Activity) {
				Activity act = (Activity) o;
				actNumber++;
				makeActKML(alter, act,actNumber, planFolder,agentLinkStyle, iter, network);
			}
		}

		// Fill the facilities folder

		for (Object o : myPlan.getPlanElements()) {
			if (o instanceof Activity) {
				Activity myAct = (Activity) o;
	//			Activity myActivity =myPerson.getKnowledge().getMentalMap().getActivity(myAct).getFacility().toString();
				String myActivity=facilities.getFacilities().get(myAct.getFacilityId()).getActivityOptions().get(myAct.getType()).toString();
				//Above lines call code that results in a null pointer. Test
				// michi's new change. Note the Act.setFacility() might not
				// always be kept up-to-date by socialNetowrk code, check this. JH 02-07-2008
				StyleType myStyle=facStyle.get(myAct.getType());
				PlacemarkType aFacility = kmlObjectFactory.createPlacemarkType();
				aFacility.setId(myAct.getType().substring(0, 1));
				aFacility.setDescription(myActivity);
				aFacility.setAddress("address");
				aFacility.setStyleUrl(myStyle.getId());
				aFacility.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));
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

				featureExists = false;
				featureIterator = facilitiesFolder.getAbstractFeatureGroup().listIterator();
				while (featureIterator.hasNext() && !featureExists) {
					AbstractFeatureType abstractFeature = featureIterator.next().getValue();
					if (abstractFeature.getId().equals(aFacility.getId())) {
						featureExists = true;
					}
				}
				if (!featureExists) {
					facilitiesFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(aFacility));
				}
			}
		}

		log.info("    done.");

	}

	private static void makeEgoSpaceKML_Line(int iter, FolderType planFolder, Byte[] color) {

		String id = ego.getId().toString();

		StyleType egoSpaceStyle = kmlObjectFactory.createStyleType();
		egoSpaceStyle.setId("ego space "+id+"-"+iter);

		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		lst.setColor(new byte[]{color[0], color[1], color[2], color[3]});
		lst.setWidth(2.0);
		System.out.println(" # # # # makeEgoSpaceKML_Line "+color.toString());

		FolderType es = kmlObjectFactory.createFolderType();
		es.setId("ego space "+id+"-"+iter);
		es.setName("ego space "+id+"-"+iter);
		es.setDescription("Contains one agent's ego space in one iteration");
		es.setStyleUrl(egoSpaceStyle.getId());
		es.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		boolean folderExists = false;
		ListIterator<JAXBElement<? extends AbstractFeatureType>> featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !folderExists) {
			AbstractFeatureType abstractFeature = featureIterator.next().getValue();
			if (abstractFeature.getId().equals(es.getId())) {
				folderExists = true;
			}
		}
		if (!folderExists) {
			planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(es));
		}

		// make the activity spaces
		new PersonCalcEgoSpace(knowledges, facilities).run(ego);

		// add the points of the activity space to the polygon
		// if PersonCalcEgoSpace() failed to give an activity space, ...
		if((ActivitySpaces.getActivitySpaces(ego).size()>1)){
			ActivitySpace space = ActivitySpaces.getActivitySpaces(ego).get(1);
			if (space instanceof ActivitySpaceEllipse) {

//				calculate the circumference points (boundary)
				double a = space.getParam("a").doubleValue();
				double b = space.getParam("b").doubleValue();
				double theta = space.getParam("theta").doubleValue();
				double x = space.getParam("x").doubleValue();
				double y = space.getParam("y").doubleValue();
				Coord oldCoord=null;
				for (double t=0.0; t<2.0*Math.PI; t=t+2.0*Math.PI/360.0) {
					// "a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0"
					double p_xOut = a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta) + x;
//					double p_xIn = 0.9*(a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta)) + x;
					// "a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0"
					double p_yOut = a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta) + y;
//					double p_yIn = 0.9*(a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta)) + y;
					Coord coordOut = trafo.transform(new CoordImpl(p_xOut, p_yOut));
//					CoordI coordIn = trafo.transform(new Coord(p_xIn, p_yIn));
//					Point pointIn= new Point(coordIn.getX(),coordIn.getY(), 0.0);

					if(t>0.0){

						PlacemarkType linkPlacemark = kmlObjectFactory.createPlacemarkType();
						linkPlacemark.setId(Double.toString(t));
						linkPlacemark.setStyleUrl(egoSpaceStyle.getId());
						linkPlacemark.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

						LineStringType lineString = kmlObjectFactory.createLineStringType();
						lineString.getCoordinates().add(Double.toString(oldCoord.getX()) + "," + Double.toString(oldCoord.getY()) + ",0.0");
						lineString.getCoordinates().add(Double.toString(coordOut.getX()) + "," + Double.toString(coordOut.getY()) + ",0.0");
						linkPlacemark.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lineString));

						es.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(linkPlacemark));

					}
					oldCoord=coordOut;
				}
			}
		}
	}

	private static void makeTangentialSocialLinkKML(Person myPerson, int i,	FolderType folder){

		StyleType tangentialLinkStyle = kmlObjectFactory.createStyleType();
		tangentialLinkStyle.setId("tangentialLinkStyle"+myPerson.getId().toString());

		byte[] color = new byte[]{(byte) 255, (byte) 128, (byte) 128, (byte) 128};
		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		lst.setColor(color);
		lst.setWidth(5.0);
		tangentialLinkStyle.setLineStyle(lst);

		// Add a line from the alter's home to another alter's home in the color grey
		String id = myPerson.getId().toString();

		PlacemarkType socialLink = kmlObjectFactory.createPlacemarkType();
		socialLink.setId(id+ai.getId().toString()+"_"+i);
		socialLink.setName("Tangential social link_"+i);
		socialLink.setStyleUrl(tangentialLinkStyle.getId());
		socialLink.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		LineStringType lineString = kmlObjectFactory.createLineStringType();

		Coord coordFrom = trafo.transform(((ActivityImpl)myPerson.getSelectedPlan().getPlanElements().get(0)).getCoord());
		lineString.getCoordinates().add(Double.toString(coordFrom.getX()) + "," + Double.toString(coordFrom.getY()) + ",0.0");
		Coord coordTo = trafo.transform(((ActivityImpl)ai.getSelectedPlan().getPlanElements().get(0)).getCoord());
		lineString.getCoordinates().add(Double.toString(coordTo.getX()) + "," + Double.toString(coordTo.getY()) + ",0.0");
		socialLink.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lineString));
		folder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(socialLink));
		log.info("Feature added: grey social link from alter "+ai.getId()+" to alter "+myPerson.getId());
	}

	private static void makeCoreSocialLinkKML(Person myPerson, int i,
			FolderType folder, StyleType agentLinkStyle){
		// Add a line from the alter's home to the ego's home in the color of the alter
		String id = myPerson.getId().toString();
		byte[] color = agentLinkStyle.getLineStyle().getColor();
		StyleType socialLinkStyle = kmlObjectFactory.createStyleType();
		socialLinkStyle.setId(id+ego.getId().toString());
		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		lst.setColor(color);
		lst.setWidth(5.0);
		socialLinkStyle.setLineStyle(lst);

		PlacemarkType socialLink = kmlObjectFactory.createPlacemarkType();
		socialLink.setId(id+ego.getId().toString()+"_"+i);
		socialLink.setName("Core social link_"+i);
		socialLink.setStyleUrl(socialLinkStyle.getId());
		socialLink.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		LineStringType lineString = kmlObjectFactory.createLineStringType();
		Coord coordFrom = trafo.transform(((ActivityImpl)myPerson.getSelectedPlan().getPlanElements().get(0)).getCoord());
		lineString.getCoordinates().add(Double.toString(coordFrom.getX()) + "," +Double.toString(coordFrom.getY()) + ",0.0");
		Coord coordTo = trafo.transform(((ActivityImpl)ego.getSelectedPlan().getPlanElements().get(0)).getCoord());
		lineString.getCoordinates().add(Double.toString(coordTo.getX()) + "," +Double.toString(coordTo.getY()) + ",0.0");
		socialLink.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lineString));
		folder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(socialLink));
	}

	private static void makeActivitySpaceKML_Line(Person myPerson, int i,
			FolderType planFolder, StyleType agentLinkStyle) {

		String id = myPerson.getId().toString();

//		String spaceName="P"+id+" activity space Iter= "+i;
		StyleType activitySpaceStyle=null;

		boolean styleExists = false;
		ListIterator<JAXBElement<? extends AbstractStyleSelectorType>> styleIterator = myKMLDocument.getAbstractStyleSelectorGroup().listIterator();
		while (styleIterator.hasNext() && !styleExists) {
			AbstractStyleSelectorType abstractStyle = styleIterator.next().getValue();
			if (abstractStyle.getId().equals("agentLinkStyle"+myPerson.getId().toString())) {
				activitySpaceStyle = (StyleType) abstractStyle;
			}
		}

		byte[] color = agentLinkStyle.getLineStyle().getColor();
		LineStyleType lst = kmlObjectFactory.createLineStyleType();
		lst.setColor(color);
		lst.setWidth(4.0);
		activitySpaceStyle.setLineStyle(lst);

		FolderType as = kmlObjectFactory.createFolderType();
		as.setId("activity space "+id+"-"+i);
		as.setName("activity space "+id+"-"+i);
		as.setDescription("Contains one agent's activity space in one iteration");
		as.setStyleUrl(activitySpaceStyle.getId());
		as.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		boolean featureExists = false;
		ListIterator<JAXBElement<? extends AbstractFeatureType>> featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !featureExists) {
			AbstractFeatureType abstractFeature = featureIterator.next().getValue();
			if (abstractFeature.getId().equals(as.getId())) {
				featureExists = true;
			}
		}
		if (!featureExists) {
			planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(as));
		}


		// make the activity spaces
		// If knowledge already has activity spaces, erase them all and replace them with
		// the new one, to be sure what this activity space corresponds with the plan
		// in the iteration. The activity spaces added to knowledge should be plan (iteration)
		// attributes, not knowledge attributes and it's not certain what iteration each
		// activity space pertains to.
		// For the KMZ animations you can overwrite (erase, replace) the activity space
		// each iteration.

		if(!(ActivitySpaces.getActivitySpaces(myPerson)==null)){
			ActivitySpaces.getActivitySpaces(myPerson).clear();
		}
		new PersonCalcActivitySpace("all", knowledges).run(myPerson);
//		new PersonDrawActivitySpace().run(myPerson);
		// add the points of the activity space to the polygon
		ActivitySpace space = ActivitySpaces.getActivitySpaces(myPerson).get(0);

		if (space instanceof ActivitySpaceEllipse) {

//			calculate the circumference points (boundary)
			double a = space.getParam("a").doubleValue();
			double b = space.getParam("b").doubleValue();
			double theta = space.getParam("theta").doubleValue();
			double x = space.getParam("x").doubleValue();
			double y = space.getParam("y").doubleValue();
			Coord oldCoord=null;
			for (double t=0.0; t<2.0*Math.PI; t=t+2.0*Math.PI/360.0) {
				// "a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0"
				double p_xOut = a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta) + x;
//				double p_xIn = 0.9*(a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta)) + x;
				// "a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0"
				double p_yOut = a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta) + y;
//				double p_yIn = 0.9*(a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta)) + y;
				Coord coordOut = trafo.transform(new CoordImpl(p_xOut, p_yOut));
//				CoordI coordIn = trafo.transform(new Coord(p_xIn, p_yIn));
//				Point pointIn= new Point(coordIn.getX(),coordIn.getY(), 0.0);

				if(t>0.0){

					PlacemarkType linkPlacemark = kmlObjectFactory.createPlacemarkType();
					linkPlacemark.setId(Double.toString(t));
					linkPlacemark.setStyleUrl(activitySpaceStyle.getId());
					linkPlacemark.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

					LineStringType lstrt = kmlObjectFactory.createLineStringType();
					lstrt.getCoordinates().add(Double.toString(oldCoord.getX()) + "," + Double.toString(oldCoord.getY()) + ",0.0");
					lstrt.getCoordinates().add(Double.toString(coordOut.getX()) + "," + Double.toString(coordOut.getY()) + ",0.0");
					linkPlacemark.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lstrt));
					as.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(linkPlacemark));
				}
				oldCoord=coordOut;
			}
		}
	}

//	private static void makeActivitySpaceKML_Poly(Person myPerson, int i,
//			Folder planFolder, Color color) {
//		// TODO
//		// define polygon style
//		String id = myPerson.getId().toString();
//		LineStyle spaceLineStyle = new LineStyle(color, LineStyle.DEFAULT_COLOR_MODE, LineStyle.DEFAULT_WIDTH);
//		PolyStyle spacePolyStyle = new PolyStyle(color,PolyStyle.DEFAULT_COLOR_MODE, false, true);
//		Style spaceStyle = new Style(id);
//		spaceStyle.setLineStyle(spaceLineStyle);
//		spaceStyle.setPolyStyle(spacePolyStyle);
//		String spaceName="activity space";
//
//		// make the activity spaces
//		new PersonCalcActivitySpace("all").run(myPerson);
////		new PersonDrawActivitySpace().run(myPerson);
//		// add the points of the activity space to the polygon
//		ActivitySpace space = myPerson.getKnowledge().getActivitySpaces().get(0);
//
//		List<Point> boundaryOut = new ArrayList<Point>();
//		List<Point> boundaryIn = new ArrayList<Point>();
//		if (space instanceof ActivitySpaceEllipse) {
//			spaceName = myPerson.getId()+"-ellipse-"+i;
//
////			calculate the circumference points (boundary)
//			double a = space.getParam("a").doubleValue();
//			double b = space.getParam("b").doubleValue();
//			double theta = space.getParam("theta").doubleValue();
//			double x = space.getParam("x").doubleValue();
//			double y = space.getParam("y").doubleValue();
//			for (double t=0.0; t<2.0*Math.PI; t=t+2.0*Math.PI/360.0) {
//				// "a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0"
//				double p_xOut = a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta) + x;
//				double p_xIn = 0.9*(a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta)) + x;
//				// "a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0"
//				double p_yOut = a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta) + y;
//				double p_yIn = 0.9*(a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta)) + y;
//				Coord coordOut = trafo.transform(new CoordImpl(p_xOut, p_yOut));
//				Point pointOut= new Point(coordOut.getX(),coordOut.getY(), 0.0);
//				Coord coordIn = trafo.transform(new CoordImpl(p_xIn, p_yIn));
//				Point pointIn= new Point(coordIn.getX(),coordIn.getY(), 0.0);
//				boundaryOut.add(pointOut);
////				boundaryIn.add(pointIn);
//			}
//		}
//
//		// define the polygon feature
//
//		Placemark pl = new Placemark(
//				spaceName,
//				spaceName,
//				spaceName+": test activity space",
//				Feature.DEFAULT_ADDRESS,
//				Feature.DEFAULT_LOOK_AT,
//				spaceStyle.getId(),
//				Feature.DEFAULT_VISIBILITY,
//				Feature.DEFAULT_REGION,
//				timeSpan);
//
//		// make the polygon feature
//		Polygon polygon = new Polygon(true,true,Geometry.DEFAULT_ALTITUDE_MODE);
//		polygon.setBoundary(new LinearRing(boundaryOut));
//		polygon.addInnerBoundary(new LinearRing(boundaryIn));
//		// add the polygon feature
//		pl.setGeometry(polygon);
//		if(!planFolder.containsFeature(pl.getId())){
//			planFolder.addFeature(pl);
//		}
//
//	}

	private static void makeActKML(Person myPerson, Activity act, int actNo, FolderType planFolder, StyleType agentLinkStyle, int iter, Network network) {

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
//				log.info(fullActName);
			}
			break;
		case 's':
			styleUrl = shopStyle.getId();
			fullActName = "shop"+myPerson.getId()+"-"+actNo;
			break;
		case 'l':
			styleUrl = leisureStyle.getId();
			fullActName = "leisure"+myPerson.getId();
			break;
		case 'w':
			styleUrl = workStyle.getId();
			fullActName = "work"+myPerson.getId()+"-"+actNo;
			break;
		case 'e':
			styleUrl = educStyle.getId();
			fullActName = "education"+myPerson.getId()+"-"+actNo;
			break;
		}

		actEndTime = act.getEndTime();
		if (actEndTime == Time.UNDEFINED_TIME) {
			actEndTime = 24.0 * 60 * 60;
		}

		PlacemarkType pl = kmlObjectFactory.createPlacemarkType();
		pl.setId(fullActName);
		pl.setDescription(fullActName+": "+Time.writeTime(act.getStartTime()) + " - " + Time.writeTime(actEndTime));
		pl.setStyleUrl(styleUrl);
		pl.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		Coord geometryCoord = trafo.transform(new CoordImpl(act.getCoord().getX(), act.getCoord().getY()));
		PointType actPoint = kmlObjectFactory.createPointType();
		actPoint.getCoordinates().add(Double.toString(geometryCoord.getX()) + "," + Double.toString(geometryCoord.getY()) + ",0.0");
		pl.setAbstractGeometryGroup(kmlObjectFactory.createPoint(actPoint));

		boolean featureExists = false;
		ListIterator<JAXBElement<? extends AbstractFeatureType>> featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !featureExists) {
			AbstractFeatureType abstractFeature = featureIterator.next().getValue();
			if (abstractFeature.getId().equals(pl.getId())) {
				featureExists = true;
			}
		}
		if (!featureExists) {
			planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(pl));
		}

//		if (!fullActName.equals("evening home")) {
		Link actLink = network.getLinks().get(act.getLinkId());
		PlacemarkType agentLink = generateLinkPlacemark(actLink, agentLinkStyle, trafo);

		featureExists = false;
		featureIterator = planFolder.getAbstractFeatureGroup().listIterator();
		while (featureIterator.hasNext() && !featureExists) {
			AbstractFeatureType abstractFeature = featureIterator.next().getValue();
			if (abstractFeature.getId().equals(agentLink.getId())) {
				featureExists = true;
			}
		}
		if (!featureExists) {
			planFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(agentLink));
		}
//		}

	}

//	private static void setFacStyles(Color color) {
//
//		// Generates facility styles specific to agent
//
//		double labelScale = 1.0;
//		workStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/W.png")));
//		workStyle.setLabelStyle(
//				new LabelStyle(
//						color,
//						ColorStyle.DEFAULT_COLOR_MODE,
//						labelScale));
//		leisureStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/L.png")));
//		leisureStyle.setLabelStyle(
//				new LabelStyle(
//						color,
//						ColorStyle.DEFAULT_COLOR_MODE,
//						labelScale));
//		educStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/E.png")));
//		educStyle.setLabelStyle(
//				new LabelStyle(
//						color,
//						ColorStyle.DEFAULT_COLOR_MODE,
//						labelScale));
//		shopStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/S.png")));
//		shopStyle.setLabelStyle(
//				new LabelStyle(
//						color,
//						ColorStyle.DEFAULT_COLOR_MODE,
//						labelScale));
//		homeStyle.setIconStyle(new IconStyle(new Icon("http://maps.google.com/mapfiles/kml/paddle/H.png")));
//		homeStyle.setLabelStyle(
//				new LabelStyle(
//						color,
//						ColorStyle.DEFAULT_COLOR_MODE,
//						labelScale));
//	}

	private static Byte[] setColor(int i, int intervals) {

		// returns a color as a function of integer i
		byte alpha = (byte) 255;

		byte r = (byte)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) + 1));
//		log.info(r);
		byte g = (byte)(127.0 * (Math.cos((i * 2 * Math.PI) / intervals) + 1));
//		log.info(g);
		byte b = (byte)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) * (-1) + 1));
//		log.info(b);

		Byte[] color = new Byte[]{alpha, b, g, r};
		return color;
	}

	public static void write() {

		if(config.getModule(KML21_MODULE)==null) return;
		log.info("    writing KML files out...");

//		KMLWriter myKMLDocumentWriter;
		KMZWriter myKMZDocumentWriter;
		myKMZDocumentWriter = new KMZWriter(mainKMLFilename);
//		myKMLDocumentWriter = new KMLWriter(myKML, mainKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);

		try {
			//add the matsim logo to the kml
			myKMLDocument.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(MatsimKMLLogo.writeMatsimKMLLogo(myKMZDocumentWriter)));
//			networkFolder.addFeature(new MatsimKMLLogo(myKMZDocumentWriter));
		} catch (IOException e) {
			log.error("Cannot add logo to the KMZ file.", e);
		}

		myKMZDocumentWriter.writeMainKml(myKML);
		myKMZDocumentWriter.close();
//		myKMLDocumentWriter.write();
//		myKMLDocumentWriter = new KMLWriter(coloredLinkKML, coloredLinkKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);


		log.info("    done.");

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

	private static PlacemarkType generateLinkPlacemark(Link link, StyleType style, CoordinateTransformation trafo, int iter) {

		PlacemarkType linkPlacemark = kmlObjectFactory.createPlacemarkType();
		linkPlacemark.setId("link" + link.getId());
		linkPlacemark.setStyleUrl(style.getId());
		linkPlacemark.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));

		LineStringType lineString = kmlObjectFactory.createLineStringType();

		Node fromNode = link.getFromNode();
		Coord fromNodeWorldCoord = fromNode.getCoord();
		Coord fromNodeGeometryCoord = trafo.transform(new CoordImpl(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		Node toNode = link.getToNode();
		Coord toNodeWorldCoord = toNode.getCoord();
		Coord toNodeGeometryCoord = trafo.transform(new CoordImpl(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));

		lineString.getCoordinates().add(Double.toString(fromNodeGeometryCoord.getX()) + "," + Double.toString(fromNodeGeometryCoord.getY()) + ",0.0");
		lineString.getCoordinates().add(Double.toString(toNodeGeometryCoord.getX()) + "," + Double.toString(toNodeGeometryCoord.getY()) + ",0.0");
		linkPlacemark.setAbstractGeometryGroup(kmlObjectFactory.createLineString(lineString));

		return linkPlacemark;
	}
}

