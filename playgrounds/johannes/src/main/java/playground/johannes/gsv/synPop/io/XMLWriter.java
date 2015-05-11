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

package playground.johannes.gsv.synPop.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 * 
 */
public class XMLWriter extends MatsimXmlWriter {
	
	public void write(String file, Collection<ProxyPerson> persons) {
		openFile(file);
		
		writeXmlHead();
		
		writeStartTag(Constants.PERSONS_TAG, null);
		for (ProxyPerson person : persons) {
			writePerson(person);
		}
		writeEndTag(Constants.PERSONS_TAG);

		close();
	}

	private void writePerson(ProxyPerson person) {
		List<Tuple<String, String>> atts = getAttributes(person.getAttributes());
		
		atts.add(new Tuple<String, String>(Constants.ID_KEY, person.getId()));
		
		writeStartTag(Constants.PERSON_TAG, atts);
		for(ProxyPlan plan : person.getPlans())
			writePlan(plan);
		writeEndTag(Constants.PERSON_TAG);

	}

	private void writePlan(ProxyPlan plan) {
		writeStartTag(Constants.PLAN_TAG, getAttributes(plan.getAttributes()));
		for (int i = 0; i < plan.getActivities().size(); i++) {
			if (i > 0)
				writeLeg(plan.getLegs().get(i - 1));

			writeActivity(plan.getActivities().get(i));
		}
		writeEndTag(Constants.PLAN_TAG);
	}

	private void writeActivity(ProxyObject activity) {
		writeStartTag(Constants.ACTIVITY_TAG, getAttributes(activity.getAttributes()), true);
	}

	private void writeLeg(ProxyObject leg) {
		writeStartTag(Constants.LEG_TAG, getAttributes(leg.getAttributes()), true);
	}

	private List<Tuple<String, String>> getAttributes(Map<String, String> attributes) {
		List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>(attributes.size() + 1);

		for (Entry<String, String> entry : attributes.entrySet()) {
			String value = null;
			
//			AttributeSerializer serializer = serializers.get(entry.getKey());
//			if (serializer == null) {
				
				if (entry.getValue() != null) {
					value = entry.getValue().toString();
				}
				
//			} else {
//				value = serializer.encode(entry.getValue());
//			}

			if(value != null) {
				atts.add(new Tuple<String, String>(entry.getKey(), value));
			}
		}

		return atts;
	}
}
