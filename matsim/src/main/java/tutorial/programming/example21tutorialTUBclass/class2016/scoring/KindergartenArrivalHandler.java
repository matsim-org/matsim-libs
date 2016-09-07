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
package tutorial.programming.example21tutorialTUBclass.class2016.scoring;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class KindergartenArrivalHandler  implements PersonArrivalEventHandler, ActivityStartEventHandler {

	Id<Link> kindergartenLink = Id.createLinkId(8142);
	Set<Id<Person>> arrivedOnLinkByCar = new HashSet<>();
	int kinder = 0;

	@Override
	public void reset(int iteration) {
		arrivedOnLinkByCar = new HashSet<>();
		kinder = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLinkId().equals(kindergartenLink)){
			if (event.getLegMode().equals(TransportMode.car)){
				this.arrivedOnLinkByCar.add(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		if (event.getLinkId().equals(kindergartenLink)){
			if (event.getActType().equals("kindergarten1"))
			kinder++;
		}
	}
	
	
}
