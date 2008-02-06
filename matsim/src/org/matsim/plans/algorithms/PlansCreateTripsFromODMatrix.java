/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreateTripsFromODMatrix.java
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

package org.matsim.plans.algorithms;

import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class PlansCreateTripsFromODMatrix extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	Matrix matrix = null;
	ArrayList<Double> timeDistribution = null;
	int timeBinSize;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateTripsFromODMatrix(final Matrix matrix, final ArrayList<Double> timeDistribution) {
		super();
		this.matrix = matrix;
		this.timeDistribution = new ArrayList<Double>(timeDistribution.size());
		double sum = 0.0;
		for (int i = 1, max = timeDistribution.size(); i < max; i++) {
			sum += timeDistribution.get(i);	// instead of building the sum every time
			this.timeDistribution.add(sum); // build the sum once and cache them
		}
		this.timeBinSize = (24*3600) / timeDistribution.size();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		if (plans.getName() == null) {
			plans.setName("created by '" + this.getClass().getName() + "' with matrix '" + this.matrix.getId() + "'");
		}
		if (!plans.getPersons().isEmpty()) {
			Gbl.errorMsg("[plans=" + plans + " is not empty]");
		}

		ZoneLayer layer = (ZoneLayer)this.matrix.getLayer();

		try {
			int counter = 0;
			double sum = 0.0;
			for (ArrayList<Entry> entries : this.matrix.getFromLocations().values()) {
				for (Entry entry : entries) {
					sum += entry.getValue();
					while (sum >= 1.0) {
						counter++;
						sum--;
						Person person = new Person(Integer.toString(counter), null/*sex*/, null/*age*/, null/*license*/, "yes"/*car_avail*/, "yes"/*employed*/);
						Plan plan = person.createPlan(null, "yes");
						Coord coord = WorldUtils.getRandomCoordInZone((Zone)entry.getFromLocation(), layer);
						int endTime = -1;

						double rnd = Gbl.random.nextDouble();
						for (int i = 0, max = this.timeDistribution.size(); i < max && endTime == -1; i++) {
							if (rnd <= this.timeDistribution.get(i)) {
								endTime = i*this.timeBinSize + Gbl.random.nextInt(this.timeBinSize);
							}
						}

						plan.createAct("work", coord.getX(), coord.getY(), null, 0/*startTime*/, endTime/*endTime*/, endTime/*dur*/, true/*isPrimary*/);
						plan.createLeg(1, "car", endTime/*depTime*/, 0/*travTime*/, Integer.MIN_VALUE/*arrTime*/);
						plan.createAct("work", coord.getX(), coord.getY(), null, Integer.MIN_VALUE/*startTime*/, 24*3600/*endTime*/, Integer.MIN_VALUE/*duration*/, false/*isPrimary*/);

						plans.addPerson(person); // add person should be last for when plans-streaming is one, because in this moment the plans are written to file.
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("    done.");
	}

}
