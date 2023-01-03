/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.bvgAna.anaLevel1.personId2DelayAtStop;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;

/**
 * Simple data container to store departure time and agent enters vehicle time for each agent at different stops.
 * 
 * @author aneumann
 *
 */
public class PersonId2DelayAtStopData {
	
	private final Id personId;

	// Could be double[] ???
	private ArrayList<Double> agentDepartsPTInteraction = new ArrayList<Double>();
	private ArrayList<Double> agentEntersVehicle = new ArrayList<Double>();
	
	public PersonId2DelayAtStopData(Id personId){
		this.personId = personId;
	}
	
	/**
	 * Get departure time sorted by occurrence for each departs pt interaction event.
	 * 
	 * @return A list containing the departure time 
	 */
	public ArrayList<Double> getAgentDepartsPTInteraction() {
		return this.agentDepartsPTInteraction;
	}

	/**
	 * Get enter time sorted by occurrence for each agent enters a vehicle event.
	 * 
	 * @return A list containing the enter time 
	 */
	public ArrayList<Double> getAgentEntersVehicle() {
		return this.agentEntersVehicle;
	}	
	
	@Override
	public String toString() {
		return "Person: " + this.personId + ", # left pt interaction: " + this.agentDepartsPTInteraction.size() + ", # vehicle entered: " + this.agentEntersVehicle.size();
	}

	public void addAgentDepartureEvent(PersonDepartureEvent event) {
		this.agentDepartsPTInteraction.add(new Double(event.getTime()));		
	}

	public void addPersonEntersVehicleEvent(PersonEntersVehicleEvent event) {
		this.agentEntersVehicle.add(new Double(event.getTime()));		
	}

}
