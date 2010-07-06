/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAlgo.java
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

package playground.ciarif.modechoice_old ;



import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class ModeAlgo extends AbstractPersonAlgorithm{

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModeAlgo() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

  
	@Override
	public void run(Person person) {
		
		double rd = MatsimRandom.getRandom().nextDouble();
		Plan plan = person.getSelectedPlan();
		List<? extends PlanElement> acts_legs = plan.getPlanElements();

		for (int i=1; i<acts_legs.size()-1; i=i+2) {
			Leg leg = (Leg)acts_legs.get(i);
			if (rd<0.5){
				(leg).setMode("train");
			}
		}
	}
	
	public double calcDist (Person person) {
		
		double dist=0;
		Plan plan = person.getSelectedPlan();
		List<? extends PlanElement> acts_legs = plan.getPlanElements();
		
		for (int i=2; i<acts_legs.size(); i=i+2) {
			ActivityImpl act = (ActivityImpl)acts_legs.get(i);
			Coord coord1 = ((ActivityImpl)acts_legs.get(i)).getCoord();
			Coord coord2 = ((ActivityImpl)acts_legs.get(i - 2)).getCoord();
			
			dist = dist + CoordUtils.calcDistance(coord1, coord2);
		}
		return dist;
	}
		
}
