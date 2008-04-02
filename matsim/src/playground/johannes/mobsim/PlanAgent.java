/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;

/**
 * @author illenberger
 *
 */
public class PlanAgent implements MobsimAgent {
	
	private final Person person;

	private int currentPlanIndex;
	
	private int currentRouteIndex;
	
	private Link currentLink;
	
	public PlanAgent(Person person) {
		this.person = person;
	}
	
	public Link getLink() {
		return currentLink;
//		int index = currentPlanIndex;
//		if(index % 2 != 0) {
//			index++;
//		}
//		
//		return ((Act)person.getSelectedPlan().getActsLegs().get(index)).getLink();
	}

	public double getDepartureTime(double time) {
		int index = currentPlanIndex;
		if(index % 2 != 0) {
			index++;
		}
		
		if(isDone())
			return Double.MAX_VALUE;
		else
			return ((Act)person.getSelectedPlan().getActsLegs().get(index)).getEndTime();
			
	}

	public String getNextMode(double time) {
		int index = currentPlanIndex;
		if(index % 2 == 0) {
			if(index < person.getSelectedPlan().getActsLegs().size() - 1)
				index++;
			else
				return null;
			
		}
		
		return ((Leg)person.getSelectedPlan().getActsLegs().get(index)).getMode();
	}

	public Link getNextLink(double time) {
		/*
		 * We need the link-based route implementation here!
		 */
		return null;
	}

	public void beforeSim() {
		currentRouteIndex = -1;
		currentPlanIndex = 0;
		
		Act act = (Act)person.getSelectedPlan().getActsLegs().get(currentPlanIndex);
		act.setStartTime(0);
		if (act.getEndTime() != Time.UNDEFINED_TIME && act.getDur() != Time.UNDEFINED_TIME) {
			act.setEndTime(Math.min(act.getEndTime(), act.getDur()));
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			act.setEndTime(act.getDur());
		}
		
		currentLink = act.getLink();
	}

	public boolean isDone() {
		if(currentPlanIndex < person.getSelectedPlan().getActsLegs().size() - 1)
			return false;
		else
			return true;
	}

	public void arrival(double time) {
		Leg leg = (Leg)person.getSelectedPlan().getActsLegs().get(currentPlanIndex);
		leg.setArrTime(time);
		leg.setTravTime(leg.getArrTime() - leg.getDepTime());
		currentRouteIndex = -1;
		currentPlanIndex++;
		
		Act act = (Act)person.getSelectedPlan().getActsLegs().get(currentPlanIndex);
		act.setStartTime(time);
		if (act.getEndTime() != Time.UNDEFINED_TIME && act.getDur() != Time.UNDEFINED_TIME) {
			act.setEndTime(Math.min(act.getEndTime(), time + act.getDur()));
		} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
			act.setEndTime(time + act.getDur());
		}
	}

	public void departure(double time) {
		Act act = (Act)person.getSelectedPlan().getActsLegs().get(currentPlanIndex);
		act.setEndTime(time);
		act.setDur(act.getEndTime() - act.getStartTime());
		currentPlanIndex++;
		
		Leg leg = (Leg)person.getSelectedPlan().getActsLegs().get(currentPlanIndex);
		leg.setDepTime(time);
		currentRouteIndex = 0;
	}

	public void enterLink(Link link, double time) {
		/*
		 * TODO: Catch OutOfBoundsException!
		 */
		currentLink = link;
		currentRouteIndex++;
		Link desiredLink = ((Leg)person.getSelectedPlan().getActsLegs().get(currentPlanIndex)).getRoute().getLinkRoute()[currentRouteIndex];
		if(currentLink != desiredLink)
			currentRouteIndex--;
	}
	
//	public Link getCurrentLink() {
//		return currentLink;
//	}

	public Person getPerson() {
		return person;
	}

	public IdI getId() {
		// TODO Auto-generated method stub
		return null;
	}
}
