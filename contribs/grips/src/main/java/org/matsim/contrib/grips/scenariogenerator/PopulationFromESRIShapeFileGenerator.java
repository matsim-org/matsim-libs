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
package org.matsim.contrib.grips.scenariogenerator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.grips.algorithms.FeatureTransformer;
import org.matsim.contrib.grips.config.GripsConfigModule;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DistributionType;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Point;

//this implementation is only a proof of concept
@Deprecated
public class PopulationFromESRIShapeFileGenerator {

	private static final Logger log = Logger.getLogger(PopulationFromESRIShapeFileGenerator.class);

	private final String populationShapeFile;
	protected final Scenario scenario;
	protected int id = 0;
	protected final Random rnd = MatsimRandom.getRandom();
	protected final Id safeLinkId;

	private final GripsConfigModule gcm;

	public PopulationFromESRIShapeFileGenerator(Scenario sc, String populationFile, Id safeLinkId) {
		log.warn("This implementation is a only a proof of concept!");
		this.scenario = sc;
		this.populationShapeFile = populationFile;
		this.safeLinkId = safeLinkId;
		this.gcm = (GripsConfigModule) sc.getConfig().getModule("grips");
	}

	public void run() {
		log.info("Generating population from ESRI shape file.");
		FeatureSource fs = ShapeFileReader.readDataFile(this.populationShapeFile);
		CoordinateReferenceSystem crs = fs.getSchema().getDefaultGeometry().getCoordinateSystem();
		try {
			@SuppressWarnings("unchecked")
			Iterator<Feature> it = fs.getFeatures().iterator();
			while (it.hasNext()) {
				Feature ft = it.next();
				try {
					FeatureTransformer.transform(ft, crs, this.scenario.getConfig());
				} catch (MismatchedDimensionException e) {
					throw new RuntimeException(e);
				} catch (FactoryException e) {
					throw new RuntimeException(e);
				} catch (TransformException e) {
					throw new RuntimeException(e);
				} catch (IllegalAttributeException e) {
					throw new RuntimeException(e);
				} 
				createPersons(ft);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		log.info("done");



	}

	protected void createPersons(Feature ft) {
		Population pop = this.scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		long number = (Long)ft.getAttribute("persons");
		for (; number > 0; number--) {
			Person pers = pb.createPerson(this.scenario.createId(Integer.toString(this.id++)));
			pop.addPerson(pers);
			Plan plan = pb.createPlan();
			Coord c = getRandomCoordInsideFeature(this.rnd, ft);
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			LinkImpl l = net.getNearestLink(c);
			Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
			double departureTime = getDepartureTime();
			act.setEndTime(departureTime);
			plan.addActivity(act);
			Leg leg = pb.createLeg("car");
			plan.addLeg(leg);
			Activity act2 = pb.createActivityFromLinkId("post-evac", this.safeLinkId);
			act2.setEndTime(0);
			plan.addActivity(act2);
			plan.setScore(0.);
			pers.addPlan(plan);
		}
	}

	private double getDepartureTime() {
		DepartureTimeDistributionType depTimeDistr = this.gcm.getDepartureTimeDistribution();
		if (depTimeDistr == null) {
			log.warn("No departure time distribution is given! So, we let start all evacuees at once !");
			return 0;
		}
		
		if (depTimeDistr.getDistribution() == DistributionType.LOG_NORMAL) {
			double mu = depTimeDistr.getMu();
			double sigma = depTimeDistr.getSigma();
			double r = MatsimRandom.getRandom().nextGaussian();
			return 3600*Math.exp(mu + sigma*r);
		} else if(depTimeDistr.getDistribution() == DistributionType.NORMAL) {
			double mu = depTimeDistr.getMu();
			double sigma = depTimeDistr.getSigma();
			double r = MatsimRandom.getRandom().nextGaussian();
			return 3600*mu+sigma*r;
		}
		
		throw new RuntimeException("unknown distribution type:" + depTimeDistr.getDistribution());
	}

	protected Coord getRandomCoordInsideFeature(Random rnd, Feature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!ft.getDefaultGeometry().contains(p));
		return MGC.point2Coord(p);
	}

}
