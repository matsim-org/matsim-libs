/* *********************************************************************** *
 * project: org.matsim.*
 * MyRuns.java
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

package playground.meisterk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesProductionKTI;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.planomat.PlanomatFitnessFunctionWrapper;
import org.matsim.planomat.PlanomatStrategyManagerConfigLoader;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator;
import org.matsim.plans.Act;
import org.matsim.plans.ActUtilityParameters;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithmI;
import org.matsim.plans.algorithms.PersonAnalyseTimesByActivityType;
import org.matsim.plans.algorithms.PersonAnalyseTimesByActivityType.Activities;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.PlanomatOptimizeTimes;
import org.matsim.replanning.modules.StrategyModuleI;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.util.TravelTimeI;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.LabelStyle;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.world.Location;

public class MyRuns {

	public final static String CONFIG_MODULE = "planCalcScore";
	public final static String CONFIG_WAITING = "waiting";
	public final static String CONFIG_LATE_ARRIVAL = "lateArrival";
	public final static String CONFIG_EARLY_DEPARTURE = "earlyDeparture";
	public final static String CONFIG_TRAVELING = "traveling";
	public final static String CONFIG_PERFORMING = "performing";
	public final static String CONFIG_LEARNINGRATE = "learningRate";
	public final static String CONFIG_DISTANCE_COST = "distanceCost";

	public static final int TIME_BIN_SIZE = 300;

	protected static final TreeMap<String, ActUtilityParameters> utilParams = new TreeMap<String, ActUtilityParameters>();
	protected static double marginalUtilityOfWaiting = Double.NaN;
	protected static double marginalUtilityOfLateArrival = Double.NaN;
	protected static double marginalUtilityOfEarlyDeparture = Double.NaN;
	protected static double marginalUtilityOfTraveling = Double.NaN;
	protected static double marginalUtilityOfPerforming = Double.NaN;
	protected static double distanceCost = Double.NaN;
	protected static double abortedPlanScore = Double.NaN;
	protected static double learningRate = Double.NaN;

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

//		MyRuns.writeGUESSFile();
//		MyRuns.planomatStandAloneDemo();
//		MyRuns.testReplanningRate();
//		MyRuns.testDepartureDelayCalculator();
//		MyRuns.testTravelTimeEstimator();
//		MyRuns.testTravelTimeI();
//		MyRuns.testCharyparNagelFitnessFunction();
//		MyRuns.conversionSpeedTest();
//		MyRuns.convertPlansV0ToPlansV4();
//		MyRuns.produceSTRC2007KML();
		MyRuns.facilityActivities();

		System.out.println();

	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {

		Gbl.createConfig(args);
//		Gbl.createWorld();
//		Gbl.createFacilities();

		run();

	}

	private static void testReplanningRate() {

		for (int iter=0; iter<=200; iter++) {
//			System.out.println(iter + " : " + PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, 100, 0.1));
			System.out.println(PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, 100, 0.1));

			System.out.println(iter + " : " + PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, 100, 0.1));
		}

	}

	private static void facilityActivities() {

		String actType = "s13";
		String shopId = "";
		
		System.out.println("Reading facilities...");
		Facilities facilityLayer = new Facilities();
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(facilityLayer);
		//facilities_reader.setValidating(false);
		facilities_reader.readFile(Gbl.getConfig().facilities().getInputFile());
		facilityLayer.printFacilitiesCount();
		Gbl.getWorld().setFacilityLayer(facilityLayer);
		System.out.println("Reading facilities...done.");

		// does every facility activity have a wednesday opentime?
		TreeSet<String> wednesdayLessActivityTypes = new TreeSet<String>();

		// IGNORE HOME!!!
		// tta -> wkday
		TreeSet<Opentime> opentimes = null;
		TreeMap<String, TreeSet<Opentime>> closestShopOpentimes = new TreeMap<String, TreeSet<Opentime>>();
		
		for (Facility f : facilityLayer.getFacilities().values()) {

//			Activity shopActivity = f.getActivity("shop");
//			if (shopActivity == null) {
//				shopActivity = f.createActivity("shop");
//				
//				if (f.getId().toString().startsWith("Denner")) {
//					shopId = "Denner____8050_Zürich_REGENSBERGSTR. 309";
//				} else if (f.getId().toString().startsWith("Migros")) {
//					shopId = "Migros__Zürich-Affoltern-ZH_Migros Zürich_8046_Zürich_Jonas-Furrerstrasse 21";
//				}
//				
//				closestShopOpentimes = ((Facility) facilityLayer.getLocation(shopId)).getActivity("shop").getOpentimes();
//				shopActivity.setOpentimes(closestShopOpentimes);
//			}
			
			for (Activity a : f.getActivities().values()) {

				if (!a.getType().equals("home") && !a.getType().startsWith("B01")) {
					opentimes = a.getOpentimes("wkday");
					if (opentimes == null) {
						opentimes = a.getOpentimes("wed");
					}
					if (opentimes == null) {
						
//						if (f.getId().toString().startsWith("Pick")) {
//							// for the missing pickpay opentimes, use a random pickpay shop
//							// let's use the one close to my home :-)
//							shopId = "Pick Pay__Affoltern__8046_Zürich_In Böden 174";
//						} else {
//							// for the missing coop opentimes, use a random coop shop
//							// let's use the one close to my home :-)
//							shopId = "Coop_CC_Wehntalerstrasse_Coop Zürich_8046_Zürich_Wehntalerstrasse 549";
//						}
//						
//						Activity shopsOf2005ShopAct = ((Facility) facilityLayer.getLocation(shopId)).getActivity(FacilitiesProductionKTI.ACT_TYPE_SHOP);
//						if (shopsOf2005ShopAct != null) {
//							closestShopOpentimes = shopsOf2005ShopAct.getOpentimes();
//						}
//						a.setOpentimes(closestShopOpentimes);
						wednesdayLessActivityTypes.add(f.getId() + "_" + a.getType());
					}
				}	
			}

		}

		System.out.println("Facility activity types without a wed or wkday opening interval: ");
		System.out.println();
		for (String str : wednesdayLessActivityTypes) {
			System.out.println(str);
		}
		System.out.println();

//		System.out.println("Writing facilities xml file... ");
//		FacilitiesWriter facilities_writer = new FacilitiesWriter(facilityLayer);
//		facilities_writer.write();
//		System.out.println("Writing facilities xml file...done.");

//		boolean foundAct = false;

//		Facility facility = ((Facility) facilityLayer.getLocation("10000017"));
//		Iterator<String> facilityActTypeIterator = facility.getActivities().keySet().iterator();
//		String facilityActType = null;
//		// first is opening time, second is closing time
//		double[] openingInterval = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
////		double openingTime = Double.MAX_VALUE;
////		double closingTime = Double.MIN_VALUE;

//		while (facilityActTypeIterator.hasNext() && !foundAct) {

//		facilityActType = facilityActTypeIterator.next();
//		System.out.println(facilityActType);
//		if (actType.substring(0, 1).equals(facilityActType.substring(0, 1))) {
//		foundAct = true;
//		System.out.println("We'll use the following facility activity type: ");
//		System.out.println("\t" + facilityActType);

//		TreeSet<Opentime> opentimes = facility.getActivity(facilityActType).getOpentimes("wed");
//		System.out.println("We'll use wednesday's open times objects: ");
//		for (Opentime opentime : opentimes) {
//		System.out.println(opentime.toString());
//		// ignoring lunch breaks with the following procedure
//		openingInterval[0] = Math.min(openingInterval[0], opentime.getStartTime());
//		openingInterval[1] = Math.max(openingInterval[1], opentime.getEndTime());
//		}

//		System.out.println("Final opening times: ");
//		System.out.println("Open: " + Time.writeTime(openingInterval[0]));
//		System.out.println("Close: " + Time.writeTime(openingInterval[1]));

//		}

//		}

//		if (!foundAct) {
//		Gbl.errorMsg("No suitable facility activity type found. Aborting...");
//		}

	}

	private static void produceSTRC2007KML() {

		// config parameters
		final String KML21_MODULE = "kml21";
		final String KML21_OUTPUT_DIRECTORY = "outputDirectory";
		final String KML21_OUTPUT_FILENAME = "outputFileName";
		final String KML21_USE_COMPRESSION = "useCompression";
		final String SEP = System.getProperty("file.separator");

		CoordI worldCoord;
		CoordI geometryCoord;
		Placemark pl;

		String myKMLFilename =
			Gbl.getConfig().getParam(KML21_MODULE, KML21_OUTPUT_DIRECTORY) +
			SEP +
			Gbl.getConfig().getParam(KML21_MODULE, KML21_OUTPUT_FILENAME);

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);

		KML myKML;
		Document myKMLDocument;

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);

		///////////////////////////
		// display agent population
		///////////////////////////
		Style maleHomeStyle = new Style("maleHomeStyle");
		myKMLDocument.addStyle(maleHomeStyle);
		maleHomeStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/shapes/homegardenbusiness.png"),
						new Color("ff", "ff", "00", "00"),
						ColorStyle.DEFAULT_COLOR_MODE,
						0.5));

		Style femaleHomeStyle = new Style("femaleHomeStyle");
		myKMLDocument.addStyle(femaleHomeStyle);
		femaleHomeStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/shapes/homegardenbusiness.png"),
						new Color("ff", "00", "00", "ff"),
						ColorStyle.DEFAULT_COLOR_MODE,
						0.5));

		Folder populationFolder = new Folder(
				"population",
				"population",
				"population",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(populationFolder);

		CoordinateTransformationI trafo = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.CH1903_LV03, TransformationFactory.WGS84);

		for (Person person : matsimAgentPopulation.getPersons().values()) {
			List<Plan> plans = person.getPlans();
			// use the first plan
			Plan onePlan = plans.get(0);
			// home is first activity in the plan
			Act home = (Act) onePlan.getIteratorAct().next();
			geometryCoord = trafo.transform(home.getCoord());

			pl = new Placemark(
					"agent" + person.getId(),
					Feature.DEFAULT_NAME,
					Feature.DEFAULT_DESCRIPTION,
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					(person.getSex().equals("m")) ? maleHomeStyle.getStyleUrl() : femaleHomeStyle.getStyleUrl(),
							Feature.DEFAULT_VISIBILITY,
							Feature.DEFAULT_REGION,
							Feature.DEFAULT_TIME_PRIMITIVE);
			populationFolder.addFeature(pl);

			Point agentPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
			pl.setGeometry(agentPoint);
		}

		///////////////////////////
		// display touched network
		///////////////////////////
		Style linkStyle = new Style("defaultLinkStyle");
		myKMLDocument.addStyle(linkStyle);
		linkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "00", "00"), ColorStyle.DEFAULT_COLOR_MODE, 2));

		Folder networkCutFolder = new Folder(
				"used network",
				"used network",
				"used network",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(networkCutFolder);

		for (Link link : network.getLinks().values()) {
			networkCutFolder.addFeature(MyRuns.generateLinkPlacemark(link, linkStyle, trafo));
		}

		///////////////////////////
		// display agent #1008808's first activity plan
		///////////////////////////

		String myAgentId = new String("1008808");

		Style agentHomeStyle = new Style("agentHomeStyle");
		myKMLDocument.addStyle(agentHomeStyle);
		agentHomeStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/shapes/homegardenbusiness.png"),
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));
		agentHomeStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));

		Style shopStyle = new Style("shopStyle");
		myKMLDocument.addStyle(shopStyle);
		shopStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/shapes/shopping.png"),
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));
		shopStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));

		Style leisureStyle = new Style("leisureStyle");
		myKMLDocument.addStyle(leisureStyle);
		leisureStyle.setIconStyle(
				new IconStyle(
						new Icon("http://maps.google.com/mapfiles/kml/shapes/movies.png"),
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));
		leisureStyle.setLabelStyle(
				new LabelStyle(
						Color.DEFAULT_COLOR,
						ColorStyle.DEFAULT_COLOR_MODE,
						2.0));

		Style agentLinkStyle = new Style("agentLinkStyle");
		myKMLDocument.addStyle(agentLinkStyle);
		agentLinkStyle.setLineStyle(new LineStyle(new Color("ff", "00", "ff", "ff"), ColorStyle.DEFAULT_COLOR_MODE, 4));

		Folder agentFolder = new Folder(
				"agent #" + myAgentId,
				"agent #" + myAgentId,
				"activity plan of agent #" + myAgentId,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				false,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);
		myKMLDocument.addFeature(agentFolder);

		Person person = matsimAgentPopulation.getPerson(myAgentId);
		Plan firstPlan = person.getPlans().get(0);

		String styleUrl = null;
		String fullActName = null;
		double actEndTime;
		for (Object o : firstPlan.getActsLegs()) {

			if (o.getClass().equals(Act.class)) {

				Act act = ((Act) o);
				char actType = act.getType().charAt(0);
				switch(actType) {
				case 'h':
					styleUrl = agentHomeStyle.getStyleUrl();
					if (act.getStartTime() == 0.0) {
						fullActName = "morning home";
					} else {
						fullActName = "evening home";
					}
					break;
				case 's':
					styleUrl = shopStyle.getStyleUrl();
					fullActName = "shop";
					break;
				case 'l':
					styleUrl = leisureStyle.getStyleUrl();
					fullActName = "leisure";
					break;
				}

				actEndTime = act.getEndTime();
				if (actEndTime == Time.UNDEFINED_TIME) {
					actEndTime = 24.0 * 60 * 60;
				}

				pl = new Placemark(
						fullActName,
						Time.writeTime(act.getStartTime()) + " - " + Time.writeTime(actEndTime),
						fullActName + " activity",
						Feature.DEFAULT_ADDRESS,
						Feature.DEFAULT_LOOK_AT,
						styleUrl,
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						Feature.DEFAULT_TIME_PRIMITIVE);
				agentFolder.addFeature(pl);

				worldCoord = act.getCoord();
				geometryCoord = trafo.transform(new Coord(worldCoord.getX(), worldCoord.getY()));
				Point actPoint = new Point(geometryCoord.getX(), geometryCoord.getY(), 0.0);
				pl.setGeometry(actPoint);

				if (!fullActName.equals("evening home")) {
					Link actLink = act.getLink();
					agentFolder.addFeature(MyRuns.generateLinkPlacemark(actLink, agentLinkStyle, trafo));
				}

			} else if (o.getClass().equals(Leg.class)) {

				Link[] routeLinks = ((Leg) o).getRoute().getLinkRoute();
				for (Link routeLink : routeLinks) {
					agentFolder.addFeature(MyRuns.generateLinkPlacemark(routeLink, agentLinkStyle, trafo));
				}

			}

		}

		MyRuns.writeKML(myKML, myKMLFilename);

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

	private static void writeKML(KML theKML, String theKMLFilename) {

		System.out.println("writing KML files out...");

		KMLWriter myKMLDocumentWriter;
		myKMLDocumentWriter = new KMLWriter(theKML, theKMLFilename, KMLWriter.DEFAULT_XMLNS, false);
		myKMLDocumentWriter.write();

		System.out.println("done.");

	}

	private static void testTravelTimeI() {

		// 1 minute step size
		double stepsize = 1.0/60;
		String mode = "car";

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		// - events
		Events events = new Events();
//		TravelTimeI tTravelCalc = new TravelTimeCalculator(network);
		TravelTimeI tTravelCalc = new LinearInterpolatingTTCalculator(network);
		events.addHandler((EventHandlerI) tTravelCalc);
		events.printEventHandlers();
		MyRuns.readEvents(events, network);

		Person person = matsimAgentPopulation.getPerson("2");
		Plan plan = person.getSelectedPlan();

		Act origin = null;
		Act destination = null;
		ActIterator ia = plan.getIteratorAct();
		origin = (Act) ia.next();
		destination = (Act) ia.next();
		LegIterator il = plan.getIteratorLeg();
		Leg leg = (Leg) il.next();
		Route route = leg.getRoute();
		System.out.println(route.toString());
		Link[] links = route.getLinkRoute();
		Link destinationLink = destination.getLink();

		String outputDir = "/Users/meisterk/Documents/workspace/matsimJ/output/";
		String eventsFilename = Gbl.getConfig().getParam("events", "inputFile");
		System.out.println(eventsFilename);
		String iterationNr = eventsFilename.substring(eventsFilename.lastIndexOf("/") + 1);
		iterationNr = iterationNr.substring(0, iterationNr.indexOf("."));
		System.out.println("Iteration to be analyzed: " + iterationNr);

		String travelTimeFilename = outputDir;
		travelTimeFilename += tTravelCalc.toString();
		travelTimeFilename += "-" + iterationNr + ".txt";

		double tTravelEstimation = 0.0;
		Link link;

		try {
			// open
			BufferedWriter tTravelFile = new BufferedWriter(new FileWriter(travelTimeFilename));

			// headline
			tTravelFile.write("#T_dep");
			for (Link l : links) {
				tTravelFile.write("\t" + l.getId());
			}
			tTravelFile.write("\t" + destinationLink.getId());
			tTravelFile.newLine();

			// data
			for (double tDep = 4.0; tDep < 10.0; tDep += stepsize) {
				tTravelFile.write(Double.toString(tDep));
				for (Link l : links) {
					tTravelEstimation = tTravelCalc.getLinkTravelTime(l, tDep * 3600);
					tTravelFile.write("\t" + Double.toString(tTravelEstimation));
				}
				tTravelEstimation = tTravelCalc.getLinkTravelTime(destinationLink, tDep * 3600);
				tTravelFile.write("\t" + Double.toString(tTravelEstimation));
				tTravelFile.newLine();
			}

			// close
			tTravelFile.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println();
		System.out.println();

	}

	private static void testDepartureDelayCalculator() {

		String mode = "car";

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		// - events
		Events events = new Events();
		DepartureDelayAverageCalculator delayCalc = new DepartureDelayAverageCalculator(network, 900);
		events.addHandler(delayCalc);
		events.printEventHandlers();
		MyRuns.readEvents(events, network);

		Person person = matsimAgentPopulation.getPerson("2");
		Plan plan = person.getSelectedPlan();
		Act originAct = null;
		ActIterator ia = plan.getIteratorAct();
		originAct = (Act) ia.next();
		Link originLink = originAct.getLink();
		System.out.println("Analysing departure delays for link # " + originLink.getId());

		String outputDir = "/Users/meisterk/Documents/workspace/matsimJ/output/";
		String eventsFilename = Gbl.getConfig().getParam("events", "inputFile");
		System.out.println(eventsFilename);
		String iterationNr = eventsFilename.substring(eventsFilename.lastIndexOf("/") + 1);
		iterationNr = iterationNr.substring(0, iterationNr.indexOf("."));
		System.out.println("Iteration to be analyzed: " + iterationNr);

		String departureDelayFilename = outputDir;
		departureDelayFilename += delayCalc.toString();
		departureDelayFilename += "-" + iterationNr + ".txt";


		try {
			BufferedWriter departureDelayFile = new BufferedWriter(new FileWriter(departureDelayFilename));
			departureDelayFile.write("#T_dep\tt_delay");
			departureDelayFile.newLine();

			// 1 minute step size
			double stepsize = 1.0/60;

			for (double tDep = 4.0; tDep < 10.0; tDep += stepsize) {

				double departureDelay = delayCalc.getLinkDepartureDelay(originLink, tDep * 3600);

				departureDelayFile.write(
						tDep + "\t" +
						departureDelay);
				departureDelayFile.newLine();

			}

			departureDelayFile.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void testTravelTimeEstimator() {

		String mode = "car";

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		// - events
		Events events = new Events();
		TravelTimeI tTravelCalc = MyRuns.initTravelTimeIForPlanomat(network);
		LegTravelTimeEstimator ltte = MyRuns.createLegTravelTimeEstimator(events, network, tTravelCalc);
		events.printEventHandlers();
		MyRuns.readEvents(events, network);

		System.out.println();
		System.out.println("getLegTravelTimeEstimation() ");
		System.out.println();

		Person person = matsimAgentPopulation.getPerson("2");
		Plan plan = person.getSelectedPlan();

		Act origin = null;
		Act destination = null;
		ActIterator ia = plan.getIteratorAct();
		origin = (Act) ia.next();
		destination = (Act) ia.next();
		LegIterator il = plan.getIteratorLeg();
		Leg leg = (Leg) il.next();
		Route route = leg.getRoute();
		System.out.println(route.toString());
		System.out.println();

		double workOpeningTime = 7.0 * 60 * 60;

		Config config = Gbl.getConfig();

		marginalUtilityOfTraveling = config.charyparNagelScoring().getTraveling() / 3600.0;
		marginalUtilityOfLateArrival = config.charyparNagelScoring().getLateArrival() / 3600.0;
		marginalUtilityOfPerforming = config.charyparNagelScoring().getPerforming() / 3600.0;


		String outputDir = "/Users/meisterk/Documents/workspace/matsimJ/output/";
		String eventsFilename = Gbl.getConfig().getParam("events", "inputFile");
		System.out.println(eventsFilename);
		String iterationNr = eventsFilename.substring(eventsFilename.lastIndexOf("/") + 1);
		iterationNr = iterationNr.substring(0, iterationNr.indexOf("."));
		System.out.println("Iteration to be analyzed: " + iterationNr);

		String analysisSummaryFilename = outputDir;
		analysisSummaryFilename += ltte.toString() + "_" + tTravelCalc.toString();
		analysisSummaryFilename += "-" + iterationNr + ".txt";

		try {
			BufferedWriter analysisSummaryFile = new BufferedWriter(new FileWriter(analysisSummaryFilename));
			analysisSummaryFile.write("#t_dep\tt_travel\tt_arr\tt_late\tt_wait\tcost");
			analysisSummaryFile.newLine();

			// 1 minute step size
			double stepsize = 60;

			for (double tDep = (4.0 * 60 * 60); tDep < (10.0 * 60 * 60); tDep += stepsize) {

				double overallCost = 0.0;

				double tTravelEstimation = ltte.getLegTravelTimeEstimation(person.getId(), tDep, origin.getLink(), destination.getLink(), route, mode);
				double costTravel = tTravelEstimation * marginalUtilityOfTraveling;
				overallCost += costTravel;

				double tArrEstimation = tDep + tTravelEstimation;
				double tLateEstimation = Math.max(0, tArrEstimation - workOpeningTime);
				double costLate = tLateEstimation * marginalUtilityOfLateArrival;
				overallCost += costLate;

				double tWaitEstimation = Math.max(0, workOpeningTime - tArrEstimation);
				double costOpportunity = tWaitEstimation * marginalUtilityOfPerforming;
				overallCost -= costOpportunity;

				analysisSummaryFile.write(
						(tDep / 60 / 60) + "\t" +
						tTravelEstimation + "\t" +
						tArrEstimation + "\t" +
						tLateEstimation + "\t" +
						tWaitEstimation + "\t" +
						overallCost);
				analysisSummaryFile.newLine();
			}
			analysisSummaryFile.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println();

//		leg = (Leg) il.next();
//		route = leg.getRoute();
//		System.out.println(route.toString());
//		System.out.println();

//		for (double dd = 0.0; dd < 24.0; dd += 0.25) {
//		System.out.println("  " + dd + " - " + ltte.getLegTravelTimeEstimation(person.getId(), dd * 60 * 60, origin.getLink(), destination.getLink(), route, mode));
//		}

	}

	private static void convertPlansV0ToPlansV4() {

		System.out.println("performing vo to v4 plans conversion...");

		NetworkLayer network = MyRuns.initWorldNetwork();
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		MyRuns.writePopulation(matsimAgentPopulation);

		System.out.println("performing vo to v4 plans conversion...DONE.");


	}

	private static void planomatStandAloneDemo() {

		System.out.println("Running planomatDemo()...");
		System.out.println();

		System.out.println("  Processing config parameters...");
		System.out.println();

		// optimization method
		String optimizationToolboxName = Gbl.getConfig().planomat().getOptimizationToolbox();

		if (optimizationToolboxName.equals("jgap")) {
			System.out.println("    Using JGAP optimization toolbox.");
		} else {
			Gbl.errorMsg("    Unknown optimization toolbox identifier \"" + optimizationToolboxName + "\". Check parameter optimizationToolbox in module planomat.");
		}
		System.out.println("  Processing config parameters...DONE.");

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		// - events
		Events events = new Events();
		TravelTimeI tTravelCalc = MyRuns.initTravelTimeIForPlanomat(network);
		LegTravelTimeEstimator ltte = MyRuns.createLegTravelTimeEstimator(events, network, tTravelCalc);
		events.printEventHandlers();
		MyRuns.readEvents(events, network);

		// init matsim & run
		StrategyManager strategyManager = initStrategy(ltte, matsimAgentPopulation);
		strategyManager.run(matsimAgentPopulation);

		MyRuns.writePopulation(matsimAgentPopulation);

		System.out.println();
		System.out.println("planomatDemo() done.");

	}

	public static NetworkLayer initWorldNetwork() {

		NetworkLayer network = null;

		System.out.println("  creating network layer... ");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		return network;
	}

	private static NetworkLayer readNetwork(String filename) {

		NetworkLayer network = null;

		return network;
	}

	// Gbl.getConfig().plans().getInputFile()
	public static Plans initMatsimAgentPopulation(String inputFilename, boolean isStreaming, ArrayList<PersonAlgorithmI> algos) {

		Plans population = null;

		System.out.println("  reading plans xml file... ");
		population = new Plans(isStreaming);

		if (isStreaming) {
			// add plans algos for streaming
			if (algos != null) {
				for (PersonAlgorithmI algo : algos) {
					population.addAlgorithm(algo);
				}
			}
		}
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(inputFilename);
		population.printPlansCount();
		System.out.println("  done.");

		return population;
	}

	private static StrategyManager initStrategy(LegTravelTimeEstimator estimator, Plans matsimAgentPopulation) {

		StrategyManager strategyManager = new StrategyManager();
		PlanStrategy simplePlanomatStrategy = new PlanStrategy(new RandomPlanSelector());
		StrategyModuleI planomatStrategyModule = new PlanomatOptimizeTimes(estimator/*, matsimAgentPopulation*/);
		simplePlanomatStrategy.addStrategyModule(planomatStrategyModule);
		strategyManager.addStrategy(simplePlanomatStrategy, 1.0);

		System.out.println("  done.");

		return strategyManager;
	}

	private static TravelTimeI initTravelTimeIForPlanomat(NetworkLayer network) {

		TravelTimeI linkTravelTimeEstimator = null;

//		String travelTimeIName = Gbl.getConfig().planomat().getLinkTravelTimeEstimatorName();
//		if (travelTimeIName.equalsIgnoreCase("org.matsim.demandmodeling.events.algorithms.TravelTimeCalculator")) {
//			linkTravelTimeEstimator = new TravelTimeCalculatorArray(network);
//		} else if (travelTimeIName.equalsIgnoreCase("org.matsim.playground.meisterk.planomat.LinearInterpolatingTTCalculator")) {
//			linkTravelTimeEstimator = new LinearInterpolatingTTCalculator(network);
//		} else {
//			Gbl.errorMsg("Invalid name of implementation of TravelTimeI: " + travelTimeIName);
//		}

		return linkTravelTimeEstimator;

	}

	private static LegTravelTimeEstimator createLegTravelTimeEstimator(Events events, NetworkLayer network, TravelTimeI linkTravelTimeEstimator) {

		LegTravelTimeEstimator estimator = null;

		int timeBinSize = 900;

		// it would be nice to load the estimator via reflection (see below)
		// but if we just use make instead of Eclipse (as usual on a remote server)
		// only classes occuring in the code are compiled,
		// so we do it without reflection
		String estimatorName = Gbl.getConfig().planomat().getLegTravelTimeEstimatorName();
		if (estimatorName.equalsIgnoreCase("MyRecentEventsBasedEstimator")) {
			estimator = new MyRecentEventsBasedEstimator();
			events.addHandler((EventHandlerI) estimator);

		} else if (estimatorName.equalsIgnoreCase("CetinCompatibleLegTravelTimeEstimator")) {
			DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(network, timeBinSize);
			estimator = new CetinCompatibleLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
			events.addHandler((EventHandlerI) linkTravelTimeEstimator);
		} else if (estimatorName.equalsIgnoreCase("CharyparEtAlCompatibleLegTravelTimeEstimator")) {
			DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(network, timeBinSize);
			estimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
			events.addHandler((EventHandlerI) linkTravelTimeEstimator);
			events.addHandler(tDepDelayCalc);

		}
		else {
			Gbl.errorMsg("Invalid name of implementation of LegTravelTimeEstimatorI: " + estimatorName);
		}

		return estimator;

	}

	public static void readEvents(Events events, NetworkLayer network) {

		// load test events
		long startTime, endTime;

		System.out.println("  reading events file and (probably) running events algos");
		startTime = System.currentTimeMillis();
		new MatsimEventsReader(events).readFile(Gbl.getConfig().events().getInputFile());
		endTime = System.currentTimeMillis();
		System.out.println("  done.");
		System.out.println("  reading events from file and processing them took " + (endTime - startTime) + " ms.");
		System.out.flush();

	}

	private static void writePopulation(Plans population) {

		System.out.println("Writing plans file...");
		PlansWriter plans_writer = new PlansWriter(population);
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");
	}

	private static void writeNetwork(NetworkLayer network, String filename) {

		System.out.println("writing network file" + filename + " ... ");
		NetworkWriter network_writer = new NetworkWriter(network, filename);
		network_writer.write();
		System.out.println("done.");

	}

	private static void testCharyparNagelFitnessFunction() {

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Plans.NO_STREAMING, null);
		// - events
		Events events = new Events();
		TravelTimeI tTravelCalc = new TravelTimeCalculatorArray(network);
		LegTravelTimeEstimator ltte = MyRuns.createLegTravelTimeEstimator(events, network, tTravelCalc);
		events.printEventHandlers();
		MyRuns.readEvents(events, network);

		// put all the parameters in the config file later
		double planLength = 24.0 * 3600;
		//   use fixed default mutation rate from Herrera Et Al, later use a variable mutation rate
		int mutationRate = 20; // 1/0.005
		//   use population size of 50 as in previous planomat implentation
		int popSize = Gbl.getConfig().planomat().getPopSize();
		int numEvolutions = Gbl.getConfig().planomat().getJgapMaxGenerations();
		int percentEvolution = numEvolutions / 10;

		Configuration jgapConfiguration = null;
		IChromosome sampleChromosome = null;
		Genotype jgapGAPopulation = null;

		Iterator<Person> personIt = matsimAgentPopulation.getPersons().values().iterator();
		while (personIt.hasNext()) {
			Person person = personIt.next();
			System.out.println("  Processing agent # " + person.getId());
			Plan plan = person.getSelectedPlan();

			// analyze / modify plan for our purposes:
			// 1, how many activities do we have?
			// 2, clean all time information
			int numActs = 0;
			for (Object o : plan.getActsLegs()) {

				if (o.getClass().equals(Act.class)) {
					((Act) o).setDur(Time.UNDEFINED_TIME);
					((Act) o).setEndTime(Time.UNDEFINED_TIME);
					numActs++;
				} else if (o.getClass().equals(Leg.class)) {
					((Leg) o).setTravTime(Time.UNDEFINED_TIME);
				}

			}

			// first and last activity are assumed to be the same
			numActs -= 1;

			System.out.println("    Configuring JGAP...");
			try {
				jgapConfiguration.reset();
				jgapConfiguration = new DefaultConfiguration();

				Gene[] sampleGenes = new Gene[numActs];
				sampleGenes[0] = new DoubleGene(jgapConfiguration, 0.0, planLength);
				// plan starts at 8:00
				//sampleGenes[0].setAllele(8.00);
				for (int ii=1; ii < sampleGenes.length; ii++) {
					sampleGenes[ii] = new DoubleGene(jgapConfiguration, 0.0, planLength);
					// each act has same length
					//sampleGenes[ii].setAllele(planLength / numActs);
				}

				sampleChromosome = new Chromosome(jgapConfiguration, sampleGenes);

				jgapConfiguration.setSampleChromosome( sampleChromosome );

				// initialize scoring function
				CharyparNagelScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory();
				CharyparNagelScoringFunction sf = (CharyparNagelScoringFunction) sfFactory.getNewScoringFunction(plan);

				PlanomatFitnessFunctionWrapper fitFunc = new PlanomatFitnessFunctionWrapper( sf, plan, ltte );

				jgapConfiguration.setFitnessFunction( fitFunc );

				// elitist selection (DeJong, 1975)
				jgapConfiguration.setPreservFittestIndividual(Boolean.TRUE);

				jgapConfiguration.setPopulationSize( popSize );
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
			System.out.println("    Configuring JGAP...DONE.");

			System.out.println("    Generating initial population...");
			try {
				jgapGAPopulation = Genotype.randomInitialGenotype( jgapConfiguration );
			} catch (InvalidConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("    Generating initial population...DONE.");
			System.out.println();

			System.out.println("Running evolution...");

			for (int ii=0; ii < numEvolutions; ii++) {
				jgapGAPopulation.evolve();
//				// Print progress.
//				// ---------------
				if (ii % percentEvolution == 0) {
					//progress++;
					IChromosome fittest = jgapGAPopulation.getFittestChromosome();
					double fitness = fittest.getFitnessValue();
					System.out.println("Currently fittest Chromosome has fitness " +
							fitness);
//					if (fitness >= MAX_FITNESS) {
//					System.out.println("We discovered that MAX_FITNESS has been reached after " + ii + " generations. Cancelling evolution...");
//					System.out.flush();
//					break;
//					}
				}
			}

			System.out.println("Running evolution...DONE.");
			System.out.println();

			IChromosome fittest = jgapGAPopulation.getFittestChromosome();
			System.out.println("The fittest looks like this: "  + ((Chromosome) fittest).toString());
			System.out.println("Fitness of the fittest: " + fittest.getFitnessValue());

			MyRuns.writeChromosome2Plan(fittest, plan, ltte);
			MyRuns.writePopulation(matsimAgentPopulation);

		}

	}

	/**
	 * Writes a JGAP chromosome back to matsim plan object.
	 *
	 * @param individual the GA individual (usually the fittest after evolution) whose values will be written back to a plan object
	 * @param plan the plan that will be altered
	 */
	private static void writeChromosome2Plan(IChromosome individual, Plan plan, LegTravelTimeEstimator estimator) {

		Act activity = null;
		Leg leg = null;

		Gene[] fittestGenes = individual.getGenes();

		int max = plan.getActsLegs().size();
		double now = 0;

		for (int ii = 0; ii < max; ii++) {

			Object o = plan.getActsLegs().get(ii);

			if (o.getClass().equals(Act.class)) {

				activity = ((Act) o);

				// handle first activity
				if (ii == 0) {
					// set start to midnight
					activity.setStartTime(now);
					// set end time of first activity
					activity.setEndTime(((DoubleGene) fittestGenes[ii / 2]).doubleValue());
					// calculate resulting duration
					activity.setDur(activity.getEndTime() - activity.getStartTime());
					// move now pointer to activity end time
					now += activity.getEndTime();

					// handle middle activities
				} else if ((ii > 0) && (ii < (max - 1))) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// set duration middle activity
					activity.setDur(((DoubleGene) fittestGenes[ii / 2]).doubleValue());
					// move now pointer by activity duration
					now += activity.getDur();
					// set end time accordingly
					activity.setEndTime(now);

					// handle last activity
				} else if (ii == (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					activity.setDur(Time.UNDEFINED_TIME);
					activity.setEndTime(Time.UNDEFINED_TIME);

				}

			} else if (o.getClass().equals(Leg.class)) {

				leg = ((Leg) o);

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepTime(now);
				// set arrival time to estimation
				Location origin = ((Act) plan.getActsLegs().get(ii - 1)).getLink();
				Location destination = ((Act) plan.getActsLegs().get(ii + 1)).getLink();

				double travelTimeEstimation = estimator.getLegTravelTimeEstimation(
						plan.getPerson().getId(),
						0.0,
						origin,
						destination,
						leg.getRoute(),
				"car");
				leg.setTravTime(travelTimeEstimation);
				now += leg.getTravTime();
				// set planned arrival time accordingly
				leg.setArrTime(now);

			}
		}

		System.out.println(plan.getScore());

	}

	private static void conversionSpeedTest() {

		final int MAX = 10000000;
		String str;
		long startTime, endTime;

		// with object allocation
		startTime = System.currentTimeMillis();

		for (int ii=0; ii < MAX; ii++) {
			str = new Integer(ii).toString();
		}

		endTime = System.currentTimeMillis();

		System.out.println("With: " + (endTime - startTime));

		// without object allocation
		startTime = System.currentTimeMillis();

		for (int ii=0; ii < MAX; ii++) {
			str = Integer.toString(ii);
		}

		endTime = System.currentTimeMillis();

		System.out.println("Without: " + (endTime - startTime));

	}

	/**
	 * Used this routine for MeisterEtAl_Heureka_2008 paper,
	 * plot of number of deps, arrs by activity type to visualize 
	 * the time distribution from microcensus.
	 */
	public static void analyseInitialTimes() {

		// initialize scenario with events from a given events file
		// - network
		final NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		PersonAlgorithmI pa = new PersonAnalyseTimesByActivityType(TIME_BIN_SIZE);
		ArrayList<PersonAlgorithmI> plansAlgos = new ArrayList<PersonAlgorithmI>();
		plansAlgos.add(pa);

		Plans matsimAgentPopulation = new Plans(Plans.USE_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(matsimAgentPopulation);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		matsimAgentPopulation.printPlansCount();
		int[][] numDeps = ((PersonAnalyseTimesByActivityType) pa).getNumDeps();
		MyRuns.writeAnArray(numDeps, "output/deptimes.txt");
		int[][] numArrs = ((PersonAnalyseTimesByActivityType) pa).getNumArrs();
		MyRuns.writeAnArray(numArrs, "output/arrtimes.txt");
		int[][] numTraveling = ((PersonAnalyseTimesByActivityType) pa).getNumTraveling();
		MyRuns.writeAnArray(numTraveling, "output/traveling.txt");

	}

	private static void writeAnArray(int[][] anArray, String filename) {

		File outFile = null;
		BufferedWriter out = null;

		outFile = new File(filename);

		try {
			out = new BufferedWriter(new FileWriter(outFile));

			boolean timesAvailable = true;
			int timeIndex = 0;

			out.write("#");
			for (int ii=0; ii < Activities.values().length; ii++) {
				out.write(Activities.values()[ii] + "\t");
			}
			out.newLine();

			while (timesAvailable) {

				timesAvailable = false;

				out.write(Time.writeTime(timeIndex * TIME_BIN_SIZE) + "\t");
				for (int aa=0; aa < anArray.length; aa++) {

//					if (numDeps[aa][timeIndex] != null) {
					if (timeIndex < anArray[aa].length) {
						out.write(Integer.toString(anArray[aa][timeIndex]));
						timesAvailable = true;
					} else {
						out.write("0");
					}
					out.write("\t");
				}
				out.newLine();
				timeIndex++;
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
