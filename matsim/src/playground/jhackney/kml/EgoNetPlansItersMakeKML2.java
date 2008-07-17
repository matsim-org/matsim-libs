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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.ActivitySpace;
import org.matsim.plans.ActivitySpaceEllipse;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonCalcActivitySpace;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Geometry;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.LabelStyle;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.LinearRing;
import org.matsim.utils.vis.kml.LookAt;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.PolyStyle;
import org.matsim.utils.vis.kml.Polygon;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.TimeStamp;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;

import playground.jhackney.algorithms.PersonDrawActivitySpace;

public class EgoNetPlansItersMakeKML2 {

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

	private static KML myKML;
//	, coloredLinkKML;
	private static Document myKMLDocument;
//	, coloredLinkKMLDocument;

	private static Folder networkFolder=null;

	private static Style workStyle, leisureStyle,
	educStyle, shopStyle, homeStyle;//, agentLinkStyle;
	private static HashMap<String,Style> facStyle= new HashMap<String,Style>();
	private static CoordinateTransformationI trafo;
	private static Config config = null;
	private static final Logger log = Logger.getLogger(EgoNetPlansItersMakeKML.class);

	private static TreeMap<String,Folder> agentMap = new TreeMap<String,Folder>();
	private static Person ego;
	private static TreeMap<Id,Color> colors = new TreeMap<Id,Color>();
	private static TimeStamp timeStamp;
	private static int nColors;
	private static Person ai;

	public static void setUp(Config config, NetworkLayer network) {
		EgoNetPlansItersMakeKML2.config=config;
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

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);

//		coloredLinkKML = new KML();
//		coloredLinkKMLDocument = new Document("network main feature");
//		coloredLinkKML.setFeature(coloredLinkKMLDocument);

		log.info("    done.");

