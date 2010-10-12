/* *********************************************************************** *
 * project: org.matsim.*
 * BavariaPvCreator
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
package playground.dgrether.detailedEval;

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

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class BavariaPvCreator extends BavariaDemandCreator {

	private static final String popPrognose2025_2004 = DgPaths.REPOS  + "runs-svn/run1062/1062.output_plans.xml.gz";

	private static final String events2004 = DgPaths.REPOS  + "runs-svn/run1062/ITERS/it.0/1062.0.events.xml.gz";
	
	private static final String popOutFileGv = DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/personenVerkehr/population_pv_bavaria_10pct_wgs84.xml.gz";

	
	public BavariaPvCreator(){
		this.popFile = popPrognose2025_2004;
		this.eventsFile = events2004;
		this.popOutFile = popOutFileGv;
	}

	
	@Override
	protected void addNewPerson(Link startLink, Person person, Population newPop, Route route) {
		PopulationFactory popFactory = newPop.getFactory();
		Person newPerson = popFactory.createPerson(person.getId());
		newPop.addPerson(newPerson);
		Plan newPlan = popFactory.createPlan();
		newPerson.addPlan(newPlan);
		Activity oldWorkAct = ((Activity)((Plan)person.getPlans().get(2)).getPlanElements().get(0));
		//home activity
		Activity newAct = popFactory.createActivityFromCoord("pvHome", startLink.getCoord());
		LinkLeaveEvent leaveEvent = this.collector.getLinkLeaveEvent(person.getId(), startLink.getId());
		newAct.setEndTime(leaveEvent.getTime());
		newPlan.addActivity(newAct);
		//leg
		Leg leg = popFactory.createLeg("car");
		newPlan.addLeg(leg);
		//work activity
		Link endLink = net.getLinks().get(route.getEndLinkId());
		newAct = popFactory.createActivityFromCoord("pvWork", endLink.getCoord());
		newAct.setEndTime(oldWorkAct.getEndTime());
		newPlan.addActivity(newAct);
		//leg
		leg = popFactory.createLeg("car");
		newPlan.addLeg(leg);
		newAct = popFactory.createActivityFromCoord("pvHome", startLink.getCoord());
		newPlan.addActivity(newAct);
	}
	
	public static void main(String[] args) throws IOException {
		new BavariaPvCreator().createBavariaGvPop();
	}


}
