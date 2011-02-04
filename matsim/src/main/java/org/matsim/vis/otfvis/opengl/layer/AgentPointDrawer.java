/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.opengl.layer;

import java.awt.Color;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

/**
 * yyyy Despite its name, this is not a Drawer but a Receiver. It receives agents 
 * (as declared by its interface but not by its name) and pushes them towards the AgentPointLayer.  
 * That AgentPointLayer has its own (Array)Drawer.  The class here cannot be renamed because of the 
 * ConnectionManager/mvi issue.  kai, feb'11
 */
public class AgentPointDrawer implements OTFDataSimpleAgentReceiver {

	private final OGLAgentPointLayer oglAgentPointLayer;
	
	private static OTFOGLDrawer.FastColorizer redToGreenColorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 50.}, new Color[] {Color.RED, Color.YELLOW, Color.GREEN});

	AgentPointDrawer(OGLAgentPointLayer oglAgentPointLayer) {
		this.oglAgentPointLayer = oglAgentPointLayer;
	}

	@Override
	public void setAgent( AgentSnapshotInfo agInfo ) {
		char[] id = agInfo.getId().toString().toCharArray();
		
		if ( OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme().equalsIgnoreCase( OTFVisConfigGroup.COLORING_BVG ) ) {

			if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.DARK_GRAY, true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
			} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
				String idstr = agInfo.getId().toString();
				if ( idstr.contains("line_B")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.MAGENTA, true);
				} else if ( idstr.contains("line_T")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.RED, true);
				} else if ( idstr.contains("line_S")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.GREEN, true);
				} else if ( idstr.contains("line_U")) {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.BLUE, true);
				} else {
					this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
				}
			} else {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.YELLOW, true);
			}
		} else {
		
			if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), redToGreenColorizer.getColorZeroOne(agInfo.getColorValueBetweenZeroAndOne()), true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.ORANGE, true);
			} else if ( agInfo.getAgentState()==AgentState.PERSON_OTHER_MODE ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.MAGENTA, true);
			} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.BLUE, true);
			} else {
				this.oglAgentPointLayer.addAgent(id, (float)agInfo.getEasting(), (float)agInfo.getNorthing(), Color.YELLOW, true);
			}

		}

	}

	@Override
	public void invalidate(SceneGraph graph) {
		
	}

}