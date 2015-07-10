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

import java.util.Set;

import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

/**
 * @author johannes
 *
 */
public class LinkActsAndLegs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml");
		
		Set<ProxyPerson> persons = parser.getPersons();

		long counter = 0;
		
		for(ProxyPerson person : persons) {
			for(ProxyPlan plan : person.getPlans()) {
				plan.setAttribute("id", String.valueOf(counter++));
				ProxyObject act = plan.getActivities().get(0); 
				act.setAttribute("id", String.valueOf(counter++));
				act.setAttribute("prevAct", act.getAttribute("id"));
				act.setAttribute("nextAct", act.getAttribute("id"));
				
				for(int i = 0; i < plan.getLegs().size(); i++) {
					ProxyObject prevAct = plan.getActivities().get(i);
					ProxyObject leg = plan.getLegs().get(i);
					ProxyObject nextAct = plan.getActivities().get(i+1);
					
//					prevAct.setAttribute("id", String.valueOf(counter++));
					leg.setAttribute("id", String.valueOf(counter++));
					nextAct.setAttribute("id", String.valueOf(counter++));
					nextAct.setAttribute("prevAct", nextAct.getAttribute("id"));
					nextAct.setAttribute("nextAct", nextAct.getAttribute("id"));
					
					leg.setAttribute("prevAct", prevAct.getAttribute("id"));
					leg.setAttribute("nextAct", nextAct.getAttribute("id"));
					
					leg.setAttribute("prevType", prevAct.getAttribute("type"));
					leg.setAttribute("nextType", nextAct.getAttribute("type"));
				}
			}
		}
		
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.qlik.xml", persons);
	}

}
