/* *********************************************************************** *
 * project: org.matsim.*
 * ComplexPopulationFromESRIShapeFileGenerator.java
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

package playground.gregor.grips.complexscenariogenerator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.evacuation.control.algorithms.FeatureTransformer;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ComplexPopulationFromESRIShapeFileGenerator {

	private final Scenario scenario;
	private final String populationShapeFile;
	
	private List<Double> depTimeLookup;
	
	private static final int RAND_SAMPLES = 100; // the number of random numbers generated for the lookup table
	
	protected int id = 0;
	protected final Random rnd = MatsimRandom.getRandom();
	
	private final Map<Long,ODRelation> odRelations = new HashMap<Long,ODRelation>();

	public ComplexPopulationFromESRIShapeFileGenerator(Scenario sc,
			String populationFile) {
		this.scenario = sc;
		this.populationShapeFile = populationFile;
	}

	public void run() {
		
		SimpleFeatureSource fs = ShapeFileReader.readDataFile(this.populationShapeFile);
		CoordinateReferenceSystem crs = fs.getSchema().getCoordinateReferenceSystem();
		try {
			SimpleFeatureIterator it = fs.getFeatures().features();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				try {
					FeatureTransformer.transform(ft, crs, this.scenario.getConfig());
				} catch (FactoryException e) {
					throw new RuntimeException(e);
				} catch (TransformException e) {
					throw new RuntimeException(e);
				}

				long id = (Long) ft.getAttribute("id");
				ODRelation odRelation = this.odRelations.get(id);
				if (odRelation == null) {
					odRelation = new ODRelation();
					this.odRelations.put(id, odRelation);
				}
				long persons = (Long) ft.getAttribute("persons");
				if (persons > 0) {
					odRelation.o = ft;
				} else {
					odRelation.d = ft;
				}
//				createPersons(ft);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		
		for (Entry<Long, ODRelation>  e : this.odRelations.entrySet()) {
			createPersons(e);
		}
		
	}

	
	private void createPersons(Entry<Long, ODRelation> e) {
		Population pop = this.scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		long number = (Long)e.getValue().o.getAttribute("persons");
		double min = (Double)e.getValue().o.getAttribute("earliest");
		double max = (Double)e.getValue().o.getAttribute("latest");
		double sigma = (Double)e.getValue().o.getAttribute("sigma");
		double mu = (Double)e.getValue().o.getAttribute("mu");
		genDepTimeLookup(min, max, mu, sigma);
		
		for (; number > 0; number--) {
			Person pers = pb.createPerson(Id.create(this.id++, Person.class));
			pop.addPerson(pers);
			Plan plan = pb.createPlan();
			Coord c = getRandomCoordInsideFeature(this.rnd, e.getValue().o);
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			Link l = NetworkUtils.getNearestLink(net, c);
			Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
			((ActivityImpl)act).setCoord(c);
			double departureTime = getDepartureTime();
			act.setEndTime(departureTime); 
			
			plan.addActivity(act);
			Leg leg = pb.createLeg("car");
			plan.addLeg(leg);
			Coord c2 = getRandomCoordInsideFeature(this.rnd, e.getValue().d);
			Link l2 = NetworkUtils.getNearestLink(net, c2);
			Activity act2 = pb.createActivityFromLinkId("post-evac", l2.getId());
			act2.setEndTime(0);
			((ActivityImpl)act2).setCoord(c2);
			plan.addActivity(act2);
			plan.setScore(0.);
			pers.addPlan(plan);
		}
		
	}

	private void genDepTimeLookup(double min, double max, double mu, double sigma) {

		List<Double> randVariables = new ArrayList<Double>();

			double incr = (max-min)/RAND_SAMPLES;
			for (int i = 0; i < RAND_SAMPLES; i ++) {
				randVariables.add(min*3600);
				min += incr;
			}

//		Collections.sort(randVariables);
		this.depTimeLookup = randVariables;
//		double offset = this.depTimeLookup.get(0);
//		// offset >= earliest // otherwise throw Exception?
//		for (int i = 0; i < this.depTimeLookup.size(); i++) {
//			double depTime = this.depTimeLookup.get(i)-offset;
//			// departure time relative to the first agent, i.e. the start of the simulation
//			this.depTimeLookup.set(i, depTime);
//		}
		Collections.shuffle(this.depTimeLookup);
	}
	
	private double getDepartureTime() {
		return this.depTimeLookup.get((this.id-1)%this.depTimeLookup.size());
	}

	protected Coord getRandomCoordInsideFeature(Random rnd, SimpleFeature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!((Geometry) ft.getDefaultGeometry()).contains(p));
		return MGC.point2Coord(p);
	}
	

	private static final class ODRelation {
		SimpleFeature o;
		SimpleFeature d;
	}
}
