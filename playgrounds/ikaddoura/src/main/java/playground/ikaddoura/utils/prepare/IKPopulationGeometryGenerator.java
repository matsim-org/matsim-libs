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

/**
 * 
 */
package playground.ikaddoura.utils.prepare;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author ikaddoura
 *
 */
public class IKPopulationGeometryGenerator {

	private Map<Integer, Geometry> zoneGeometries = new HashMap<Integer, Geometry>();	
	private Scenario scenario;
	private Population population;
	private double scale = 1.0; 
	
	public static void main(String[] args) {
		IKPopulationGeometryGenerator popGen = new IKPopulationGeometryGenerator();
		popGen.run();
		
	}

	public void run(){
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		
		fillZoneData();
		generatePopulation();
		
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("~/plans.xml");
	}

	private void fillZoneData() {
		readShapeFile();
	}
	
	private void readShapeFile() {
		ShapeFileReader reader = new ShapeFileReader();
		Set<SimpleFeature> features;
		features = (Set<SimpleFeature>) reader.readFileAndInitialize("~/kreise_berlin.shp");
		for (SimpleFeature feature : features) {
			zoneGeometries.put(Integer.parseInt((String)feature.getAttribute("Nr")), (Geometry) feature.getDefaultGeometry());
		}
	}
	
	private static Coord drawRandomPointFromGeometry(Geometry g) {
		   Random rnd = new Random();
		   Point p;
		   double x, y;
		   do {
		      x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
		      y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
		      p = MGC.xy2Point(x, y);
		   } while (!g.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
		   return coord;
		}
	
	
	private void generatePopulation() {
		
		// Einpendler_Car
		generateHomeWorkHomeTripsCar("Berlin", 1, "Potsdam", 6, (int)(11248*scale));
		generateHomeWorkHomeTripsCar("Teltow-Fläming", 2, "Potsdam", 6, (int)(2564*scale));
		generateHomeWorkHomeTripsCar("Potsdam-Mittelmark", 3, "Potsdam", 6, (int)(11968*scale));
		generateHomeWorkHomeTripsCar("Brandenburg an der Havel", 4, "Potsdam", 6, (int)(1452*scale));
		generateHomeWorkHomeTripsCar("Havelland", 5, "Potsdam", 6, (int)(2174*scale));
		
	}

	private void generateHomeWorkHomeTripsCar(String from, int fromNr, String to, int toNr, int quantity) {
		for (int i=0; i<quantity; ++i) {
			
			Geometry startGeometry = zoneGeometries.get(fromNr);
			Geometry endGeometry = zoneGeometries.get(toNr);			
			Coord homeLocation = drawRandomPointFromGeometry(startGeometry);
			Coord workLocation = drawRandomPointFromGeometry(endGeometry);
			Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.pt));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLegCar());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLegCar());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}
	
	private Leg createDriveLegCar() {
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Activity createWork(Coord workLocation) {
		Random rnd = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime((17*60*60)+(rnd.nextDouble()*60*60)-(rnd.nextDouble()*60*60));
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Random rnd = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(8*60*60+(rnd.nextDouble()*60*60)-(rnd.nextDouble()*60*60));
		return activity;
	}

	private Id<Person> createId(String source, String sink, int i, String transportMode) {
		return Id.create(transportMode + "_" + source + "_" + sink + "_" + i, Person.class);
	}
}
