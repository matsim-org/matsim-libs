/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationFromESRIShapeFileGenerator.java
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
package org.matsim.contrib.evacuation.scenariogenerator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.evacuation.control.algorithms.FeatureTransformer;
import org.matsim.contrib.evacuation.io.DepartureTimeDistribution;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

//this implementation is only a proof of concept
@Deprecated
public class PopulationFromESRIShapeFileGenerator {

	private static final Logger log = Logger.getLogger(PopulationFromESRIShapeFileGenerator.class);

	private static final int RAND_SAMPLES = 1000; // the number of random numbers generated for the lookup table

	private final String populationShapeFile;
	protected final Scenario scenario;
	protected int id = 0;
	protected final Random rnd = MatsimRandom.getRandom();
	protected final Id safeLinkId;

	private final EvacuationConfigModule gcm;

	private List<Double> depTimeLookup;

	// Konstruktor mit Scenario, PopFile, Senke
	public PopulationFromESRIShapeFileGenerator(Scenario sc, String populationFile, Id safeLinkId) {
//		log.warn("This implementation is a only a proof of concept!");
		this.scenario = sc;
		this.populationShapeFile = populationFile;
		this.safeLinkId = safeLinkId;
		this.gcm = (EvacuationConfigModule) sc.getConfig().getModule("evacuation");
	}

	public void run() {
		log.info("Generating departure time lookup");
		genDepTimeLookup();

		log.info("Generating population from ESRI shape file.");
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
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} 
				createPersons(ft);
			}
			it.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		log.info("done");



	}

	// Hier werden this.RAND_SAMPLES Zufallszahlen erzeugt, aus denen dann gezogen wird

	private void genDepTimeLookup() {
		DepartureTimeDistribution depTimeDistr = this.gcm.getDepartureTimeDistribution();
		if (depTimeDistr == null) {
			log.warn("No departure time distribution is given! So, we let start all evacuees at once !");
			this.depTimeLookup = new ArrayList<Double>();
			this.depTimeLookup.add(0.);
			return;
		}

		List<Double> randVariables = new ArrayList<Double>();

    double min = depTimeDistr.getEarliest();
    double max = depTimeDistr.getLatest();
		if (depTimeDistr.getDistribution().equals(DepartureTimeDistribution.LOG_NORMAL)) { // 
			double mu_h = Math.log(depTimeDistr.getMu()/3600.0);
			double sigma_h = Math.log(depTimeDistr.getSigma()/3600.0);
			for (int i = 0; i < RAND_SAMPLES; i ++) {
				double sec, hrs;
				do {
					double r = MatsimRandom.getRandom().nextGaussian();
					// eine Zahl zwischen -inf und +inf, normal-verteilt
					// besser mit Stunden rechnen!
					hrs = Math.exp(mu_h + sigma_h*r);
					sec = hrs*3600.0;
				} while(sec < min || sec > max);
				randVariables.add(sec);
			}
		} else if(depTimeDistr.getDistribution().equals(DepartureTimeDistribution.NORMAL)) {
			double mu = depTimeDistr.getMu();
			double sigma = depTimeDistr.getSigma();
			for (int i = 0; i < RAND_SAMPLES; i ++) {
			    double r=0.0, sec=0.0;
			    do{
			      r = MatsimRandom.getRandom().nextGaussian();
			      sec=mu+sigma*r;
			    } while(sec < min || sec > max);
				randVariables.add(sec);
			}
		} else if (depTimeDistr.getDistribution().equals(DepartureTimeDistribution.DIRAC_DELTA)) {
			this.depTimeLookup = new ArrayList<Double>();
			this.depTimeLookup.add(0.);
			return;
		} else {
			throw new RuntimeException("unknown distribution type:" + depTimeDistr.getDistribution());
		}

		Collections.sort(randVariables);
		this.depTimeLookup = randVariables;
		double offset = this.depTimeLookup.get(0);
		// offset >= earliest // otherwise throw Exception?
		for (int i = 0; i < this.depTimeLookup.size(); i++) {
			double depTime = this.depTimeLookup.get(i)-offset;
			// departure time relative to the first agent, i.e. the start of the simulation
			this.depTimeLookup.set(i, depTime);
		}
		Collections.shuffle(this.depTimeLookup);
	}

	protected void createPersons(SimpleFeature ft) {
		Population pop = this.scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		long number = (Long)ft.getAttribute("persons");
		for (; number > 0; number--) {
			Person pers = pb.createPerson(Id.create(this.id++, Person.class));
			pop.addPerson(pers);
			Plan plan = pb.createPlan();
			Coord c = getRandomCoordInsideFeature(this.rnd, ft);
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			Link l = NetworkUtils.getNearestLink(net, c);
			Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
			((ActivityImpl)act).setCoord(c);
			double departureTime = getDepartureTime();
			act.setEndTime(departureTime); 
			// hier wird die Departure Time gesetzt
			plan.addActivity(act);
			Leg leg = pb.createLeg("car");
			plan.addLeg(leg);
			Activity act2 = pb.createActivityFromLinkId("post-evac", this.safeLinkId);
			act2.setEndTime(0);
			((ActivityImpl)act2).setCoord(this.scenario.getNetwork().getLinks().get(this.safeLinkId).getCoord());
			plan.addActivity(act2);
			plan.setScore(0.);
			pers.addPlan(plan);
		}
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

}
