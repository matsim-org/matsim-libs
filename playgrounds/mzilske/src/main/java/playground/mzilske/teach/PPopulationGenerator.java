/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mzilske.teach;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

  
public class PPopulationGenerator implements Runnable {
  
	private Map<Integer, Geometry> zoneGeometries = new HashMap<Integer, Geometry>();
	private Map<String, Geometry> zoneActivities = new HashMap<String, Geometry>();
	
	private String crs = "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	//is the coordinate system from the network
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);

	private Scenario scenario;

	private Population population;
	private static Random rnd;

	public static void main(String[] args) {
  		PPopulationGenerator potsdamPop = new PPopulationGenerator();
  		potsdamPop.run();
  	}
  
  	@Override
  	public void run() {
  		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
  		population = scenario.getPopulation();
  		fillZoneData();
  		generatePopulation(0.1);
  		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
  		populationWriter.write("./input/plans.xml");
  	}
  	
  	private void readShapeFile() {
  	   for (SimpleFeature feature : ShapeFileReader.getAllFeatures("/Users/zilske/workspace2/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp")) {
  		   zoneGeometries.put(Integer.decode((String) feature.getAttribute("Nr")), (Geometry) feature.getDefaultGeometry());
  	   }
  	}
  	
  	private void fillZoneData() {//Where are the activities?
  		readShapeFile();
  		zoneActivities.put("home_P", zoneGeometries.get((12054000)));
  		zoneActivities.put("work_P", zoneGeometries.get((12054000)));
  		zoneActivities.put("home_BER", zoneGeometries.get((11)));
  		zoneActivities.put("work_BER", zoneGeometries.get((11)));
  		zoneActivities.put("home_BH", zoneGeometries.get((12051000)));
  		zoneActivities.put("work_BH", zoneGeometries.get((12051000)));
  		zoneActivities.put("home_HVL", zoneGeometries.get((12063000)));
  		zoneActivities.put("work_HVL", zoneGeometries.get((12063000)));
  		zoneActivities.put("home_PMM", zoneGeometries.get((12069000)));
  		zoneActivities.put("work_PMM", zoneGeometries.get((12069000)));
  		zoneActivities.put("home_TFL", zoneGeometries.get((12072000)));
  		zoneActivities.put("work_TFL", zoneGeometries.get((12072000)));
  		
  	}
  
  	private void generatePopulation(double percentage) {
  		generateHomeWorkHomeTrips("home_P", "work_P", (int)(1.29*28863*percentage/100));
  	//	generateHomeWorkHomeTrips("home_P", "work_BER", (int)(1.29*13281*percentage/100));
  		generateHomeWorkHomeTrips("home_P", "work_BH", (int)(1.29*509*percentage/100));
  		generateHomeWorkHomeTrips("home_P", "work_HVL", (int)(1.29*764*percentage/100));
  		generateHomeWorkHomeTrips("home_P", "work_PMM", (int)(1.29*5981*percentage/100));
  		generateHomeWorkHomeTrips("home_P", "work_TFL", (int)(1.29*1624*percentage/100));
  	//	generateHomeWorkHomeTrips("home_BER", "work_P", (int)(1.29*13014*percentage/100));
  		generateHomeWorkHomeTrips("home_BH", "work_P", (int)(1.29*1680*percentage/100));
  		generateHomeWorkHomeTrips("home_HVL", "work_P", (int)(1.29*2515*percentage/100));
  		generateHomeWorkHomeTrips("home_PMM", "work_P", (int)(1.29*13847*percentage/100));
  		generateHomeWorkHomeTrips("home_TFL", "work_P", (int)(1.29*2966*percentage/100));
  		
  	}
  
  	private void generateHomeWorkHomeTrips(String from, String to, int quantity) {
  		for (int i=0; i<quantity; ++i) {
  			Geometry source = zoneActivities.get(from);
  			Geometry sink = zoneActivities.get(to);
  			Person person;
  			Plan plan;
  			Coord homeLocation;
  			Coord workLocation;
  			if (i%3==0){//jede dritte Person f�hrt �PNV
  				person = population.getFactory().createPerson(createPersonId(from.substring(5), to.substring(5), i, TransportMode.pt));
  				plan = population.getFactory().createPlan();
  				homeLocation = ct.transform(drawRandomPointFromGeometry(source));
  				workLocation = ct.transform(drawRandomPointFromGeometry(sink));
  				plan.addActivity(createHome(homeLocation));
  				plan.addLeg(createOPNVLeg());
  				plan.addActivity(createWork(workLocation));
  				plan.addLeg(createOPNVLeg());
  			}
  			else {
  				person = population.getFactory().createPerson(createPersonId(from.substring(5), to.substring(5), i, TransportMode.car));
  				plan = population.getFactory().createPlan();
  				homeLocation = ct.transform(drawRandomPointFromGeometry(source));
  				workLocation = ct.transform(drawRandomPointFromGeometry(sink));
  				plan.addActivity(createHome(homeLocation));
  				plan.addLeg(createDriveLeg());
  				plan.addActivity(createWork(workLocation));
  				plan.addLeg(createDriveLeg());
  			}
  			plan.addActivity(createHome(homeLocation));
  			person.addPlan(plan);
  			population.addPerson(person);
  		}
  	}
  
  	private Leg createDriveLeg() {
  		Leg leg = population.getFactory().createLeg(TransportMode.car);
  		return leg;
  	}
  	
  	private Leg createOPNVLeg() {
  		Leg leg = population.getFactory().createLeg(TransportMode.pt);
  		return leg;
  	}
  	
  	private static Coord drawRandomPointFromGeometry(Geometry g) {
  	   rnd = new Random();
  	   com.vividsolutions.jts.geom.Point p;
  	   double x, y;
  	   do {
  	      x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
  	      y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
  	      p = MGC.xy2Point(x, y);
  	   } while (!g.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
  	   return coord;
  	}

  	private Activity createWork(Coord workLocation){
  		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
  		Random rnd = new Random();
  		double endtime = 16*3600 + rnd.nextInt(2*3600);
  		activity.setEndTime(endtime);
  		return activity;
  	}
  
  	private Activity createHome(Coord homeLocation) {
  		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
  		Random rnd = new Random();
  		double endtime = 7*3600 + rnd.nextInt(2*3600);
  		activity.setEndTime(endtime);
  		return activity;
  	}
  
 	private Id<Person> createPersonId(String source, String sink, int i, String transportMode) {
 		return Id.create(source + "_" + sink + "_" + transportMode + "_" + i, Person.class);
 	}
 
 }