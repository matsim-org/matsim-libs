/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentPlanSyncView.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.david.otfvis.prefuse;

import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import playground.david.otfvis.prefuse.OTFVisDualView.PopDrawer;

public class QueryAgentPlanSyncView extends QueryAgentPlan {

	public static PopDrawer popD = null;

	@Override
	protected void onEndInit() {
		PopDrawer draw = QueryAgentPlanSyncView.popD;
		if(draw != null) {
			draw.focusOnId(this.agentId);
		}
	}
	
}

