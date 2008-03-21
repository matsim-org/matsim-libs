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

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.socialnet.EgoNet;
import org.matsim.utils.geometry.CoordI;

public class PersonCalculateActivitySpaces {

	/**
	 * @param plans
	 * @param ego
	 * @return the average distance to all alters (a radius of a disk-shaped activity space)
	 */
	public double getPersonASD1(Plans plans, Person ego) {

		double aSd = 0.;

		Act myAct = (Act) ego.getSelectedPlan().getActsLegs().get(0);
		CoordI egoHomeCoord = myAct.getCoord();
		EgoNet personNet = ego.getKnowledge().egoNet;
		ArrayList<Person> alters = personNet.getAlters();
		for (Person myAlter : alters) {
			//Coord myAlterCoord = (Coord) pfc.personGetCoords(myAlter,"home").get(0);
			myAct = (Act) myAlter.getSelectedPlan().getActsLegs().get(0);
			CoordI myAlterCoord = myAct.getCoord();
			aSd = aSd + egoHomeCoord.calcDistance(myAlterCoord);
			aSd = aSd / alters.size();
		}
		/* TODO [JH] Please check this, as it doesn't make sense for me:
		 * I think that the last line in the for-loop ("aSd = aSd / alters.size()") should
		 * be OUTSIDE of the loop. Don't you first sum up all the distances, and than divide
		 * by the number of values to get the average?
		 */
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
		Act myAct = (Act) plan.getActsLegs().get(0);//Note this is not safe if ego is not sleeping at home

		for (int i = 2, max = plan.getActsLegs().size() - 2; i < max; i += 2) {
			Act act1 = (Act) (plan.getActsLegs().get(i));

			if (myAct != null && act1 != null) {
				double dist = act1.getCoord().calcDistance(myAct.getCoord());
				aSd += dist;
				numAct++;
			}
		}
		aSd = aSd / numAct;
		return aSd;
	}

}
