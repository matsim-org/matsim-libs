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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.*;

/**
 * @author johannes
 * 
 */
public class XMLHandler extends MatsimXmlParser {

	private static final Logger logger = Logger.getLogger(XMLHandler.class);

	private Set<Person> persons;

	private Person person;

	private Episode plan;

	private List<String> blacklist = new ArrayList<>();

	private final Factory factory;

	public XMLHandler(Factory factory) {
		this.factory = factory;
	}

	public Set<? extends Person> getPersons() {
		return persons;
	}

	public void addToBlacklist(String key) {
		blacklist.add(key);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equalsIgnoreCase(Constants.PERSONS_TAG)) {
			persons = new LinkedHashSet<>();

		} else if (name.equalsIgnoreCase(Constants.PERSON_TAG)) {
			person = factory.newPerson(getAttribute(Constants.ID_KEY, atts));

			for (int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				if (!type.equalsIgnoreCase(Constants.ID_KEY)) {
					if (!blacklist.contains(type)) {
						person.setAttribute(type, getAttribute(type, atts));
					}
				}
			}

		} else if (name.equalsIgnoreCase(Constants.PLAN_TAG)) {
			plan = factory.newEpisode();

			for (int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);

				if (!blacklist.contains(type)) {
					plan.setAttribute(type, getAttribute(type, atts));
				}

			}

		} else if (name.equalsIgnoreCase(Constants.ACTIVITY_TAG)) {
			Segment act = factory.newSegment();

			for (int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				if (!blacklist.contains(type)) {
					act.setAttribute(type, getAttribute(type, atts));
				}
			}
			plan.addActivity(act);

		} else if (name.equalsIgnoreCase(Constants.LEG_TAG)) {
			Segment leg = factory.newSegment();

			for (int i = 0; i < atts.getLength(); i++) {
				String type = atts.getLocalName(i);
				if (!blacklist.contains(type)) {
					leg.setAttribute(type, getAttribute(type, atts));
				}
			}
			plan.addLeg(leg);

		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equalsIgnoreCase(Constants.PERSON_TAG)) {
			persons.add(person);
			person = null;

			if (persons.size() % 50000 == 0)
				logger.info(String.format("Parsed %s persons...", persons.size()));
		} else if (name.equalsIgnoreCase(Constants.PLAN_TAG)) {
			person.addEpisode(plan);
			plan = null;
		}

	}

	private String getAttribute(String key, Attributes atts) {
		return atts.getValue(key);
	}
}
