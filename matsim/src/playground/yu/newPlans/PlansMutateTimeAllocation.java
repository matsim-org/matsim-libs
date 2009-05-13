/* *********************************************************************** *
 * project: org.matsim.*
 * PlansMutateTimeAllocation.java
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

/**
 * 
 */
package playground.yu.newPlans;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

/**
 * @author yu
 * 
 */
public class PlansMutateTimeAllocation implements StartupListener {
	private PlanMutateTimeAllocation pmta = new PlanMutateTimeAllocation(1800,
			MatsimRandom.getLocalInstance());

	public void notifyStartup(StartupEvent event) {
		for (Person person : event.getControler().getPopulation().getPersons()
				.values())
			for (Plan plan : person.getPlans())
				pmta.run(plan);
	}

	public static void main(String[] args) {
		Controler ctl = new Controler(args);
		ctl.addControlerListener(new PlansMutateTimeAllocation());
		ctl.setWriteEventsInterval(0);
		ctl.run();
	}
}
