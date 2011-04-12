/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.bvg09.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.filters.AbstractPersonFilter;

/**
 * @author droeder
 *
 */
public class PlanElementFilter extends AbstractPersonFilter{
	private Map<Id, ArrayList<PlanElement>> elements;
	
	public PlanElementFilter(){
		this.elements = new HashMap<Id, ArrayList<PlanElement>>();
	}
	
	@Override
	public void run(Person p){
		if(judge(p)){
			this.count();
		}
	}
	
	public Map<Id, ArrayList<PlanElement>> getElements(){
		return this.elements;
	}
	@Override
	public boolean judge(Person person) {

		if(this.containsPT(person) && this.moreThanOneElement(person)){
			this.elements.put(person.getId(), (ArrayList<PlanElement>) person.getSelectedPlan().getPlanElements());
			return true;
		}else{
			return false;
		}
	}

	private boolean containsPT(Person person) {
		boolean pt = false;
		for(PlanElement pe: person.getSelectedPlan().getPlanElements()){
			if(pe instanceof Leg && (((Leg) pe).getMode().equals(TransportMode.pt) || ((Leg) pe).getMode().equals(TransportMode.transit_walk)) ){
				pt = true;
			}
		}
		return pt;
	}
	
	private boolean moreThanOneElement(Person person){
		if(person.getSelectedPlan().getPlanElements().size() > 1){
			return true;
		}else{
			return false;
		}
	}

}
