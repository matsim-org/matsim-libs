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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
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
	public void run(PersonImpl person) {
		
		double rd = MatsimRandom.getRandom().nextDouble();
		PlanImpl plan = person.getSelectedPlan();
		List<? extends BasicPlanElement> acts_legs = plan.getPlanElements();

		for (int i=1; i<acts_legs.size()-1; i=i+2) {
			LegImpl leg = (LegImpl)acts_legs.get(i);
			if (rd<0.5){
				(leg).setMode(TransportMode.train);
			}
		}
	}
	
	public double calcDist (PersonImpl person) {
		
		double dist=0;
		PlanImpl plan = person.getSelectedPlan();
		List<? extends BasicPlanElement> acts_legs = plan.getPlanElements();
		
		for (int i=2; i<acts_legs.size(); i=i+2) {
			ActivityImpl act = (ActivityImpl)acts_legs.get(i);
			Coord coord1 = ((ActivityImpl)acts_legs.get(i)).getCoord();
			Coord coord2 = ((ActivityImpl)acts_legs.get(i - 2)).getCoord();
			
			dist = dist + CoordUtils.calcDistance(coord1, coord2);
		}
		return dist;
	}
		
}
