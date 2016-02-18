/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.teach.demand;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * @author jbischoff
 * A basic class for demand generation used in the MATSim class at TU Berlin.
 * This class needs some (intended) additional programming effort by its user.
 */
public class CreateDemand {

	private static final String NETWORKFILE = "input/network.xml";
	private static final String KREISE = "input/Landkreise/Kreise.shp";
	
	private static final String PLANSFILEOUTPUT = "input/plans.xml";
	
	private static final String SHOPS = "input/shops.txt";
	private static final String KINDERGARTEN = "input/kindergaerten.txt";
	
	
	private Scenario scenario;
	private Map<String,Geometry> shapeMap;
	private static double SCALEFACTOR = 0.1;
	
	private Map<String,Coord> kindergartens;
	private Map<String,Coord> shops;
	
	
	CreateDemand (){
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
				
	}
	
	private void run() {
		this.shapeMap = readShapeFile(KREISE, "Nr");
		this.shops = readFacilityLocations(SHOPS);
		this.kindergartens = readFacilityLocations(KINDERGARTEN);
		
		//CB-CB
		double commuters = 22709*SCALEFACTOR;
		double carcommuters = 0.55 * commuters;
		//Watch out, counties come with some extra zeros in the end in the shape file
		System.out.println(this.shapeMap.keySet());
		Geometry home = this.shapeMap.get("12052000");
		Geometry work = this.shapeMap.get("12052000");
		for (int i = 0; i<=commuters;i++){
			String mode = "car";
			if (i>carcommuters) mode = "pt";
			Coord homec = drawRandomPointFromGeometry(home);
			System.out.println(homec+" : closest kg: "+this.findClosestCoordFromMap(homec, this.kindergartens)+"closest shop:"+this.findClosestCoordFromMap(homec, this.shops));
//			System.out.println(this.findClosestCoordFromMap(homec, this.kindergartens));
			Coord workc = drawRandomPointFromGeometry(work);
			createOnePerson(i, homec, workc, mode, "CB_CB");
		}
	
		PopulationWriter pw = new PopulationWriter(scenario.getPopulation(),scenario.getNetwork());
		pw.write(PLANSFILEOUTPUT);
	}
	
	
	private void createOnePerson(int i, Coord coord, Coord coordWork, String mode, String toFromPrefix) {
		Id<Person> personId = Id.createPersonId(toFromPrefix+i);
		Person person = scenario.getPopulation().getFactory().createPerson(personId);
 
		Plan plan = scenario.getPopulation().getFactory().createPlan();
 
 
		Activity home = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(9*60*60);
		plan.addActivity(home);
 
		Leg hinweg = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(hinweg);
 
		Activity work = scenario.getPopulation().getFactory().createActivityFromCoord("work", coordWork);
		work.setEndTime(17*60*60);
		plan.addActivity(work);
 
		Leg rueckweg = scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(rueckweg);
 
		Activity home2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);
 
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}
	
	private  Coord drawRandomPointFromGeometry(Geometry g) {
		   Random rnd = MatsimRandom.getLocalInstance();
		   Point p;
		   double x, y;
		   do {
		      x = g.getEnvelopeInternal().getMinX() +  rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
		      y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
		      p = MGC.xy2Point(x, y);
		   } while (!g.contains(p));
		   Coord coord = new Coord(p.getX(), p.getY());
		   return coord;
		}
	


	public static void main(String[] args) {

		CreateDemand cd = new CreateDemand();
		cd.run();
	}
	
	public Map<String,Geometry> readShapeFile(String filename, String attrString){
		//attrString: FÃ¼r Brandenburg: Nr
		//fÃ¼r OSM: osm_id
		
		Map<String,Geometry> shapeMap = new HashMap<String, Geometry>();
		
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);
				Geometry geometry;

				try {
					geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
					shapeMap.put(ft.getAttribute(attrString).toString(),geometry);

				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} 
		return shapeMap;
	}	
	
	private Map<String,Coord> readFacilityLocations (String fileName){
		
		FacilityParser fp = new FacilityParser();
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterRegex("\t");
		config.setCommentRegex("#");
		config.setFileName(fileName);
		new TabularFileParser().parse(config, fp);
		return fp.getFacilityMap();
		
	}
	
	private Coord findClosestCoordFromMap(Coord location, Map<String,Coord> coordMap){
		Coord closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Coord coord : coordMap.values()){
			double distance = CoordUtils.calcEuclideanDistance(coord, location);
			if (distance<closestDistance) {
				closestDistance = distance;
				closest = coord;
			}
		}
		return closest;
	}
	
	
	
}

	class FacilityParser implements TabularFileHandler{
		
		
	private Map<String,Coord> facilityMap = new HashMap<String, Coord>();	
	CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:32633"); 

	@Override
	public void startRow(String[] row) {
		try{
		Double x = Double.parseDouble(row[2]);
		Double y = Double.parseDouble(row[1]);
		Coord coords = new Coord(x,y);
		this.facilityMap.put(row[0],ct.transform(coords));
		}
		catch (NumberFormatException e){
			//skips line
		}
	}

	public Map<String, Coord> getFacilityMap() {
		return facilityMap;
	}
	
	
}