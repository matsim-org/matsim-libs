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

package playground.ciarif.algos ;



import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

public class ModeAlgo extends PersonAlgorithm{

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
		
		double rd = Gbl.random.nextDouble();
		Plan plan = person.getSelectedPlan();
		ArrayList<Object> acts_legs = plan.getActsLegs();

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
		ArrayList<Object> acts_legs = plan.getActsLegs();
		
		for (int i=2; i<acts_legs.size(); i=i+2) {
			Act act = (Act)acts_legs.get(i);
			double distX = (((Act)acts_legs.get(i)).getCoord().getX() - ((Act)acts_legs.get(i-2)).getCoord().getX());
			// Position variation on the x axis 
			double distY = (((Act)acts_legs.get(i)).getCoord().getY() - ((Act)acts_legs.get(i-2)).getCoord().getY());
			// Position variation on the y axis
			dist = dist + Math.sqrt(Math.pow(distX, 2)+ Math.pow(distY, 2));
		}
		return dist;
	}
		
}
