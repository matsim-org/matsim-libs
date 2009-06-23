/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityOptionFinder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.MDSAM;


import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.facilities.algorithms.AbstractFacilityAlgorithm;
import org.matsim.core.controler.Controler;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;

import playground.mfeil.config.PlanomatXConfigGroup;





/**
 * Class that searches all facilities of a given facilities layer (e.g., the facilities of a scenario) 
 * for activity options. Returns a list of all found activity options.
 * 
 * @author Matthias Feil
 */
public class ActivityTypeFinder extends AbstractFacilityAlgorithm {
	
	private List<String> actTypes;
	private static final Logger log = Logger.getLogger(ActivityTypeFinder.class);
	private final Controler controler;
	
	public ActivityTypeFinder (Controler controler){
		this.controler = controler;
		this.actTypes = new ArrayList<String>();
	}
	
	public void run (final ActivityFacilities facilities) {
		for (ActivityFacility f : facilities.getFacilities().values()) {
			run(f);
		}
		log.info("Searching available activity types done.");
	}
	public void run(ActivityFacility facility){
		Collection<ActivityOption> facActTypes = facility.getActivityOptions().values();
		for (Iterator<ActivityOption> iterator = facActTypes.iterator();iterator.hasNext();){
			ActivityOption act = iterator.next();
			if (!this.actTypes.contains(act.getType())){
				this.actTypes.add(act.getType());
			}
		}
	}
	public List<String> getActTypes (){
		return this.actTypes;
	}
	
	public List<String> getActTypes (Person agent){
		if (PlanomatXConfigGroup.getActTypes().equals("knowledge")){
			return this.getKnActTypes(agent);
		}
		
		else if (PlanomatXConfigGroup.getActTypes().equals("customized")){
			List<String> agentKnActTypes = this.getKnActTypes(agent);
			if (agent.getAge()<6){
				return agentKnActTypes;
			}
			// remove defined act types from overall list, unless in agent's knowledge
			List<String> agentCuActTypes = new ArrayList<String>(this.actTypes);
			if (agent.getAge()<18){
				if (!agentKnActTypes.contains("education_kindergarten"))agentCuActTypes.remove("education_kindergarten");
				if (!agentKnActTypes.contains("education_higher"))agentCuActTypes.remove("education_higher");
				if (!agentKnActTypes.contains("work_sector2"))agentCuActTypes.remove("work_sector2");
				if (!agentKnActTypes.contains("work_sector3"))agentCuActTypes.remove("work_sector3");
				return agentCuActTypes;
			}
			// remove defined act types from overall list, unless in agent's knowledge
			else {
				if (!agentKnActTypes.contains("education_kindergarten")) agentCuActTypes.remove("education_kindergarten");
				if (!agentKnActTypes.contains("education_primary"))agentCuActTypes.remove("education_primary");
				if (!agentKnActTypes.contains("education_secondary"))agentCuActTypes.remove("education_secondary");
				return agentCuActTypes;
			}
		}
		
		else return this.actTypes;
	}
	
	private List<String> getKnActTypes (Person agent){
		// get act options of agent
		Collection<ActivityOption> agentActOptions = ((ScenarioImpl)(this.controler.getScenarioData())).getKnowledges().getKnowledgesByPersonId().get(agent.getId()).getActivities();
		// convert them into act types
		List<String> agentActTypes = new ArrayList<String>();
		for (Iterator<ActivityOption> iterator = agentActOptions.iterator();iterator.hasNext();){
			ActivityOption act = iterator.next();
			if (!agentActTypes.contains(act.getType())){
				agentActTypes.add(act.getType());
			}
		}
		return agentActTypes;
	}
	
}
