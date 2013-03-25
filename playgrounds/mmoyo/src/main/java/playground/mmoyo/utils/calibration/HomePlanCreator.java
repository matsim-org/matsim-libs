/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;

public class HomePlanCreator implements PersonAlgorithm{
	final Population population;
	final String strHOME = "home";
	final String strWALK = "walk";
	
	public HomePlanCreator (final Population population){
		this.population = population;
	}
	
	@Override
	public void run(Person person) {
		PopulationFactory popFactory= this.population.getFactory();
		Plan homePlan = popFactory.createPlan();
		ActivityImpl actHome = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0));
		Coord homeCoord = actHome.getCoord();
		Id linkId =  actHome.getLinkId();
		ActivityImpl homeAct = new ActivityImpl(strHOME, homeCoord);
		homeAct.setEndTime(3600.0);
		homeAct.setLinkId(linkId);
		homePlan.addActivity(homeAct);
		Leg leg = popFactory.createLeg(strWALK);
		leg.setTravelTime(10.0);
		homePlan.addLeg(leg);
		homeAct = new ActivityImpl(strHOME, homeCoord);
		homeAct.setEndTime(Time.MIDNIGHT-1);  
		homeAct.setLinkId(linkId);
		//homeAct.setStartTime(85500.0);//85500 = 23:45 hr  OLD
		homePlan.addActivity(homeAct);
		person.addPlan(homePlan);
	}
	
}