		///////////////////////////
		// display road network
		///////////////////////////
		Style linkStyle = new Style("defaultLinkStyle");
		myKMLDocument.addStyle(linkStyle);
		linkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "00", "00"), ColorStyle.DEFAULT_COLOR_MODE, 1));

		networkFolder = new Folder(
				"network used",
				"network used",
				network.getName(),
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

		log.info("    generating styles...");

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

		log.info("    done.");
		
		nColors=36;
		log.info("Setting nColors = "+ nColors);
	}


	public static void loadData(Person myPerson, int iter){

		ego=myPerson;
		ai=myPerson;
		if(config.getModule(KML21_MODULE)==null) return;

		log.info("    loading Plan data. Processing EgoNet ...");

		// load Data into KML folders for myPerson
		int i=0;
		ArrayList<Person> persons = myPerson.getKnowledge().getEgoNet().getAlters();
		
		// more colors than in current egonet to allow for adding agents to egonet without repeating colors
//		nColors=persons.size()*2;
		
		loadData(ego, 0, iter);

		// Proceed to the EgoNet of myPerson
		
		Iterator<Person> altersIt= persons.iterator();

		while(altersIt.hasNext()){
			Person p = altersIt.next();
			i++;
			log.info("CALLING KML FOR EGONET PERSON "+p.getId());
			loadData(p,i, iter);

			// Two persons in EgoNet are compared, agent i and agent j>i, to see if they know each other
			// Update ai each iteration
			ai=p;
		}

		log.info(" ... done.");
	}
	public static void loadData(Person alter, int i, int iter) {

		log.info("    loading Plan data. Processing person ...");
//		TODO make one file per agent and put in the routes and acts each iteration
//		TODO ensure that each agent has a unique color by using the P_ID (?how to quickly find min/max for scaling the colors?)

		Plan myPlan = alter.getSelectedPlan();

//		Color color = setColor(i, nColors+1);
		Color color = new Color("0","0","0","0");
		if(colors.keySet().contains(alter.getId())){
			color=colors.get(alter.getId());
		}else{
			color = setColor(i, nColors + 1);
			colors.put(alter.getId(), color);
		}
		
		timeStamp = new TimeStamp(new GregorianCalendar(1970, 0, 1, iter, 0, 0));


		Style agentLinkStyle =null;
		if(!myKMLDocument.containsStyle("agentLinkStyle"+alter.getId().toString())){
			agentLinkStyle = new Style("agentLinkStyle"+alter.getId().toString());
			agentLinkStyle.setLineStyle(new LineStyle(color, ColorStyle.DEFAULT_COLOR_MODE, 14));
			myKMLDocument.addStyle(agentLinkStyle);
		}else// Set a constant color for each agent for all iterations
			agentLinkStyle=myKMLDocument.getStyle("agentLinkStyle"+alter.getId().toString());


		Folder agentFolder;
		if(!myKMLDocument.containsFeature("agent "+myPlan.getPerson().getId().toString())){
			agentFolder = new Folder(
					"agent "+myPlan.getPerson().getId().toString(),
					"agent "+myPlan.getPerson().getId().toString(),
					"Contains one agent",
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					Feature.DEFAULT_STYLE_URL,
					true,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);
//					new TimeStamp(new GregorianCalendar(1970, 0, 1, 0, 0, iter)));


			log.info("MAKING NEW KML AGENT FOLDER FOR "+ agentFolder.getId());
			myKMLDocument.addFeature(agentFolder);
			agentMap.put(agentFolder.getId(),agentFolder);
		}else{
			agentFolder =agentMap.get("agent "+myPlan.getPerson().getId().toString());
		}

		// route folder for each agent and iteration (this is the plan)
		Folder planFolder = new Folder(
				"plan "+myPlan.getPerson().getId().toString()+"-"+iter,
				"plan "+myPlan.getPerson().getId().toString()+"-"+iter,
				"Contains one agent's route and its activities in one iteration",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				true,
				Feature.DEFAULT_REGION,
				timeStamp);
		log.info("MAKING NEW KML PLAN FOLDER "+planFolder.getId()+" FOR AGENT "+agentFolder.getId());
		agentFolder.addFeature(planFolder);

		// put the activity space polygon into the planFolder
//		makeActivitySpaceKML_Poly(myPerson, iter, planFolder, color);
		makeActivitySpaceKML_Line(alter,iter,planFolder,agentLinkStyle);

		makeCoreSocialLinkKML(alter,iter,planFolder,agentLinkStyle);
		log.info("");
		if(!(ai.equals(ego))){
			if(!(ai.equals(alter))){
			log.info("Making tangential social link in grey");
		makeTangentialSocialLinkKML(alter,iter,planFolder);
		}
		}

		// put facilities in a folder, one facilities folder for each Plan
		Folder facilitiesFolder = new Folder(
				"facilities "+myPlan.getPerson().getId().toString()+"-"+iter,
				"facilities "+myPlan.getPerson().getId().toString()+"-"+iter,
				"Contains all the facilities of one agent's plan in one iteration.",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				timeStamp);

		if(!planFolder.containsFeature(facilitiesFolder.getId())){
			planFolder.addFeature(facilitiesFolder);
		}

		ActLegIterator actLegIter = myPlan.getIterator();
		Act act0 = (Act) actLegIter.nextAct();
		makeActKML(alter, act0, 0, planFolder, agentLinkStyle, iter);
		int actNumber=0;
		while(actLegIter.hasNextLeg()){//alternates Act-Leg-Act-Leg and ends with Act

			Leg leg = (Leg) actLegIter.nextLeg();

			Link[] routeLinks = (leg).getRoute().getLinkRoute();
			for (Link routeLink : routeLinks) {
				Placemark agentLinkL = generateLinkPlacemark(routeLink, agentLinkStyle, trafo, iter);
				if(!planFolder.containsFeature(agentLinkL.getId())){
					planFolder.addFeature(agentLinkL);
				}
			}
			Act act = (Act) actLegIter.nextAct();
			actNumber++;
			makeActKML(alter, act,actNumber, planFolder,agentLinkStyle, iter);
		}



		// Fill the facilities folder

		ActIterator aIter = myPlan.getIteratorAct();
		while(aIter.hasNext()){
			Act myAct = (Act) aIter.next();
//			Activity myActivity =myPerson.getKnowledge().getMentalMap().getActivity(myAct).getFacility().toString();
			String myActivity=myAct.getFacility().getActivity(myAct.getType()).toString();
			//Above lines call code that results in a null pointer. Test
			// michi's new change. Note the Act.setFacility() might not
			// always be kept up-to-date by socialNetowrk code, check this. JH 02-07-2008
			Style myStyle=facStyle.get(myAct.getType());
			Placemark aFacility = new Placemark(
					myAct.getType().substring(0, 1),
					null,
					myActivity,
					"address",
					Feature.DEFAULT_LOOK_AT,
					myStyle.getStyleUrl(),
					true,
					Feature.DEFAULT_REGION,
					timeStamp);
			if(!facilitiesFolder.containsFeature(aFacility.getId())){
				facilitiesFolder.addFeature(aFacility);
			}

			// Get the coordinates of the facility associated with the Act and transform
			// to WGS84 for GoogleEarth

			CoordI geometryCoord = trafo.transform(myAct.getCoord());
			Point myPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
			aFacility.setGeometry(myPoint);
			aFacility.setLookAt(new LookAt(geometryCoord.getX(),geometryCoord.getY()));
		}

		log.info("    done.");

	}

	private static void makeTangentialSocialLinkKML(Person myPerson, int i,
			Folder folder){
		
		
		Style tangentialLinkStyle=new Style("tangentialLinkStyle"+myPerson.getId().toString());
		Color color = new Color(255,128,128,128);
		tangentialLinkStyle.setLineStyle(new LineStyle(color, ColorStyle.DEFAULT_COLOR_MODE, 14));
		// Add a line from the alter's home to another alter's home in the color grey
		String id = myPerson.getId().toString();
		Placemark socialLink =  new Placemark(
				id+ai.getId().toString()+"_"+i,
				"Tangential social link_"+i,
				Feature.DEFAULT_DESCRIPTION,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				tangentialLinkStyle.getId(),
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				timeStamp);

		CoordI coordFrom = trafo.transform((Coord)((Act)myPerson.getSelectedPlan().getActsLegs().get(0)).getCoord());
		Point pointFrom= new Point(coordFrom.getX(),coordFrom.getY(), 0.0);
		CoordI coordTo = trafo.transform((Coord)((Act)ai.getSelectedPlan().getActsLegs().get(0)).getCoord());
		Point pointTo= new Point(coordTo.getX(),coordTo.getY(), 0.0);
		socialLink.setGeometry(new LineString(pointFrom, pointTo));
		folder.addFeature(socialLink);
		log.info("Feature added: grey social link from alter "+ai.getId()+" to alter "+myPerson.getId());
	}
	
	private static void makeCoreSocialLinkKML(Person myPerson, int i,
			Folder folder, Style agentLinkStyle){
		// Add a line from the alter's home to the ego's home in the color of the alter
		String id = myPerson.getId().toString();
		Placemark socialLink =  new Placemark(
				id+ego.getId().toString()+"_"+i,
				"Core social link_"+i,
				Feature.DEFAULT_DESCRIPTION,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				agentLinkStyle.getId(),
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				timeStamp);

		CoordI coordFrom = trafo.transform((Coord)((Act)myPerson.getSelectedPlan().getActsLegs().get(0)).getCoord());
		Point pointFrom= new Point(coordFrom.getX(),coordFrom.getY(), 0.0);
		CoordI coordTo = trafo.transform((Coord)((Act)ego.getSelectedPlan().getActsLegs().get(0)).getCoord());
		Point pointTo= new Point(coordTo.getX(),coordTo.getY(), 0.0);
		socialLink.setGeometry(new LineString(pointFrom, pointTo));
		folder.addFeature(socialLink);
	}

	private static void makeActivitySpaceKML_Line(Person myPerson, int i,
			Folder planFolder, Style agentLinkStyle) {

		String id = myPerson.getId().toString();

//		String spaceName="P"+id+" activity space Iter= "+i;
		Style activitySpaceStyle=null;
		activitySpaceStyle=myKMLDocument.getStyle("agentLinkStyle"+myPerson.getId().toString());
		Color color = agentLinkStyle.getLineStyle().getColor();
		activitySpaceStyle.setLineStyle(new LineStyle(color, ColorStyle.DEFAULT_COLOR_MODE, 4));


		Folder as = new Folder(
				"activity space "+id+"-"+i,
				"activity space "+id+"-"+i,
				"Contains one agent's activity space in one iteration",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				true,
				Feature.DEFAULT_REGION,
				timeStamp);

		if(!planFolder.containsFeature(as.getId())){
			planFolder.addFeature(as);
		}	


		// make the activity spaces
		new PersonCalcActivitySpace("all").run(myPerson);
//		new PersonDrawActivitySpace().run(myPerson);
		// add the points of the activity space to the polygon
		ActivitySpace space = myPerson.getKnowledge().getActivitySpaces().get(0);
		if (space instanceof ActivitySpaceEllipse) {

//			calculate the circumference points (boundary)
			double a = space.getParam("a").doubleValue();
			double b = space.getParam("b").doubleValue();
			double theta = space.getParam("theta").doubleValue();
			double x = space.getParam("x").doubleValue();
			double y = space.getParam("y").doubleValue();
			Point oldPoint=null;
			for (double t=0.0; t<2.0*Math.PI; t=t+2.0*Math.PI/360.0) {
				// "a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0"
				double p_xOut = a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta) + x;
//				double p_xIn = 0.9*(a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta)) + x;
				// "a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0"
				double p_yOut = a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta) + y;
//				double p_yIn = 0.9*(a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta)) + y;
				CoordI coordOut = trafo.transform(new Coord(p_xOut, p_yOut));
				Point pointOut= new Point(coordOut.getX(),coordOut.getY(), 0.0);
//				CoordI coordIn = trafo.transform(new Coord(p_xIn, p_yIn));
//				Point pointIn= new Point(coordIn.getX(),coordIn.getY(), 0.0);

				if(t>0.0){

					Placemark linkPlacemark = new Placemark(
							Double.toString(t),
							Feature.DEFAULT_NAME,
							Feature.DEFAULT_DESCRIPTION,
							Feature.DEFAULT_ADDRESS,
							Feature.DEFAULT_LOOK_AT,
							agentLinkStyle.getId(),
							Feature.DEFAULT_VISIBILITY,
							Feature.DEFAULT_REGION,
							timeStamp);

					linkPlacemark.setGeometry(new LineString(oldPoint, pointOut));
					as.addFeature(linkPlacemark);
				}
				oldPoint=pointOut;
			}
		}	
	}

	private static void makeActivitySpaceKML_Poly(Person myPerson, int i,
			Folder planFolder, Color color) {
		// TODO
		// define polygon style
		String id = myPerson.getId().toString();
		LineStyle spaceLineStyle = new LineStyle(color, LineStyle.DEFAULT_COLOR_MODE, LineStyle.DEFAULT_WIDTH);
		PolyStyle spacePolyStyle = new PolyStyle(color,PolyStyle.DEFAULT_COLOR_MODE, false, true);
		Style spaceStyle = new Style(id);
		spaceStyle.setLineStyle(spaceLineStyle);
		spaceStyle.setPolyStyle(spacePolyStyle);
		String spaceName="activity space";

		// make the activity spaces
		new PersonCalcActivitySpace("all").run(myPerson);
		new PersonDrawActivitySpace().run(myPerson);
		// add the points of the activity space to the polygon
		ActivitySpace space = myPerson.getKnowledge().getActivitySpaces().get(0);

		List<Point> boundaryOut = new ArrayList<Point>();
		List<Point> boundaryIn = new ArrayList<Point>();
		if (space instanceof ActivitySpaceEllipse) {
			spaceName = myPerson.getId()+"-ellipse-"+i;

//			calculate the circumference points (boundary)
			double a = space.getParam("a").doubleValue();
			double b = space.getParam("b").doubleValue();
			double theta = space.getParam("theta").doubleValue();
			double x = space.getParam("x").doubleValue();
			double y = space.getParam("y").doubleValue();
			for (double t=0.0; t<2.0*Math.PI; t=t+2.0*Math.PI/360.0) {
				// "a*((cos(t)))*cos(phi) - b*((sin(t)))*sin(phi) + x0"
				double p_xOut = a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta) + x;
				double p_xIn = 0.9*(a*Math.cos(t)*Math.cos(theta) - b*Math.sin(t)*Math.sin(theta)) + x;
				// "a*((cos(t)))*sin(phi) + b*((sin(t)))*cos(phi) + y0"
				double p_yOut = a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta) + y;
				double p_yIn = 0.9*(a*Math.cos(t)*Math.sin(theta) + b*Math.sin(t)*Math.cos(theta)) + y;
				CoordI coordOut = trafo.transform(new Coord(p_xOut, p_yOut));
				Point pointOut= new Point(coordOut.getX(),coordOut.getY(), 0.0);
				CoordI coordIn = trafo.transform(new Coord(p_xIn, p_yIn));
				Point pointIn= new Point(coordIn.getX(),coordIn.getY(), 0.0);
				boundaryOut.add(pointOut);
//				boundaryIn.add(pointIn);
			}
		}		

		// define the polygon feature

		Placemark pl = new Placemark(
				spaceName,
				spaceName,
				spaceName+": test activity space",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				spaceStyle.getId(),
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				timeStamp);

		// make the polygon feature
		Polygon polygon = new Polygon(true,true,Geometry.DEFAULT_ALTITUDE_MODE);
		polygon.setBoundary(new LinearRing(boundaryOut));
		polygon.addInnerBoundary(new LinearRing(boundaryIn));
		// add the polygon feature
		pl.setGeometry(polygon);
		if(!planFolder.containsFeature(pl.getId())){
			planFolder.addFeature(pl);
		}

	}

	private static void makeActKML(Person myPerson, Act act, int actNo, Folder planFolder, Style agentLinkStyle, int iter) {
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
//				log.info(fullActName);
			}
			break;
		case 's':
			styleUrl = shopStyle.getStyleUrl();
			fullActName = "shop"+myPerson.getId()+"-"+actNo;
			break;
		case 'l':
			styleUrl = leisureStyle.getStyleUrl();
			fullActName = "leisure"+myPerson.getId();
			break;
		case 'w':
			styleUrl = workStyle.getStyleUrl();
			fullActName = "work"+myPerson.getId()+"-"+actNo;
			break;
		case 'e':
			styleUrl = educStyle.getStyleUrl();
			fullActName = "education"+myPerson.getId()+"-"+actNo;
			break;
		}

		actEndTime = act.getEndTime();
		if (actEndTime == Time.UNDEFINED_TIME) {
			actEndTime = 24.0 * 60 * 60;
		}


		Placemark pl = new Placemark(
				fullActName,
				null,
				fullActName+": "+Time.writeTime(act.getStartTime()) + " - " + Time.writeTime(actEndTime),
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				styleUrl,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				timeStamp);
		if(!planFolder.containsFeature(pl.getId())){
			planFolder.addFeature(pl);
		}

		CoordI geometryCoord = trafo.transform(new Coord(act.getCoord().getX(), act.getCoord().getY()));
		Point actPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
		pl.setGeometry(actPoint);

