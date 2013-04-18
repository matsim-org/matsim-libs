/* *********************************************************************** *
 * project: org.matsim.*
 * CarAvailabilityTabuChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.Move;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.TabuChecker;

/**
 * @author thibautd
 */
public class CarAvailabilityTabuChecker implements TabuChecker<JointPlan> {

	@Override
	public void notifyMove(
			final Solution<? extends JointPlan> currentSolution,
			final Move toApply,
			final double resultingFitness) {
	}

	@Override
	public boolean isTabu(
			final Solution<? extends JointPlan> solution,
			final Move move) {
		final Solution<? extends JointPlan> resultingSol = move.apply( solution );
		final JointPlan resultingJointPlan = resultingSol.getPhenotype();

		for (Plan plan : resultingJointPlan.getIndividualPlans().values()) {
			final PersonImpl person = (PersonImpl) plan.getPerson();
			final boolean carAvail = !"never".equals( person.getCarAvail() ) &&
				!"no".equals( person.getLicense() );
			if (carAvail) continue;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg && ((Leg) pe).getMode().equals( TransportMode.car )) {
					return true;
				}
			}
		}

		return false;
	}
}

