/* *********************************************************************** *
 * project: org.matsim.*
 * TravelProba.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;

import gnu.trove.TDoubleDoubleHashMap;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class TravelProba {

	public static void main(String args[]) throws TransformException, FileNotFoundException, IOException {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/plans.xml");

		Population pop = scenario.getPopulation();

		CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
		CoordinateReferenceSystem targetCRS = CRSUtils.getCRS(21781);
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS);

		} catch (Exception e) {
			e.printStackTrace();
		}

		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
				Activity act = (Activity) plan.getPlanElements().get(i);
				double[] points = new double[] { act.getCoord().getX(), act.getCoord().getY() };
				transform.transform(points, 0, points, 0, 1);
				act.getCoord().setX(points[0]);
				act.getCoord().setY(points[1]);
			}
		}

		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Discretizer discretizer = new LinearDiscretizer(5000.0);
		GeometryFactory geoFactory = new GeometryFactory();

		Distribution distr = new Distribution();
		int cnt1 = 0;
		int cnt2 = 0;
		for (Person person2 : pop.getPersons().values()) {
			Plan plan = person2.getPlans().get(0);

			for (int i = 1; i < plan.getPlanElements().size(); i += 2) {

				if (plan.getPlanElements().size() > i + 1) {
					Activity destAct = (Activity) plan.getPlanElements().get(i + 1);
					Activity srcAct = (Activity) plan.getPlanElements().get(i - 1);

					Route route = ((Leg) plan.getPlanElements().get(i)).getRoute();

					if (route != null) {
//						if (destAct.getType().startsWith("l")) {
							TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
							Point p1 = geoFactory.createPoint(new Coordinate(srcAct.getCoord().getX(), srcAct
									.getCoord().getY()));
							for (Person person : pop.getPersons().values()) {
								Plan plan2 = person.getPlans().get(0);
								for (int j = 2; j < plan2.getPlanElements().size(); j += 2) {
									Activity act2 = (Activity) plan2.getPlanElements().get(j);
//									if (act2.getType().startsWith("l")) {
										Point p2 = geoFactory.createPoint(new Coordinate(act2.getCoord().getX(), act2
												.getCoord().getY()));
										double d = dCalc.distance(p1, p2);
										n_d.put(discretizer.discretize(d), 1.0);
//									}
								}
							}

							double d = discretizer.discretize(route.getDistance());
							double n = n_d.get(d);
							if (n > 0) {
								distr.add(d, 1 / n);
							} else
								cnt2++;
//						}
					}
				}
				cnt1++;
				if(cnt1 % 500 == 0) {
					System.out.println("Processed " + cnt1 + " persons. " + cnt2 + " samples skipped");
				}
			}
		}

		Distribution.writeHistogram(distr.absoluteDistribution(),
				"/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/analysis/proba.all.txt");
	}
}
