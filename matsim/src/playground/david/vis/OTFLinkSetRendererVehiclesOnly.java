/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererVehiclesOnly.java
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

package playground.david.vis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.renderers.ValueColorizer;

import playground.david.vis.interfaces.OTFAgentHandler;
import playground.david.vis.interfaces.OTFParamProvider;

public class OTFLinkSetRendererVehiclesOnly extends RendererA {

	private final boolean RANDOMIZE_LANES = false;

	private final boolean RENDER_CELL_CONTOURS = true;

	private final ValueColorizer colorizer = new ValueColorizer();

	private final OTFVisNet network;

	private final double laneWidth;

	private BufferedImage image;
	private final AffineTransform boxTransformOLD = new AffineTransform();
	private final AffineTransform displayTransformOLD = new AffineTransform();

	public OTFLinkSetRendererVehiclesOnly(OTFVisNet network) {
		super();
		this.network =  network;

		this.laneWidth = OTFVisNet.Link.laneWidth;
	}

	private final boolean redrawLanes = true;

	public BufferedImage captureScreen(Graphics2D display) {
		Component target = getNetJComponent();
		Rectangle rect = getNetJComponent().getVisibleRect();
		Rectangle rect2 = getNetJComponent().getParent().getBounds();

		image = new BufferedImage((int)rect.getWidth(), (int)rect.getHeight()+40, BufferedImage.TYPE_INT_RGB);
		Graphics2D imdisplay = image.createGraphics();
		imdisplay.fillRect( 0, 0, (int)rect.getWidth() - 1, (int)rect.getHeight() - 1 );
		imdisplay.setTransform(display.getTransform());
		target.paint(imdisplay);

		return image;
	}

	// -------------------- RENDERING --------------------
	private void renderLink(OTFVisNet.Link link, Graphics2D display, AffineTransform linkTransform) {

		final int NodeWidth = 20;
		final int lanes = link.getLanes();
		final int cellLength_m = (int)Math.round(link.getLength_m());
		final int cellWidth_m = (int)Math.round(laneWidth * lanes);
		int cellStart_m = 0;

		display.setColor(Color.WHITE);
		if (network.lastLink == link) {
			display.setColor(Color.lightGray);
		}

		display.fillRect(cellStart_m + NodeWidth, -cellWidth_m, cellLength_m - NodeWidth,
				cellWidth_m);

		display.setColor(Color.BLACK);
		display.drawRect(0 + NodeWidth, -cellWidth_m, cellLength_m-NodeWidth, cellWidth_m);
//		if (getVisConfig().showLinkLabels()) {
		display.setFont(display.getFont().deriveFont((float)(cellWidth_m*0.5)));
		display.drawString(link.getDisplayText(), (int)(0.5*cellLength_m), -(int)(cellWidth_m*0.5));
//		}

//		if (isOTF) {
//		if (((OTFVisNet)network).lastLink == link) {
//		display.setColor(Color.RED);
//		display.fillRect(cellStart_m, -cellWidth_m, cellLength_m,
//		cellWidth_m);
//		display.drawRect(0, -cellWidth_m, cellLength_m
//		* link.getDisplayValueCount(), cellWidth_m);
//		}
//		}

	}

	private void renderLinks(Graphics2D display,
			AffineTransform boxTransform) {

		NetJComponent comp = getNetJComponent();
		AffineTransform originalTransform = display.getTransform();

		Iterator<OTFVisNet.Link> it = null;
		it = network.getLinks().iterator();

		for (; it.hasNext();)
		{
			OTFVisNet.Link link =  it.next();

			(link).setVisible(false);


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
			final double cellWidth_m = laneWidth * link.getLanes();

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
//			if (isOTF) {
//			((OTFVisNet.Link)link).setVisible(true);
//			}

//			AffineTransform lin2trans =  link.getLinear2PlaneTransform();
//			if (lin2trans != null) linkTransform.concatenate(lin2trans);
//			/*
//			* (1) RENDER LINK
//			*/
//			display.setTransform(linkTransform);
//			renderLink(link, display, linkTransform);
		}
	}

	//Anything above 50km/h should be yellow!
	private final ValueColorizer colorizer2 = new ValueColorizer(
			new double[] { 0.0, 30., 50.}, new Color[] {
					Color.RED, Color.YELLOW, Color.GREEN});

	private void drawAgent( Graphics2D display, OTFParamProvider agent) {

		int xPosIndex = agent.getIndex("PosX"); 
		int yPosIndex = agent.getIndex("PosY"); 
		int colorIndex = agent.getIndex("Color"); 
		int state = agent.getIntParam(agent.getIndex("State")); 

		Color color = colorizer2.getColor(0.1 + 0.9*agent.getFloatParam(colorIndex));
		if ((state & 1) != 0) color = Color.lightGray;

		Point2D.Float pos = new Point2D.Float(agent.getFloatParam(xPosIndex), agent.getFloatParam(yPosIndex));
		// draw agent...
//		final int lane = (RANDOMIZE_LANES ? (agent.hashCode()
//		% lanes + 1) : agent.getLane());


		final double agentWidth = laneWidth *0.9;
		final double agentLength = agentWidth*0.9;
		final double offsetX = - 0.5 * agentLength;

		// there is only ONE displayvalue!
		if (state == 1 ) {
			display.setColor(Color.gray);
		} else {
			display.setColor(color);
		}

		display.fillOval((int)Math.round(pos.x + offsetX), (int)pos.y, (int)Math.round(agentLength), (int)Math.round(agentWidth));
		String id = agent.getStringParam(4);

		if (network.selectedAgents.contains(id)) {
			// Also: Draw a circle around the selected agent
			display.setColor(Color.BLUE);
			int circleWidth = (int)(2.* agentWidth);
			display.drawArc(Math.round(pos.x-circleWidth), (int)Math.round(pos.y-circleWidth+0.5*agentWidth), 2*circleWidth, 2*circleWidth, 0, 360);
		}

	}


	static int ii= 0;
	@Override
	protected synchronized void myRendering(Graphics2D display,
			AffineTransform boxTransform) {
		boolean drawAgents = true;

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();

		display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));
		//display.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON));


//		if (redrawLanes) {
//			renderLinks(display, boxTransform);
//			redrawLanes = false;
//		}
		
		Gbl.startMeasurement();
		renderLinks(display, boxTransform);
		display.setTransform(new AffineTransform());
		display.drawImage(image, null, 0, 0);
		display.setTransform(originalTransform);

		AffineTransform linkTransform = new AffineTransform(
				originalTransform);
		linkTransform.concatenate(boxTransform);
		display.setTransform(linkTransform);

		// now go through links to draw agents
		for (OTFAgentHandler agent : network.getAgents()) {
			OTFParamProvider agentP = (OTFParamProvider)agent;
			String id = agentP.getStringParam(4);

			drawAgent(display,agentP);

		}



		display.setTransform(originalTransform);
		Gbl.printElapsedTime();
	}


	ControlToolbar toolbar = null;

	public void setControlToolbar(ControlToolbar toolbar) {
		this.toolbar = toolbar;
	}

}
