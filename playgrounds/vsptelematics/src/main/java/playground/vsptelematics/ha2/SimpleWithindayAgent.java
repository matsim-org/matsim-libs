/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleWithindayAgent
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
public class SimpleWithindayAgent extends PersonDriverAgentImpl {

	
	protected SimpleWithindayAgent(Person p, Netsim simulation) {
		super(p.getSelectedPlan(), simulation);
		// (not sure if this will still work; this used to extend from EperimentalWithindayAgent. kai, feb'14)
	}

	@Override
	public Id<Link> chooseNextLinkId(){
		Id<Link> currentLinkId  = this.getCurrentLinkId();
		Id<Link> nextLink = null;
		if (currentLinkId.equals(Id.create("1", Link.class))){
			nextLink = Id.create("2", Link.class);
		}
		else if (currentLinkId.equals(Id.create("2", Link.class))){
			nextLink = Id.create("4", Link.class);
		}
		else if (currentLinkId.equals(Id.create("4", Link.class))){
			nextLink = Id.create("6", Link.class);
		}
		return nextLink;
	}

}
