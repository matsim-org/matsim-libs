/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class CopyAttributes {

	public static boolean subsample;

	public static final Logger logger = Logger.getLogger(CopyAttributes.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
//		subsample = true;
		parser.parse(args[0]);
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();

		parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
//		subsample = false;
		parser.parse(args[1]);

		Map<String, PlainPerson> templates = new HashMap<>();
		for (PlainPerson person : (Set<PlainPerson>)parser.getPersons()) {
			String id = person.getId(); // extractId(person);
			templates.put(id, person);
		}

		int cnt = 0;
		ProgressLogger.init(persons.size(), 2, 10);
		for (PlainPerson person : persons) {
//			String id = person.getId();
			String id = extractId(person);
			PlainPerson template = templates.get(id);

			if (template != null) {
				if (person.getEpisodes().size() > 1) {
					throw new RuntimeException("Person has more than one plan.");
				}
				Episode plan = person.getEpisodes().get(0);
				Episode templatePlan = template.getEpisodes().get(0);
				for (int i = 0; i < plan.getActivities().size(); i++) {
					Attributable act = plan.getActivities().get(i);
					Attributable templAct = templatePlan.getActivities().get(i);

					act.setAttribute(CommonKeys.ACTIVITY_TYPE, templAct.getAttribute(CommonKeys.ACTIVITY_TYPE));
				}
			} else {
				cnt++;
			}
			ProgressLogger.step();
		}
		
		if(cnt > 0) {
			logger.info(String.format("No template found for %s persons.", cnt));
		}

		XMLWriter writer = new XMLWriter();
		writer.write(args[2], persons);
	}

	private static String extractId(PlainPerson person) {
		String id = person.getId();
		int idx = id.indexOf("clone");
		if (idx > -1) {
			id = id.substring(0, idx);
		}
		String tokens[] = id.split("\\.");
		return String.format("%s.%s", tokens[0], tokens[1]);
	}

}