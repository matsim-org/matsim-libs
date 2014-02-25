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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
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
	public Id chooseNextLinkId(){
		Id currentLinkId  = this.getCurrentLinkId();
		Id nextLink = null;
		if (currentLinkId.equals(new IdImpl("1"))){
			nextLink = new IdImpl("2");
		}
		else if (currentLinkId.equals(new IdImpl("2"))){
			nextLink = new IdImpl("4");
		}
		else if (currentLinkId.equals(new IdImpl("4"))){
			nextLink = new IdImpl("6");
		}
		return nextLink;
	}

}
