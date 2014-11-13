/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author anhorni
 */
public class Memorizer implements ShutdownListener {	
	private AgentMemories memories;
	private String day;
	
	public Memorizer(AgentMemories memories, String day) {
		this.memories = memories;
		this.day = day;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		Controler controler = event.getControler();
        Population population = controler.getScenario().getPopulation();
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (this.memories.getMemory(person.getId()) == null) {
				this.memories.addMemory(person.getId(), new AgentMemory());
			}			
			this.memories.getMemory(person.getId()).addPlan(plan, this.day);
		}		
	}

	public AgentMemories getMemories() {
		return memories;
	}

	public void setMemories(AgentMemories memories) {
		this.memories = memories;
	}
	
	public void setDay(String day) {
		this.day = day;
	}
}