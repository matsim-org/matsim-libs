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

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class BavariaGvCreator extends BavariaDemandCreator {
	
	private static final String popPrognose2025_2004 = DgPaths.REPOS  + "runs-svn/run1060/1060.output_plans.xml.gz";

	private static final String events2004 = DgPaths.REPOS  + "runs-svn/run1060/ITERS/it.0/1060.0.events.xml.gz";
	
	private static final String popOutFileGv = DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_10pct_wgs84.xml.gz";

	
	
	public BavariaGvCreator(){
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

	
	

	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new BavariaGvCreator().createBavariaPop();
	}






}

