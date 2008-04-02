/* *********************************************************************** *
 * project: org.matsim.*
 * DeliberateAgentFactory.java
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

/**
 * 
 */
package playground.johannes.mobsim;

import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class DeliberateAgentFactory implements MobsimAgentFactory {

	private PlanAgentFactory planAgentFactory;
	
	public List<DeliberateAgent> buildAgents() {
		List<PlanAgent> planAgents = planAgentFactory.buildAgents();
		List<DeliberateAgent> deliberateAgents = new ArrayList<DeliberateAgent>(planAgents.size());
		
		for(PlanAgent a : planAgents)
			deliberateAgents.add(new DeliberateAgent(a, null));
		
		return deliberateAgents;
	}

}
