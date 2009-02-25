/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.population;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.LocationType;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.BasicAct;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.BasicPopulationBuilder;
import org.matsim.interfaces.basic.v01.BasicRoute;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.routes.LinkCarRoute;

/**
 * @author dgrether
 */
public class PopulationBuilderImpl implements BasicPopulationBuilder {

	private static final Logger log = Logger
			.getLogger(PopulationBuilderImpl.class);
	
	private Population population;
	private NetworkLayer network;
	private Facilities facilities;

	public PopulationBuilderImpl(NetworkLayer network, Population population, Facilities facilities) {
		this.network = network;
		this.population = population;
		this.facilities = facilities;
	}

	public BasicAct createAct(BasicPlan basicPlan, String currentActType,
			BasicLocation currentlocation) {
		Act act = null;
		if (currentlocation != null) {
			if (currentlocation.getCenter() != null) {
				act = ((Plan)basicPlan).createAct(currentActType, currentlocation.getCenter());
			}
			
			if (currentlocation.getId() != null){
				if (currentlocation.getLocationType() == LocationType.FACILITY) {
					Facility fac = facilities.getFacilities().get(currentlocation.getId());
					if (act == null) {
						act = ((Plan)basicPlan).createAct(currentActType, fac);
					}
					else {
						act.setFacility(fac);
					}
				}
				else if (currentlocation.getLocationType() == LocationType.LINK) {
					Link link = this.network.getLink(currentlocation.getId());
					if (act == null) {
						act = ((Plan)basicPlan).createAct(currentActType, link);
					}
					else {
						act.setLink(link);
					}
				}
			}
		}
		else {
			StringBuilder builder = new StringBuilder();
			builder.append("Act number: ");
			builder.append(((Plan)basicPlan).getActsLegs().size());
			builder.append(" of Person Id: " );
			builder.append(((Plan)basicPlan).getPerson().getId());
			builder.append(" has no location information. This is not possible to prevent by the XML Grammar used, however it should result in incorrect behaviour of the framework. Only use with expert knowledge!");
			log.warn(builder.toString());
		}
		return act;
	}

	public BasicLeg createLeg(BasicPlan basicPlan, Mode legMode) {
		return ((Plan)basicPlan).createLeg(legMode);
	}

	public BasicPerson createPerson(Id id) {
		Person p = new PersonImpl(id);
		this.population.addPerson(p);
		return p;
	}

	public BasicPlan createPlan(BasicPerson person, boolean selected) {
		Person p = (Person) person;
		return p.createPlan(false);
	}

	public BasicPlan createPlan(BasicPerson currentPerson) {
		return createPlan(currentPerson, false);
	}

	public BasicRoute createRoute(Id startLinkId, Id endLinkId, final List<Id> currentRouteLinkIds) {
		Link start = network.getLink(startLinkId);		
		Link end = network.getLink(endLinkId);
		if (start == null) {
			throw new IllegalStateException("Cann't create Route with StartLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		if (end == null) {
			throw new IllegalStateException("Cann't create Route with EndLink Id " + startLinkId + " because the Link cannot be found in the loaded Network.");
		}
		CarRoute route = new LinkCarRoute(start, end);
		List<Link> links = new ArrayList<Link>();
		Link link;
		for (Id id : currentRouteLinkIds) {
			link = this.network.getLink(id);
			if (link == null) {
				throw new IllegalStateException("Cann't create Route over Link with Id " + id + " because the Link cannot be found in the loaded Network.");
			}
			links.add(this.network.getLink(id));
		}
		route.setLinks(start, links, end); 
		return route;
	}



	public BasicActivity createActivity(String type, BasicLocation loc) {
		Activity act = null;
		if (loc != null) {
			if ((loc.getLocationType() == LocationType.FACILITY) && this.facilities.getFacilities().containsKey(loc.getId())) {
				Facility fac = this.facilities.getFacilities().get(loc.getId());
				act = new Activity(type, fac);				
				return act;
			}
			throw new IllegalArgumentException("No facility exists with id: " + loc.getId());
		}
		throw new IllegalArgumentException("Can't create facility without location");
	}

	public BasicKnowledge createKnowledge(List<BasicActivity> acts) {
		Knowledge knowledge = null;
		if ((acts != null) && !acts.isEmpty()) {
			knowledge = new Knowledge();
			for (BasicActivity a : acts) {
				knowledge.addActivity((Activity)a);
			}
			return knowledge;
		}
		throw new IllegalArgumentException("Knowledge must contain at least one Activity!");
	}

//	public BasicAct createAct(BasicPlan plan, String string, Coord coord) {
//		
//		// TODO Auto-generated method stub
//		return null;
//	}

}
