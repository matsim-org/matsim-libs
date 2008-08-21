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
import java.util.List;
import java.util.TreeMap;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.ActUtilityParameters;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType.Activities;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.CoordinateTransformation;
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
//		MyRuns.conversionSpeedTest();
//		MyRuns.convertPlansV0ToPlansV4();
//		MyRuns.produceSTRC2007KML();

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

	private static void produceSTRC2007KML() {

		// config parameters
		final String KML21_MODULE = "kml21";
		final String KML21_OUTPUT_DIRECTORY = "outputDirectory";
		final String KML21_OUTPUT_FILENAME = "outputFileName";
		final String KML21_USE_COMPRESSION = "useCompression";
		final String SEP = System.getProperty("file.separator");

		Coord worldCoord;
		Coord geometryCoord;
		Placemark pl;

		String myKMLFilename =
			Gbl.getConfig().getParam(KML21_MODULE, KML21_OUTPUT_DIRECTORY) +
			SEP +
			Gbl.getConfig().getParam(KML21_MODULE, KML21_OUTPUT_FILENAME);

		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Population matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Gbl.getConfig().plans().getInputFile(), Population.NO_STREAMING, null);

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

		CoordinateTransformation trafo = TransformationFactory.getCoordinateTransformation(
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
				geometryCoord = trafo.transform(new CoordImpl(worldCoord.getX(), worldCoord.getY()));
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

	private static Placemark generateLinkPlacemark(Link link, Style style, CoordinateTransformation trafo) {

		Placemark linkPlacemark = null;

		Node fromNode = link.getFromNode();
		Coord fromNodeWorldCoord = fromNode.getCoord();
		Coord fromNodeGeometryCoord = trafo.transform(new CoordImpl(fromNodeWorldCoord.getX(), fromNodeWorldCoord.getY()));
		Point fromPoint = new Point(fromNodeGeometryCoord.getX(), fromNodeGeometryCoord.getY(), 0.0);

		Node toNode = link.getToNode();
		Coord toNodeWorldCoord = toNode.getCoord();
		Coord toNodeGeometryCoord = trafo.transform(new CoordImpl(toNodeWorldCoord.getX(), toNodeWorldCoord.getY()));
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

	// Gbl.getConfig().plans().getInputFile()
	public static Population initMatsimAgentPopulation(String inputFilename, boolean isStreaming, ArrayList<PersonAlgorithm> algos) {

		Population population = null;

		System.out.println("  reading plans xml file... ");
		population = new Population(isStreaming);

		if (isStreaming) {
			// add plans algos for streaming
			if (algos != null) {
				for (PersonAlgorithm algo : algos) {
					population.addAlgorithm(algo);
				}
			}
		}
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(inputFilename);
		population.printPlansCount();
		System.out.println("  done.");

		return population;
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
		PersonAlgorithm pa = new PersonAnalyseTimesByActivityType(TIME_BIN_SIZE);
		ArrayList<PersonAlgorithm> plansAlgos = new ArrayList<PersonAlgorithm>();
		plansAlgos.add(pa);

		Population matsimAgentPopulation = new Population(Population.USE_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(matsimAgentPopulation);
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
			e.printStackTrace();
		}
	}
}
