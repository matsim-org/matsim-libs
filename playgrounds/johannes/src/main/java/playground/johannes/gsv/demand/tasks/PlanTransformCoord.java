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
package playground.johannes.gsv.demand.tasks;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.gsv.demand.PopulationTask;

/**
 * @author johannes
 *
 */
public class PlanTransformCoord implements PopulationTask {

	private MathTransform transform;
	
	public PlanTransformCoord(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
	
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS);
		} catch (FactoryException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	@Override
	public void apply(Population pop) {
		for(Person person :pop.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					Coord c = act.getCoord();
					
					double[] points = new double[] { c.getX(), c.getY() };
					try {
						transform.transform(points, 0, points, 0, 1);
					} catch (TransformException e) {
						e.printStackTrace();
					}

					((ActivityImpl)act).setCoord(new Coord(points[0], points[1]));
				}
			}
		}

	}

}
