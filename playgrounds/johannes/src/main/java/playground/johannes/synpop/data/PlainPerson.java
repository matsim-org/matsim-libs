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

package playground.johannes.synpop.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class PlainPerson extends PlainElement implements Person {

	private String id;
	
//	private PlainEpisode plan;
	
	private List<Episode> plans = new ArrayList<Episode>();
	
	public PlainPerson(String id) {
		this.id = id;		
	}
	
	public String getId() {
		return id;
	}
	
	public void setPlan(Episode plan) {
//		this.plan = plan;
		if(plans.isEmpty())
			addEpisode(plan);
		else {
			plans.set(0, plan);
			((PlainEpisode)plan).setPerson(this);
		}
	}
	
	public void addEpisode(Episode plan) {
		plans.add(plan);
		((PlainEpisode)plan).setPerson(this);
	}

	public void removeEpisode(Episode episode) {
		plans.remove(episode);
		((PlainEpisode)episode).setPerson(null);
	}

	public Episode getPlan() {
//		return plan;
		return plans.get(0);
	}
	
	public List<? extends Episode> getEpisodes() {
		return plans;
	}
	
	public PlainPerson clone() {
		return cloneWithNewId(id);
	}
	
	public PlainPerson cloneWithNewId(String newId) {
		PlainPerson clone = new PlainPerson(newId);
		
		for(Entry<String, String> entry : getAttributes().entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
//		clone.setPlan(plan.clone());
		for(Episode plan : plans)
			clone.addEpisode(((PlainEpisode) plan).clone());
		
		return clone;
	}
}
