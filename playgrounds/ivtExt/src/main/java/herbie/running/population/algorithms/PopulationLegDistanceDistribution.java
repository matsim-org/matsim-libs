/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationLegDistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.population.algorithms;

import java.io.PrintStream;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Generates a crosstab of the absolute number of legs in a population, by leg mode and route distance.
 * Leg distances are classified.
 * Only selected plans are considered.
 *
 * @author meisterk
 *
 */
public class PopulationLegDistanceDistribution extends AbstractClassifiedFrequencyAnalysis implements PlanAlgorithm {

	public PopulationLegDistanceDistribution(PrintStream out) {
		super(out);
	}

	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(Plan plan) {

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				String mode = leg.getMode();

				Frequency frequency = null;
				ResizableDoubleArray rawData = null;
				if (!this.frequencies.containsKey(mode)) {
					frequency = new Frequency();
					this.frequencies.put(mode, frequency);
					rawData = new ResizableDoubleArray();
					this.rawData.put(mode, rawData);
				} else {
					frequency = this.frequencies.get(mode);
					rawData = this.rawData.get(mode);
				}

				frequency.addValue(leg.getRoute().getDistance());
				rawData.addElement(leg.getRoute().getDistance());
			}
		}

	}

}
