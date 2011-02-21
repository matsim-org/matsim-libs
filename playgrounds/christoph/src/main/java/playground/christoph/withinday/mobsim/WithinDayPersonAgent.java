/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayPersonAgent.java
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

package playground.christoph.withinday.mobsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;

public class WithinDayPersonAgent extends ExperimentalBasicWithindayAgent {
	
	private ReplannerAdministrator replannerAdministrator;
	
	public WithinDayPersonAgent(Person p, Netsim simulation) {
		super(p, simulation);
		this.replannerAdministrator = new ReplannerAdministrator();
	}
			
	// If I am understanding this correctly, the replanners that are added in the following are not actively used as instances,
	// but they are used in order to identify those agents that possess those replanners.  And only those are submitted to 
	// the replanning process.
	
	// yyyy add this to a Interface WithinDayReplanable? add it via Customizable? cdobler, Oct'10
	public final ReplannerAdministrator getReplannerAdministrator() {
		return this.replannerAdministrator;
	}
	
}