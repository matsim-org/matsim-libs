/* *********************************************************************** *
 * project: org.matsim.*
 * FlightStuckedReplanningStrategy
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
package air.run;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;


/**
 * @author dgrether
 *
 */
public class FlightStuckedReplanning implements ReplanningListener, PersonStuckEventHandler, StartupListener{

	private static final Logger log = Logger.getLogger(FlightStuckedReplanning.class);

	public static final String STRATEGY_NAME = "flightStuckedReplanningStrategy";

	private Set<Id> stuckedPersonIds = new HashSet<Id>();

	@Override
	public void notifyStartup(StartupEvent e) {
		e.getControler().getEvents().addHandler(this);
	}
	
	@Override
	public void notifyReplanning(ReplanningEvent e) {
		ReplanningContext rc = e.getReplanningContext();
		TripRouter tripRouter = rc.getTripRouter();
		PlanRouter router = new PlanRouter(tripRouter);
		Scenario  scenario = e.getControler().getScenario();
		for (Id id : this.stuckedPersonIds){
			Person p = scenario.getPopulation().getPersons().get(id);
			Plan plan = p.getSelectedPlan();
			((Activity)plan.getPlanElements().get(0)).setEndTime(3.0 * 3600.0);
			router.run(plan);
		}
	}

	
	@Override
	public void reset(int iteration) {
		this.stuckedPersonIds.clear();
	}


	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckedPersonIds .add(event.getPersonId());
	}





	
}
