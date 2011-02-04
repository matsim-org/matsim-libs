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

package org.matsim.vis.otfvis.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.opengl.gui.ValueColorizer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

/***
 * Drawer class for drawing agents
 */
public class SwingAgentDrawer extends OTFSwingAbstractDrawableReceiver implements OTFDataSimpleAgentReceiver{
	//Anything above 50km/h should be yellow!
	private final static ValueColorizer colorizer = new ValueColorizer(
			new double[] { 0.0, 0.3, 0.5}, new Color[] {
					Color.RED, Color.YELLOW, Color.GREEN});

	protected char[] id;
	protected float startX, startY, colorValueBetweenZeroAndOne;
	protected int state;

	@Override
	public void setAgent( AgentSnapshotInfo agInfo ) {
		this.id = agInfo.getId().toString().toCharArray();
		this.startX = (float) agInfo.getEasting() ;
		this.startY = (float) agInfo.getNorthing() ;
		this.colorValueBetweenZeroAndOne = (float) agInfo.getColorValueBetweenZeroAndOne() ;
		this.state = agInfo.getAgentState().ordinal() ;
	}

	@Override
	public void onDraw(Graphics2D display) {
		Color color = colorizer.getColor(this.colorValueBetweenZeroAndOne);
		Point2D.Float pos = new Point2D.Float(startX, startY);
		float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize();
		double offset = - 0.5 * agentSize;
		display.setColor(color);
		display.fillOval((int) Math.round(pos.x + offset), (int)Math.round(pos.y + offset), Math.round(agentSize), Math.round(agentSize));
	}

}