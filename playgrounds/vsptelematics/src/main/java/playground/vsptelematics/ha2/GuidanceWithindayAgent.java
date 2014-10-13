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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;


/**
 * @author dgrether
 *
 */
public class GuidanceWithindayAgent extends PersonDriverAgentImpl {

	private Id<Link> id1 = Id.create("1", Link.class);
	private Id<Link> id2 = Id.create("2", Link.class);
	private Id<Link> id3 = Id.create("3", Link.class);
	private Id<Link> id4 = Id.create("4", Link.class);
	private Id<Link> id5 = Id.create("5", Link.class);
	private Id<Link> id6 = Id.create("6", Link.class);
	private Guidance guidance;
	private Netsim simulation;
	
	protected GuidanceWithindayAgent(Person p, Netsim simulation, Guidance guidance) {
		super(p.getSelectedPlan(), simulation);
		// (not sure if this will work; this class used to extend from ExperimentalWithindayAgent. kai, feb'14)

		this.simulation = simulation;
		this.guidance = guidance;
	}

	@Override
	public Id<Link> chooseNextLinkId(){
		double time = this.simulation.getSimTimer().getTimeOfDay();
		Id<Link> currentLinkId  = this.getCurrentLinkId();
		Id<Link> nextLink = null;
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
