/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.anhorni.rc;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class RCControler extends Controler {
				
	public ObjectAttributes prefs = new ObjectAttributes();
	
	public RCControler(final String[] args) {
		super(args);	
	}

	public static void main (final String[] args) { 
		RCControler controler = new RCControler(args);
		controler.setOverwriteFiles(true);
		controler.setScoringFunctionFactory(new RCScoringFunctionFactory(
				controler.getConfig().planCalcScore(), controler.getScenario(), controler.prefs));
		
		controler.readPrefs(controler.getScenario());
    	controler.run();
    }
	
	
	private void readPrefs(Scenario scenario) {
		log.info("reading prefs ...");
		for (ActivityParams activityParams : scenario.getConfig().planCalcScore().getActivityParams()) {
			log.info("activity param:" + activityParams.getType());
			int counter = 0;
			int nextMsg = 1;
			for (Person p : scenario.getPopulation().getPersons().values()) {
				counter++;
				if (counter % nextMsg == 0) {
					nextMsg *= 2;
					log.info(" person # " + counter);
				}
				PersonImpl person = (PersonImpl)p;
				Desires desires = person.getDesires();					
				if (desires != null) {
					// hä? in the desires, only the typical duration can be specified. need to get the rest from the config anyway, or from where else?
					prefs.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getType(), desires.getActivityDuration(activityParams.getType()));
				} else {				
					prefs.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getType(), activityParams.getTypicalDuration());
					log.error("there should be desires!");
				}
				prefs.putAttribute(p.getId().toString(), "latestStartTime_" + activityParams.getType(), activityParams.getLatestStartTime());
				prefs.putAttribute(p.getId().toString(), "earliestEndTime_" + activityParams.getType(), activityParams.getEarliestEndTime());
				prefs.putAttribute(p.getId().toString(), "minimalDuration_" + activityParams.getType(), activityParams.getMinimalDuration());
			}
		}
		log.info("Reading prefs finished");
	}	
}
