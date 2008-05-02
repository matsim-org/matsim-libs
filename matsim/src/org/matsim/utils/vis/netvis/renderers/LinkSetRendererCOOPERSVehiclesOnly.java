/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererCOOPERSVehiclesOnly.java
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

package org.matsim.utils.vis.netvis.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;

public class LinkSetRendererCOOPERSVehiclesOnly extends RendererA {

	private final boolean RANDOMIZE_LANES = true;

	private final ValueColorizer colorizer = new ValueColorizer();

	private final DisplayNet network;

	private double laneWidth;

	private BufferedImage image;

	public LinkSetRendererCOOPERSVehiclesOnly(VisConfig visConfig, DisplayNet network) {
		super(visConfig);
		this.network =  network;

		this.laneWidth = DisplayLink.LANE_WIDTH
		* visConfig.getLinkWidthFactor();
	}

	private final boolean redrawLanes = false;

	public BufferedImage captureScreen(Graphics2D display) {
		Component target = getNetJComponent();
		Rectangle rect = getNetJComponent().getVisibleRect();

		image = new BufferedImage((int)rect.getWidth(), (int)rect.getHeight()+40, BufferedImage.TYPE_INT_RGB);
		Graphics2D imdisplay = image.createGraphics();
		imdisplay.fillRect( 0, 0, (int)rect.getWidth() - 1, (int)rect.getHeight() - 1 );
		imdisplay.setTransform(display.getTransform());
		target.paint(imdisplay);

		return image;
	}

	@Override
	public void setTargetComponent(NetJComponent comp) {
		super.setTargetComponent(comp);
	}

	// -------------------- RENDERING --------------------

	private void renderLinks(Graphics2D display,
			AffineTransform boxTransform) {

		NetJComponent comp = getNetJComponent();
		AffineTransform originalTransform = display.getTransform();

		for (DisplayableLinkI link : network.getLinks().values()) {

			if (!comp.checkLineInClip(link.getStartEasting(), link
					.getStartNorthing(), link.getEndEasting(), link
					.getEndNorthing())) {
				continue;
			}

			if (link.getStartEasting() == link.getEndEasting()
					&& link.getStartNorthing() == link.getEndNorthing())
				continue;

			AffineTransform linkTransform = new AffineTransform(
			originalTransform);
			linkTransform.concatenate(boxTransform);

			display.setTransform(linkTransform);
			Polygon poly = new Polygon();
	        double dy = link.getEndEasting() - link.getStartEasting();
	        double dx = link.getEndNorthing() - link.getStartNorthing();
	        double sqr = Math.sqrt(dx*dx +dy*dy);
			final double cellWidth_m = laneWidth * link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);

			dx = dx*cellWidth_m/sqr;
	        dy = -dy*cellWidth_m/sqr;

			poly.addPoint((int)(link.getStartEasting()-dx), (int)(link.getStartNorthing()-dy));
			poly.addPoint((int)(link.getStartEasting()+dx), (int)(link.getStartNorthing()+dy));
			poly.addPoint((int)(link.getEndEasting()+dx), (int)(link.getEndNorthing()+dy));
			poly.addPoint((int)(link.getEndEasting()-dx), (int)(link.getEndNorthing()-dy));
			display.setColor(Color.WHITE);
			display.fill(poly);
			display.setColor(Color.BLUE);
			display.draw(poly);
		}
	}

	static int ii= 0;
	@Override
	protected synchronized void myRendering(Graphics2D display,
			AffineTransform boxTransform) {
		String test = getVisConfig().get("ShowAgents");
		boolean drawAgents = test == null || test.equals("true");
		this.laneWidth = DisplayLink.LANE_WIDTH* getVisConfig().getLinkWidthFactor();

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();

		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));
		display.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON));

		if (redrawLanes) {
			renderLinks(display, boxTransform);
		} else {
			//Gbl.startMeasurement();
			renderLinks(display, boxTransform);
			display.setTransform(new AffineTransform());
			display.drawImage(image, null, 0, 0);
			display.setTransform(originalTransform);

			for (DisplayableLinkI link : network.getLinks().values()) {
				if (!comp.checkLineInClip(link.getStartEasting(), link
						.getStartNorthing(), link.getEndEasting(), link
						.getEndNorthing())) {
					continue;
				}

				if (link.getStartEasting() == link.getEndEasting()
						&& link.getStartNorthing() == link.getEndNorthing())
					continue;

				AffineTransform linkTransform = new AffineTransform(
						originalTransform);
				linkTransform.concatenate(boxTransform);
				AffineTransform lin2trans =  link.getLinear2PlaneTransform();
				if (lin2trans != null) linkTransform.concatenate(lin2trans);
				display.setTransform(linkTransform);

				final int lanes = link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);
				/*
				 * (2) RENDER VEHICLES
				 *
				 * IMPORTANT: If you modify this, ensure proper rendering of angents
				 * on multi-lane links!
				 */

				 final double agentWidth = laneWidth *1.2;
				 final double agentLength = agentWidth*1.2;

				 if (link.getMovingAgents() != null && drawAgents)
					 for (DrawableAgentI agent : link.getMovingAgents()) {

						 final int lane = (RANDOMIZE_LANES ? (agent.hashCode()
								 % lanes + 1) : agent.getLane());

						 final double  x = agent.getPosInLink_m();
						 final double offsetX = - 0.5 * agentLength;

						 final int y = (int)Math.round(agentWidth*(lane-lanes));

						 // there is only ONE displayvalue!
						 if (agent.getLane() <= 0) {
							 display.setColor(Color.BLUE);
						 } else {
							 display.setColor(Color.GREEN);
							 display.setColor(colorizer.getColor(0.1 + 0.9*link.getDisplayValue(0)));
						 }
						 display.fillOval((int)Math.round(x + offsetX), y, (int)Math.round(agentLength),
								 (int)Math.round(agentWidth));

					 }
			}
		}

		display.setTransform(originalTransform);
		//Gbl.printElapsedTime();
	}


	ControlToolbar toolbar = null;

	public void setControlToolbar(ControlToolbar toolbar) {
		this.toolbar = toolbar;
	}

}
