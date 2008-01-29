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

package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;

import playground.jhackney.module.socialnet.EgoNet;

public class PersonCalculateActivitySpaces {

    public double getPersonASD1(Plans plans, Person ego) {
	// Returns the average distance to all alters.
	// A radius of a disk-shaped activity space

	double aSd=0.;

	Act myAct=(Act) ego.getSelectedPlan().getActsLegs().get(0);
	CoordI egoHomeCoord = myAct.getCoord();
	EgoNet personNet = ego.getKnowledge().egoNet;
	ArrayList alters = personNet.getAlters();
	Iterator fIter = alters.iterator();
	while (fIter.hasNext()){
	    Person myAlter = (Person) fIter.next();
	    //Coord myAlterCoord = (Coord) pfc.personGetCoords(myAlter,"home").get(0);
	    myAct=(Act) myAlter.getSelectedPlan().getActsLegs().get(0);
	    CoordI myAlterCoord = myAct.getCoord();
	    aSd = aSd + egoHomeCoord.calcDistance(myAlterCoord);
	    aSd = aSd / alters.size();
	    }
	    return aSd;
    }
    public double getPersonASD2(Plan plan) {
	// Returns the average distance to all alters.
	// A radius of a disk-shaped activity space

	double aSd=0.;
	int numAct=0;
	Act myAct=(Act) plan.getActsLegs().get(0);//Note this is not safe if ego is not sleeping at home

	for (int i = 2, max= plan.getActsLegs().size(); i < max-2; i += 2) {
	    Act act1 = (Act)(plan.getActsLegs().get(i));

	    if (myAct != null || act1 != null) {
		double dist = act1.getCoord().calcDistance(myAct.getCoord());
		aSd += dist;
		numAct++;
	    }
	}
	aSd = aSd / numAct ;
	return aSd;
    }

}
