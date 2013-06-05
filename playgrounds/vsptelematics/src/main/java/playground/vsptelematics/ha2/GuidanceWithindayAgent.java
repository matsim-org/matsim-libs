/* *********************************************************************** *
 * project: org.matsim.*
 * GuidanceWithindayAgent
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
package playground.vsptelematics.ha2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;


/**
 * @author dgrether
 *
 */
public class GuidanceWithindayAgent extends ExperimentalBasicWithindayAgent {

	private Id id1 = new IdImpl("1");
	private Id id2 = new IdImpl("2");
	private Id id3 = new IdImpl("3");
	private Id id4 = new IdImpl("4");
	private Id id5 = new IdImpl("5");
	private Id id6 = new IdImpl("6");
	private Guidance guidance;
	private Netsim simulation;
	
	protected GuidanceWithindayAgent(Person p, Netsim simulation, Guidance guidance) {
		super(p, simulation);
		this.simulation = simulation;
		this.guidance = guidance;
	}

	@Override
	public Id chooseNextLinkId(){
		double time = this.simulation.getSimTimer().getTimeOfDay();
		Id currentLinkId  = this.getCurrentLinkId();
		Id nextLink = null;
		if (currentLinkId.equals(id1)){
			nextLink = this.guidance.getNextLink(time);
		}
		else if (currentLinkId.equals(id2)){
			nextLink = id4;
		}
		else if (currentLinkId.equals(id3)){
			nextLink = id5;
		}
		else if (currentLinkId.equals(id4) || currentLinkId.equals(id5)){
			nextLink = id6;
		}
		return nextLink;
	}
	
}
