/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.old.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.LoadMyScenarios;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class Patna100PctPopulation {

	// take the home coord from initial plans (in ward) and then assign these agents to nearest link, see core method of doing this.
	private final Logger logger = Logger.getLogger(Patna100PctPopulation.class);
	private final Collection<String> mainModes = Arrays.asList("car","motorbike","bike");
	private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");

	private Scenario scenario;

	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24345");
	private String outputDir = "../../../repos/runs-svn/patnaIndia/run105/input/";

	public static void main(String[] args) {
		new Patna100PctPopulation().run();
	}

	private void run() {
		String zoneFile = "../../../repos/runs-svn/patnaIndia/inputs/wardFile/Wards.shp";		
		String planFile1 = "../../../repos/runs-svn/patnaIndia/inputs/Urban_PlanFile.CSV";
		String planFile2 = "../../../repos/runs-svn/patnaIndia/inputs/27TO42zones.CSV";
		String planFile3 = "../../../repos/runs-svn/patnaIndia/inputs/Slum_PlanFile.CSV";	
		
		String netFile = "../../../repos/runs-svn/patnaIndia/inputs/network.xml";

		int ID1 =0;			
		int ID2 = 1000000;
		int ID3= 2000000;

		scenario = LoadMyScenarios.loadScenarioFromNetwork(netFile);

		readPlansFileAndStoreData(planFile1, zoneFile, ID1);
		readPlansFileAndStoreData(planFile2, zoneFile, ID2);
		readPlansFileAndStoreData(planFile3, zoneFile, ID3);

		new PopulationWriter(scenario.getPopulation()).write(outputDir+"/patna_evac_plans_100Pct.xml.gz");
		logger.info("Writing Plan file is finished.");
	}

	private void readPlansFileAndStoreData (String planFile, String zoneFile, int startId) 	{

		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);
		Iterator<SimpleFeature> iterator = features.iterator();

		BufferedReader bufferedReader;
		String line;
		try {
			bufferedReader = IOUtils.getBufferedReader(planFile);
			line = bufferedReader.readLine();
		} catch (IOException e) {
			throw new RuntimeException("File not found. Aborting...");
		}

		while ((line)!= null ) {

			String[] parts = line.split(",");
			String fromZoneId = parts [5];

			Coord homeZoneCoordTransform = null ;
			Point p=null;

			while (iterator.hasNext()){
				SimpleFeature feature = iterator.next();
				int Id = (Integer) feature.getAttribute("ID1");
				String zoneId  = String.valueOf(Id);

				if(fromZoneId.equals(zoneId) ) {
					p = getRandomPointsFromWard(feature);
					Coord fromZoneCoord = scenario.createCoord(p.getX(), p.getY());
					homeZoneCoordTransform = ct.transform(fromZoneCoord);
				}
			}

			for (int j=0; j<100; j++){ //run with 100% sample

				String travelMode = parts [8];
				int modeTravel = Integer.parseInt(travelMode);

				switch (modeTravel) {
				case 1:	 travelMode = "pt";	break;								// Bus
				case 2:	 travelMode = "pt";	break;								// Mini Bus
				case 3:  travelMode = "car";	break;
				case 4:  travelMode = "motorbike";	break;							// all 2 W motorized 
				case 5:  travelMode = "pt";	break;								// Motor driven 3W
				case 6 : travelMode = "bike";	break;						//bicycle
				case 7 : travelMode = "pt";	break;								// train
				case 8 : travelMode = "walk";	break;
				case 9 : travelMode = "bike";	break;						//CycleRickshaw
				case 9999 : travelMode = randomModeSlum();	break;				// 480 such trips are found in which mode was not available so chosing a random mode 
				case 999999 : travelMode = randomModeUrban(); break; 			// for zones 27 to 42
				}

				if(mainModes.contains(travelMode)) {
					Population pop = scenario.getPopulation();
					PopulationFactory populationFactory = pop.getFactory();

					Person person = populationFactory.createPerson(Id.createPersonId(Integer.toString(startId++)));
					pop.addPerson(person);
					Plan plan = populationFactory.createPlan();
					person.addPlan(plan);

					Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), homeZoneCoordTransform);
					
					Activity home = populationFactory.createActivityFromLinkId("home", link.getId()); 
					home.setEndTime(9*3600); 								

					plan.addActivity(home);

					Leg leg = populationFactory.createLeg(travelMode);
					plan.addLeg(leg);

					Activity evacAct = populationFactory.createActivityFromLinkId("evac", safeLinkId);
					plan.addActivity(evacAct);
				}
			}
			try {
				line = bufferedReader.readLine();
			} catch (IOException e) {
				throw new RuntimeException("File not found. Aborting...");
			}
			iterator = features.iterator();
		}
	}

	private Point getRandomPointsFromWard (SimpleFeature feature) {
		Random random = new Random(); // matsim random will return same coord.
		Point p = null;
		double x,y;
		do {
			x = feature.getBounds().getMinX()+random.nextDouble()*(feature.getBounds().getMaxX()-feature.getBounds().getMinX());
			y = feature.getBounds().getMinY()+random.nextDouble()*(feature.getBounds().getMaxY()-feature.getBounds().getMinY());
			p= MGC.xy2Point(x, y);
		} while (!((Geometry) feature.getDefaultGeometry()).contains(p));
		return p;
	}

	// this method is for slum population as 480 plans don't have information about travel mode so one random mode is assigned out of these four modes. 
	private String randomModeSlum () {
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 15, car -0, 2W - 7, Bicycle -39 and walk 39
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <= 15)  travelMode = "pt";				
		else if (rndNr > 15 && rndNr <= 22) travelMode = "motorbike";					
		else if (rndNr > 22 && rndNr <= 61) travelMode = "bike";					
		else if (rndNr > 61 && rndNr <= 100) travelMode = "walk";					
		return travelMode;
	}

	private String randomModeUrban () {
		//		share of each vehicle is given in table 5-13 page 91 in CMP Patna
		//		pt - 23, car -5, 2W - 25, Bicycle -33 and walk 14
		Random rnd = new Random();
		int rndNr = rnd.nextInt(100);
		String travelMode = null;
		if (rndNr <=23 )  travelMode = "pt";										
		else if (rndNr > 23 && rndNr <= 48) travelMode = "motorbike";					
		else if (rndNr > 48 && rndNr <= 53 ) travelMode = "car";				
		else if (rndNr > 53 && rndNr <= 86) travelMode = "bike";					
		else if (rndNr > 86 && rndNr <= 100) travelMode = "walk";			
		return travelMode;
	}
}