//		if (!fullActName.equals("evening home")) {
		Link actLink = act.getLink();
		Placemark agentLink = generateLinkPlacemark(actLink, agentLinkStyle, trafo);
		if(!planFolder.containsFeature(agentLink.getId())){
			planFolder.addFeature(agentLink);
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
//		log.info(r);
		int g = (int)(127.0 * (Math.cos((i * 2 * Math.PI) / intervals) + 1));
//		log.info(g);
		int b = (int)(127.0 * (Math.sin((i * 2 * Math.PI) / intervals) * (-1) + 1));
//		log.info(b);

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
		log.info("    writing KML files out...");

//		KMLWriter myKMLDocumentWriter;
		KMZWriter myKMZDocumentWriter;
		myKMZDocumentWriter = new KMZWriter(mainKMLFilename);
//		myKMLDocumentWriter = new KMLWriter(myKML, mainKMLFilename, KMLWriter.DEFAULT_XMLNS, useCompression);

		try {
			//add the matsim logo to the kml
			myKMLDocument.addFeature(new MatsimKMLLogo(myKMZDocumentWriter));
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

	private static Placemark generateLinkPlacemark(Link link, Style style, CoordinateTransformationI trafo) {

		Placemark linkPlacemark = null;

		Node fromNode = link.getFromNode();
		org.matsim.utils.geometry.shared.Coord fromNodeWorldCoord = fromNode.getCoord();
		org.matsim.utils.geometry.shared.Coord fromNodeGeometryCoord = (Coord) trafo.transform(new Coord(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		Point fromPoint = new Point(fromNodeGeometryCoord.getX(), fromNodeGeometryCoord.getY(), 0.0);

		Node toNode = link.getToNode();
		org.matsim.utils.geometry.shared.Coord toNodeWorldCoord = toNode.getCoord();
		org.matsim.utils.geometry.shared.Coord toNodeGeometryCoord = (Coord) trafo.transform(new Coord(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));
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
	private static Placemark generateLinkPlacemark(Link link, Style style, CoordinateTransformationI trafo, int iter) {

		Placemark linkPlacemark = null;

		Node fromNode = link.getFromNode();
		org.matsim.utils.geometry.shared.Coord fromNodeWorldCoord = fromNode.getCoord();
		org.matsim.utils.geometry.shared.Coord fromNodeGeometryCoord = (Coord) trafo.transform(new Coord(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		Point fromPoint = new Point(fromNodeGeometryCoord.getX(), fromNodeGeometryCoord.getY(), 0.0);

		Node toNode = link.getToNode();
		org.matsim.utils.geometry.shared.Coord toNodeWorldCoord = toNode.getCoord();
		org.matsim.utils.geometry.shared.Coord toNodeGeometryCoord = (Coord) trafo.transform(new Coord(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));
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
				timeStamp);

		linkPlacemark.setGeometry(new LineString(fromPoint, toPoint));

		return linkPlacemark;
	}
}

