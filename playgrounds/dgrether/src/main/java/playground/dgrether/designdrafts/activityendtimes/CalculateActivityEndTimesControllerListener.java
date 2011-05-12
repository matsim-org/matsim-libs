/* *********************************************************************** *
 * project: org.matsim.*
 * CalculateActivityEndTimesControllerListener
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
package playground.dgrether.designdrafts.activityendtimes;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.misc.Time;


/**
 * Calculates activity end times from activity durations
 * @author dgrether
 *
 */
public class CalculateActivityEndTimesControllerListener implements StartupListener {

	private static final Logger log = Logger.getLogger(CalculateActivityEndTimesControllerListener.class);

	public void notifyStartup(StartupEvent event) {
		log.info("calculating missing end times of Activities...");
		Population pop = event.getControler().getPopulation();
		for (Person person : pop.getPersons().values()){
			for (Plan plan : person.getPlans()) {
				Iterator<PlanElement> it  = plan.getPlanElements().iterator();
				while(it.hasNext()){
					PlanElement pe = it.next();
					if (pe instanceof Activity){
						Activity a = (Activity)pe;
						if (it.hasNext() && (a.getEndTime() == Time.UNDEFINED_TIME)) {
							if ((((ActivityImpl)a).getMaximumDuration() == Time.UNDEFINED_TIME)){
								log.warn("neither endtime nor duration set in plan of person id " + person.getId() + " Cannot calculate activity endtime!");
								continue;
							}
							if (a.getStartTime() == Time.UNDEFINED_TIME){
								log.warn("neither starttime nor duration set in plan of person id " + person.getId() + " Cannot calculate activity endtime!");
								continue;
							}
							a.setEndTime(a.getStartTime() + ((ActivityImpl)a).getMaximumDuration());
						}
					}
				}
				
			}
		}
		log.info("calculating missing end times of Activities completed.");
	}
	
}
