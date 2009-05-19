/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCalculateActivitySpaces.java
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

package org.matsim.socialnetworks.algorithms;

import java.util.ArrayList;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.socialnetworks.socialnet.EgoNet;

public class PersonCalculateActivitySpaces {

	/**
	 * @param plans
	 * @param ego
	 * @return the average distance to all alters (a radius of a disk-shaped activity space)
	 */
	public double getPersonASD1(Population plans, Person ego) {

		double aSd = 0.;

		Activity myAct = (Activity) ego.getSelectedPlan().getPlanElements().get(0);
		Coord egoHomeCoord = myAct.getCoord();
		EgoNet personNet = (EgoNet)ego.getKnowledge().getCustomAttributes().get(EgoNet.NAME);
		ArrayList<Person> alters = personNet.getAlters();
		for (Person myAlter : alters) {
			//Coord myAlterCoord = (Coord) pfc.personGetCoords(myAlter,"home").get(0);
			myAct = (Activity) myAlter.getSelectedPlan().getPlanElements().get(0);
			Coord myAlterCoord = myAct.getCoord();
			aSd = aSd + CoordUtils.calcDistance(egoHomeCoord, myAlterCoord);
		}
		aSd = aSd / alters.size();
		return aSd;
	}

	/**
	 * @param plan
	 * @return the average straight-line distance from agent's home to all of its activities
	 * (the radius of a disk-shaped activity space).
	 */
	public double getPersonASD2(Plan plan) {

		double aSd = 0.;
		int numAct = 0;
		Activity myAct = (Activity) plan.getPlanElements().get(0);//Note this is not safe if ego is not sleeping at home

		for (int i = 2, max = plan.getPlanElements().size() - 2; i < max; i += 2) {
			Activity act1 = (Activity) (plan.getPlanElements().get(i));

			if (myAct != null && act1 != null) {
				double dist = CoordUtils.calcDistance(act1.getCoord(), myAct.getCoord());
				aSd += dist;
				numAct++;
			}
		}
		aSd = aSd / numAct;
		return aSd;
	}

}
