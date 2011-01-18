/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchConditionComposite.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class SwitchConditionComposite implements EdgeSwitchCondition {

	private EdgeSwitchCondition[] components;
	
	public void setComponents(EdgeSwitchCondition[] components) {
		this.components = components;
	}
	
	@Override
	public boolean allowSwitch(AdjacencyMatrix<?> y, int i, int j, int u, int v) {
		for(int k = 0; k < components.length; k++)
			if(!components[k].allowSwitch(y, i, j, u, v))
				return false;
		
		return true;
	}

}
