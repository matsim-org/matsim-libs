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

package playground.johannes.synpop.data.io;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 * 
 */
public class XMLWriter extends MatsimXmlWriter {
	
	public void write(String file, Collection<? extends Person> persons) {
		openFile(file);
		
		writeXmlHead();
		
		writeStartTag(Constants.PERSONS_TAG, null);
		for (Person person : persons) {
			writePerson(person);
		}
		writeEndTag(Constants.PERSONS_TAG);

		close();
	}

	private void writePerson(Person person) {
		List<Tuple<String, String>> atts = getAttributes(person);
		
		atts.add(new Tuple<>(Constants.ID_KEY, person.getId()));
		
		writeStartTag(Constants.PERSON_TAG, atts);
		for(Episode plan : person.getEpisodes())
			writePlan(plan);
		writeEndTag(Constants.PERSON_TAG);

	}

	private void writePlan(Episode plan) {
		writeStartTag(Constants.PLAN_TAG, getAttributes(plan));
		for (int i = 0; i < plan.getActivities().size(); i++) {
			if (i > 0)
				writeLeg(plan.getLegs().get(i - 1));

			writeActivity(plan.getActivities().get(i));
		}
		writeEndTag(Constants.PLAN_TAG);
	}

	private void writeActivity(Attributable activity) {
		writeStartTag(Constants.ACTIVITY_TAG, getAttributes(activity), true);
	}

	private void writeLeg(Attributable leg) {
		writeStartTag(Constants.LEG_TAG, getAttributes(leg), true);
	}

	private List<Tuple<String, String>> getAttributes(Attributable attributable) {
		Collection<String> keys = attributable.keys();
		List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>(keys.size() + 1);

		for (String key : keys) {
			String value = attributable.getAttribute(key);
			if(value != null) {
				atts.add(new Tuple<>(key, value));
			}
		}

		return atts;
	}
}
