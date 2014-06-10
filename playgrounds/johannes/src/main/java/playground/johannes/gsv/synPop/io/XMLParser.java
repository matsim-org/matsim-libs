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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.johannes.gsv.synPop.ProxyActivity;
import playground.johannes.gsv.synPop.ProxyLeg;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class XMLParser extends MatsimXmlParser {

	private Map<String, AttributeSerializer> serializers = new HashMap<String, AttributeSerializer>();;
	
	private Set<ProxyPerson> persons;
	
	private ProxyPerson person;
	
	private ProxyPlan plan;
	
	public Set<ProxyPerson> getPersons() {
		return persons;
	}
	
	public void addSerializer(AttributeSerializer serializer) {
		serializers.put(serializer.getKey(), serializer);
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equalsIgnoreCase(Constants.PERSONS_TAG)) {
			persons = new HashSet<ProxyPerson>();
			
		} else if(name.equalsIgnoreCase(Constants.PERSON_TAG)) {
			person = new ProxyPerson((String) getAttribute(Constants.ID_KEY, atts));
			for(int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				if(!type.equalsIgnoreCase(Constants.ID_KEY)) {
					person.setAttribute(type, getAttribute(type, atts));
				}
			}
		} else if (name.equalsIgnoreCase(Constants.PLAN_TAG)) {
			plan = new ProxyPlan();
		} else if (name.equalsIgnoreCase(Constants.ACTIVITY_TAG)) {
			ProxyActivity act = new ProxyActivity();
			for(int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				act.setAttribute(type, getAttribute(type, atts));
			}
			plan.addActivity(act);
			
		} else if (name.equalsIgnoreCase(Constants.LEG_TAG)) {
			ProxyLeg leg = new ProxyLeg();
			for(int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				leg.setAttribute(type, getAttribute(type, atts));
			}
			plan.addLeg(leg);
			
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(Constants.PERSON_TAG)) {
			persons.add(person);
			person = null;
		} else if(name.equalsIgnoreCase(Constants.PLAN_TAG)) {
			person.setPlan(plan);
			plan = null;
		}

	}

	private Object getAttribute(String key, Attributes atts) {
		AttributeSerializer serializer = serializers.get(key);
		if(serializer == null) {
			return atts.getValue(key);
		} else {
			return serializer.decode(atts.getValue(key));
		}
	}
}
