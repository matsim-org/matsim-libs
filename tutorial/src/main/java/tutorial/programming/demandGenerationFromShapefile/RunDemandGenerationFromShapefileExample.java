/* *********************************************************************** *
 * project: org.matsim.*																															*
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

package tutorial.programming.demandGenerationFromShapefile;

import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * This class generates a simple artificial MATSim demand for
 * the german city Löbau. This is similar to the tutorial held
 * at the MATSim user meeting 09 by glaemmel, however based on
 * the matsim api.
 *
 * The files needed to run this tutorial are placed in the matsim examples
 * repository that can be found in the root directory of the matsim
 * sourceforge svn under the path matsimExamples/tutorial/example8DemandGeneration.
 *
 * @author glaemmel
 * @author dgrether
 *
 */
public class RunDemandGenerationFromShapefileExample {

	private static final Logger log = Logger.getLogger(RunDemandGenerationFromShapefileExample.class);

	private static int ID = 0;

	private static final String exampleDirectory = "../matsimExamples/tutorial/example8DemandGeneration/";

	public static void main(String [] args) throws IOException {

		// input files
		String zonesFile = exampleDirectory + "zones.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		SimpleFeatureSource fts = ShapeFileReader.readDataFile(zonesFile); //reads the shape file in
		Random rnd = new Random();

		SimpleFeature commercial = null;
		SimpleFeature recreation = null;

		//Iterator to iterate over the features from the shape file
		SimpleFeatureIterator it = fts.getFeatures().features();
		while (it.hasNext()) {
			SimpleFeature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
			//of other attributes
			if (ft.getAttribute("type").equals("commercial")) {
				commercial = ft;
			}
			else if (ft.getAttribute("type").equals("recreation")) {
				recreation = ft;
			}
			else if (ft.getAttribute("type").equals("housing")) {
				long l = ((Long)ft.getAttribute("inhabitant"));
				createPersons(scenario, ft, rnd, (int) l); //creates l new persons and chooses for each person a random link
				//within the corresponding polygon. after this method call every new generated person has one plan and one home activity
			}
			else {
				throw new RuntimeException("Unknown zone type:" + ft.getAttribute("type"));
			}
		}
		it.close();
		createActivities(scenario, rnd, recreation, commercial); //this method creates the remaining activities
		String popFilename = exampleDirectory + "population.xml";
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(popFilename); // and finally the population will be written to a xml file
		log.info("population written to: " + popFilename);
	}

	private static void createActivities(Scenario scenario, Random rnd,  SimpleFeature recreation, SimpleFeature commercial) {
		Population pop =  scenario.getPopulation();
		PopulationFactory pb = pop.getFactory(); //the population builder creates all we need

		for (Person pers : pop.getPersons().values()) { //this loop iterates over all persons
			Plan plan = pers.getPlans().get(0); //each person has exactly one plan, that has been created in createPersons(...)
			Activity homeAct = (Activity) plan.getPlanElements().get(0); //every plan has only one activity so far (home activity)
			homeAct.setEndTime(7*3600); // sets the endtime of this activity to 7 am

			Leg leg = pb.createLeg(TransportMode.car);
			plan.addLeg(leg); // there needs to be a log between two activities

			//work activity on a random link within one of the commercial areas
			Point p = getRandomPointInFeature(rnd, commercial);
			Activity work = pb.createActivityFromCoord("w", new Coord(p.getX(), p.getY()));
			double startTime = 8*3600;
			work.setStartTime(startTime);
			work.setEndTime(startTime + 6*3600);
			plan.addActivity(work);

			plan.addLeg(pb.createLeg(TransportMode.car));

			//recreation activity on a random link within one of the recreation area
			p = getRandomPointInFeature(rnd, recreation);
			Activity leisure = pb.createActivityFromCoord("l", new Coord(p.getX(), p.getY()));
			leisure.setEndTime(3600*19);
			plan.addActivity(leisure);

			plan.addLeg(pb.createLeg(TransportMode.car));

			//finally the second home activity - it is clear that this activity needs to be on the same link
			//as the first activity - since in this tutorial our agents do not relocate ;-)
			Activity homeActII = pb.createActivityFromCoord("h", homeAct.getCoord());
			plan.addActivity(homeActII);
		}

	}

	private static void createPersons(Scenario scenario, SimpleFeature ft, Random rnd, int number) {
		Population pop = scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		for (; number > 0; number--) {
			Person pers = pb.createPerson(Id.create(ID++, Person.class));
			pop.addPerson( pers ) ;
			Plan plan = pb.createPlan();
			Point p = getRandomPointInFeature(rnd, ft);
			Activity act = pb.createActivityFromCoord("h", new Coord(p.getX(), p.getY()));
			plan.addActivity(act);
			pers.addPlan( plan ) ;
		}
	}

	private static Point getRandomPointInFeature(Random rnd, SimpleFeature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (((Geometry) ft.getDefaultGeometry()).contains(p));
		return p;
	}


}
