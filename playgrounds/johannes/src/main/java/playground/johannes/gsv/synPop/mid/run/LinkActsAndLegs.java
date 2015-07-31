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

package playground.johannes.gsv.synPop.mid.run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class LinkActsAndLegs {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml");

		Set<ProxyPerson> persons = parser.getPersons();

		ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/gis/modena/geojson/zones.de.geojson", "NO");
		List<Zone> list = new ArrayList<>(zones.zoneSet());

		long counter = 0;

		Random random = new XORShiftRandom();

		for(ProxyPerson person : persons) {
			for(ProxyPlan plan : person.getPlans()) {
				plan.setAttribute("id", String.valueOf(counter++));
				Element act = plan.getActivities().get(0);
				act.setAttribute("id", String.valueOf(counter++));
				act.setAttribute("prevAct", act.getAttribute("id"));
				act.setAttribute("nextAct", act.getAttribute("id"));

				for(int i = 0; i < plan.getLegs().size(); i++) {
					Element prevAct = plan.getActivities().get(i);
					Element leg = plan.getLegs().get(i);
					Element nextAct = plan.getActivities().get(i+1);

//					prevAct.setAttribute("id", String.valueOf(counter++));
					leg.setAttribute("id", String.valueOf(counter++));
					nextAct.setAttribute("id", String.valueOf(counter++));
					nextAct.setAttribute("prevAct", nextAct.getAttribute("id"));
					nextAct.setAttribute("nextAct", nextAct.getAttribute("id"));

					leg.setAttribute("prevAct", prevAct.getAttribute("id"));
					leg.setAttribute("nextAct", nextAct.getAttribute("id"));

					leg.setAttribute("prevType", prevAct.getAttribute("type"));
					leg.setAttribute("nextType", nextAct.getAttribute("type"));

					leg.setAttribute("fromZone", list.get(random.nextInt(list.size())).getAttribute("NO"));
					leg.setAttribute("toZone", list.get(random.nextInt(list.size())).getAttribute("NO"));
				}
			}
		}

		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.qlik.xml", persons);
	}

}
