/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveScores.java
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

/**
 * @author illenberger
 *
 */
public class RemoveScores implements StartupListener {

	/* (non-Javadoc)
	 * @see org.matsim.controler.listener.StartupListener#notifyStartup(org.matsim.controler.events.StartupEvent)
	 */
	public void notifyStartup(StartupEvent event) {
		for(Person p : event.getControler().getPopulation()) {
			for(Plan plan : p.getPlans())
				plan.setScore(0);
		}
	}

}
