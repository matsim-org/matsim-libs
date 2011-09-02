/* *********************************************************************** *
 * project: org.matsim.*
 * Trajectory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.pysical;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author illenberger
 *
 */
public class Trajectory {

	private final List<PlanElement> elements = new ArrayList<PlanElement>(7);
	
	private final List<Double> transitions = new ArrayList<Double>(8);
	
	private final Person person;
	
	public Trajectory(Person person) {
		this.person = person;
	}
	
	public Person getPerson() {
		return person;
	}
	
	public List<PlanElement> getElements() {
		return elements;
	}
	
	public List<Double> getTransitions() {
		return transitions;
	}
	
	void addElement(PlanElement element, double endTime) {
		if(elements.isEmpty()) {
			/*
			 * This is the first element, set start time to 0.
			 */
			addElement(element, 0, endTime);
		} else {
			addElement(element, transitions.get(transitions.size() - 1), endTime);
		}
	}
	
	void addElement(PlanElement element, double startTime, double endTime) {
		if(endTime < startTime) {
			throw new RuntimeException("Start time must not be greater than end time!");
		}
		
		if(elements.isEmpty()) {
			if(element instanceof Activity) {
				elements.add(element);
				
				transitions.add(startTime);
				transitions.add(endTime);
				
			} else {
				throw new RuntimeException("First element hast to be an activity!");
			}
		} else {
			if(elements.size() % 2 == 0) {
				if(element instanceof Activity) {
					checkTimeAndInsert(element, startTime, endTime);
				} else {
					throw new RuntimeException("An activity has to follow a leg!");
				}
			} else {
				if(element instanceof Leg) {
					checkTimeAndInsert(element, startTime, endTime);
				} else {
					throw new RuntimeException("A leg has to follow an activity!");
				}
			}
		}
	}
	
	private void checkTimeAndInsert(PlanElement element, double startTime, double endTime) {
		double prevEndTime = transitions.get(transitions.size() - 1);
		if(prevEndTime == startTime) {
			elements.add(element);
			transitions.add(endTime);
		} else {
			throw new RuntimeException("Previous end time and succeeding start time have to equal!");
		}
	}
	
//	void setElements(List<PlanElement> elements) {
//		this.elements = Collections.unmodifiableList(new ArrayList<PlanElement>(elements));
//	}
//	
//	void setTransitions(List<Double> transitions) {
//		this.transitions = Collections.unmodifiableList(new ArrayList<Double>(transitions));
//	}
}
