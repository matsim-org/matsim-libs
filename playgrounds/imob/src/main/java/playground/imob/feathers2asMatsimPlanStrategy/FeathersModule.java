/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.imob.feathers2asMatsimPlanStrategy;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * @author nagel
 *
 */
public class FeathersModule implements BasicEventHandler {

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(Event event) {
		// somehow collect information
		if ( event instanceof PersonArrivalEvent ) {
			
		} else if ( event instanceof PersonDepartureEvent ) {
			
		}
	}

	@SuppressWarnings("static-method")
	Plan createPlan(Person person, PopulationFactory pf) {
		Plan newPlan = pf.createPlan() ;
		newPlan.setPerson( person );

		// add initial home activity:
		
		Activity act = null ;
		newPlan.addActivity(act);
		
		// ... (dz can help here)
		
		return newPlan;
	}
}
