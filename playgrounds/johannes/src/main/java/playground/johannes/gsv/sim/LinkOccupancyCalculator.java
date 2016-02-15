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

package playground.johannes.gsv.sim;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class LinkOccupancyCalculator implements LinkLeaveEventHandler, PersonDepartureEventHandler {

	private TObjectDoubleHashMap<Id<Link>> values;

	private TObjectIntHashMap<Id<Person>> counter;

	private TObjectDoubleHashMap<String> rates;

	private static final String HOME = "home";

	private Population population;

	private final boolean personEqualsVeh;

	public double getOccupancy(Id<Link> linkId) {
		return values.get(linkId);
	}

	public LinkOccupancyCalculator(Population population, boolean personEqualsVeh) {
		this.personEqualsVeh = personEqualsVeh;
		this.population = population;

		rates = new TObjectDoubleHashMap<String>();
		rates.put("work", 1.2);
		rates.put("edu", 1.7);
		rates.put("shop", 1.5);
		rates.put("private", 1.5);
		rates.put("leisure", 1.9);
		rates.put("pickdrop", 1.9);
		/*
		 * for foreign traffic
		 */
		rates.put("buisiness", 1.1);
		rates.put("vacations_short", 1.9);
		rates.put("vacations_long", 1.9);

	}

	public LinkOccupancyCalculator(Population population) {
		this(population, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		values = new TObjectDoubleHashMap<>(5000);
		counter = new TObjectIntHashMap<>(population.getPersons().size());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler#handleEvent
	 * (org.matsim.api.core.v01.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {

		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Person> personId = Id.createPersonId(vehicleId);
		Person person = population.getPersons().get(personId);

		Plan plan = person.getSelectedPlan();

		int idx = counter.get(personId);
		Activity nextAct = (Activity) plan.getPlanElements().get(idx + 1);
		String type = nextAct.getType();

		if (type.equalsIgnoreCase(HOME)) {
			Activity prevAct = (Activity) plan.getPlanElements().get(idx - 1);
			type = prevAct.getType();
		}

		double rate = 1;
		if (!personEqualsVeh) {
			rate = rates.get(type);
			if (rate == 0)
				rate = 1.5;
		}
		rate = 1 / rate;

		Id<Link> linkId = event.getLinkId();

		values.adjustOrPutValue(linkId, rate, rate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#
	 * handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> id = event.getPersonId();
		counter.adjustOrPutValue(id, 2, 1);
	}

	public void writeValues(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			writer.write("link\toccupancy");
			writer.newLine();

			TObjectDoubleIterator<Id<Link>> it = values.iterator();
			for (int i = 0; i < values.size(); i++) {
				it.advance();
				writer.write(it.key().toString());
				writer.write("\t");
				writer.write(String.valueOf(it.value()));
				writer.newLine();
			}

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
