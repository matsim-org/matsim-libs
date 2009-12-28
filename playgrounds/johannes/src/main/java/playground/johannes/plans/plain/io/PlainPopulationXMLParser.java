/* *********************************************************************** *
 * project: org.matsim.*
 * PlainPopulationXMLParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.plain.io;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

import playground.johannes.plans.plain.PlainActivity;
import playground.johannes.plans.plain.PlainLeg;
import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.PlainPlan;
import playground.johannes.plans.plain.PlainPlanElement;
import playground.johannes.plans.plain.PlainPopulation;
import playground.johannes.plans.plain.PlainPopulationBuilder;
import playground.johannes.plans.plain.PlainRoute;
import playground.johannes.plans.plain.impl.IdPool;


/**
 * @author illenberger
 *
 */
public class PlainPopulationXMLParser extends MatsimXmlParser {

	private static final String PLANS_TAG = "plans";
	
	private static final String PERSON_TAG = "person";
	
	private static final String PLAN_TAG = "plan";
	
	private static final String ACT_TAG = "act";
	
	private static final String LEG_TAG = "leg";
	
	private static final String ROUTE_TAG = "route";
	
	private static final String ID_ATTR = "id";
	
	private static final String SELECTED_ATTR = "selected";
	
	private static final String SCORE_ATTR = "score";
	
	private static final String TYPE_ATTR = "type";
	
	private static final String ENDTIME_ATTR = "end_time";
	
	private static final String MODE_ATTR = "mode";
	
	private static final String LINK_ATTR = "link";
	
	private static final String FACILITY_ATTR = "facility";
	
	private PlainPopulationBuilder builder;
	
	private PlainPopulation population;
	
	private PlainPerson person;
	
	private PlainPlan plan;
	
	private PlainLeg leg;
	
	private PlainRoute route;
	
	private List<String> activityTypes = new ArrayList<String>();
	
	private IdPool nodeIdPool = new IdPool();
	
	private IdPool linkIdPool = new IdPool();
	
	private IdPool facilityIdPool = new IdPool();
	
	private int personCount;
	
	public PlainPopulationXMLParser(PlainPopulationBuilder builder) {
		super(false);
		this.builder = builder;  
	}
	
	public PlainPopulation getPopulation() {
		return population;
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(ROUTE_TAG.equalsIgnoreCase(name) && route != null) {
			ArrayList<Id> ids = new ArrayList<Id>();
			
			for(String token : content.split(" ")) {
				token = token.trim();
				if(token.length() > 0) {
					ids.add(nodeIdPool.getId(token));
				}
			}
			ids.trimToSize();
			route.setNodeIds(ids);
			
			route = null;
			
		} else if (PERSON_TAG.equalsIgnoreCase(name)) {
			person = null;
			
			personCount++;
			if(personCount % 10000 == 0)
				System.out.println(String.format("Loaded %1$s persons...", personCount));
		} else if (PLAN_TAG.equalsIgnoreCase(name)) {
			plan = null;
		} else if (LEG_TAG.equalsIgnoreCase(name)) {
			leg = null;
		}
		
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(PLANS_TAG.equalsIgnoreCase(name)) {
			population = builder.createPopulation();
			
		} else if(PERSON_TAG.equalsIgnoreCase(name)) {
//			if(Math.random() < 0.5) {
			person = builder.createPerson(new IdImpl(atts.getValue(ID_ATTR)));
			population.addPerson(person);
			parsePersonAttributes(person, atts);
//			}
		} else if(person != null && PLAN_TAG.equalsIgnoreCase(name)) {
			plan = builder.createPlan();
			person.addPlan(plan);
			parsePlanAttributes(plan, atts);
			
		} else if(plan != null && ACT_TAG.equalsIgnoreCase(name)) {
			PlainActivity activity = builder.createActivity();
			plan.addPlanElement(activity);
			parseActAttributes(activity, atts);
			
		} else if(plan != null && LEG_TAG.equalsIgnoreCase(name)) {
			leg = builder.createLeg();
			plan.addPlanElement(leg);
			parseLegAttributes(leg, atts);
			
		} else if(leg != null && ROUTE_TAG.equalsIgnoreCase(name)) {
			route = builder.createRoute();
			leg.setRoute(route);
			parseRouteAttributes(route, atts);
			
		}	
	}
	
	protected void parsePersonAttributes(PlainPerson person, Attributes atts) {
		
	}
	
	protected void parsePlanAttributes(PlainPlan plan, Attributes atts) {
		if(Boolean.parseBoolean(atts.getValue(SELECTED_ATTR))) {
			person.setSelectedPlan(plan);
		}
		
		String value = atts.getValue(SCORE_ATTR);
		if(value != null)
			plan.setScore(new Double(value));
	}
	
	protected void parsePlanElementAttributes(PlainPlanElement element, Attributes atts) {
		String value = atts.getValue(ENDTIME_ATTR);
		if(value != null) {
			element.setEndTime(new Double(Time.parseTime(value)));
		}
	}
	
	protected void parseActAttributes(PlainActivity activity, Attributes atts) {
		parsePlanElementAttributes(activity, atts);
		
		String value = atts.getValue(TYPE_ATTR);
		if(value != null) {
			int idx = activityTypes.indexOf(value);
			if(idx > -1)
				activity.setType(activityTypes.get(idx));
			else {
				activityTypes.add(value);
				activity.setType(value);
			}
		}
		
		value = atts.getValue(LINK_ATTR);
		if(value != null) {
			activity.setLinkId(linkIdPool.getId(value));
		}
		
		value = atts.getValue(FACILITY_ATTR);
		if(value != null)
			activity.setFacilityId(facilityIdPool.getId(value));
	}
	
	protected void parseLegAttributes(PlainLeg leg, Attributes atts) {
		parsePlanElementAttributes(leg, atts);
		
		String value = atts.getValue(MODE_ATTR);
		if(value != null) {
			try{
				leg.setMode(TransportMode.valueOf(value));
			} catch (IllegalArgumentException e) {
				leg.setMode(TransportMode.undefined);
			}
		}
	}

	protected void parseRouteAttributes(PlainRoute route, Attributes atts) {
		
	}

}
