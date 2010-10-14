/* *********************************************************************** *
 * project: org.matsim.*
 * BavariaGvCreator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.prognose2025;

import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;


/**
 * Extension of DgPrognose2025DemandFilter for freight transport demands.
 * @author dgrether
 *
 */
public class DgPrognose2025GvDemandFilter extends DgPrognose2025DemandFilter {
	
	public DgPrognose2025GvDemandFilter(){
	}

	@Override
	protected void addNewPerson(Link startLink, Person person, Population newPop, Route route) {
		PopulationFactory popFactory = newPop.getFactory();
		Person newPerson = popFactory.createPerson(person.getId());
		newPop.addPerson(newPerson);
		Plan newPlan = popFactory.createPlan();
		newPerson.addPlan(newPlan);
		//start activity
		Activity newAct = popFactory.createActivityFromCoord("gvHome", startLink.getCoord());
		LinkLeaveEvent leaveEvent = this.collector.getLinkLeaveEvent(person.getId(), startLink.getId());
		newAct.setEndTime(leaveEvent.getTime());
		newPlan.addActivity(newAct);
		Leg leg = popFactory.createLeg("car");
		newPlan.addLeg(leg);
		//end activity
		Link endLink = net.getLinks().get(route.getEndLinkId());
		newAct = popFactory.createActivityFromCoord("gvHome", endLink.getCoord());
		newPlan.addActivity(newAct);
	} 

	public static void main(String[] args) throws IOException {
		if (args == null || args.length == 0){
			new DgPrognose2025GvDemandFilter().filterAndWriteDemand(DgDetailedEvalFiles.PROGNOSE_2025_2004_NETWORK, 
					DgDetailedEvalFiles.GV_POPULATION_INPUT_FILE, DgDetailedEvalFiles.GV_EVENTS_FILE, DgDetailedEvalFiles.BAVARIA_SHAPE_FILE,
					DgDetailedEvalFiles.GV_POPULATION_OUTPUT_FILE);
		}
		else if (args.length == 5){
			new DgPrognose2025GvDemandFilter().filterAndWriteDemand(args[0], args[1], args[2], args[3], args[4]);
		}
	}






}

