/* *********************************************************************** *
 * project: org.matsim.*
 * OTFAgentRenderer.java
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
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.renderers.RendererA;
import org.matsim.utils.vis.netvis.renderers.ValueColorizer;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;

class OTFAgentRenderer extends RendererA {

	private final boolean RANDOMIZE_LANES = true;

	private final boolean RENDER_CELL_CONTOURS = true;

	private final ValueColorizer colorizer = new ValueColorizer();

	private final OTFVisNet network;

	private double laneWidth;

	private BufferedImage image;
	private final AffineTransform boxTransformOLD = new AffineTransform();
	private final AffineTransform displayTransformOLD = new AffineTransform();

	private Plan plan = null;

	OTFAgentRenderer(final VisConfig visConfig, final OTFVisNet network) {
		super(visConfig);
		this.network =  network;
		this.laneWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
	}

	// -------------------- RENDERING --------------------
	static int ii= 0;
	@Override
	synchronized public void myRendering(final Graphics2D display,
			final AffineTransform boxTransform) {
		String test = getVisConfig().get("ShowAgents");
		boolean drawAgents = test == null || test.equals("true");
		this.laneWidth = DisplayLink.LANE_WIDTH
		* getVisConfig().getLinkWidthFactor();

		NetJComponent comp = getNetJComponent();

		AffineTransform originalTransform = display.getTransform();
		 final double agentWidth = this.laneWidth *0.9;

		display.setStroke(new BasicStroke(Math.round(0.5 * this.laneWidth)));

		AffineTransform myTransform = new AffineTransform(originalTransform);
		myTransform.concatenate(boxTransform);
		display.setTransform(myTransform);
		AffineTransform myTextTransform = new AffineTransform(myTransform);
		myTextTransform.scale(1,-1);

		if (this.plan != null) {
			List actslegs = this.plan.getActsLegs();
			for (int i= 0; i< actslegs.size(); i++) {
				if (i%2 == 0) {
					// handle Act
					Act act = (Act)actslegs.get(i);
					OTFVisNet.Link link = this.network.getLink(act.getLinkId().toString());
					display.setColor(Color.RED);
					OTFVisNet.Node node = link.getFromNode();
					OTFVisNet.Node lastNode = link.getToNode();
					if (lastNode != null){
						display.drawLine((int)node.getEasting(), (int)node.getNorthing(), (int)lastNode.getEasting(), (int)lastNode.getNorthing());
						display.setTransform(myTextTransform);
						display.drawString(act.getType(), (int)node.getEasting(), -(int)node.getNorthing());
						display.setTransform(myTransform);
					}
				} else {
					// ahndle leg
					Leg leg = (Leg)actslegs.get(i);
					List route = leg.getRoute().getRoute();
					OTFVisNet.Node lastNode = null;
					for (int j = 1; j < route.size()-1; j++) {
						String id = (String) route.get(j);
						OTFVisNet.Node node = this.network.getNode(id);
						// Draw circle around node
						display.setColor(Color.BLUE);
						int circleWidth = (int)(2.* agentWidth);
						//display.drawArc(Arithm.round(node.getEasting()-circleWidth), Arithm.round(node.getNorthing()-circleWidth+0.5*agentWidth), 2*circleWidth, 2*circleWidth, 0, 360);
						if (lastNode != null) display.drawLine((int)node.getEasting(), (int)node.getNorthing(), (int)lastNode.getEasting(), (int)lastNode.getNorthing());
						lastNode = node;
					}

				}
			}
		}


		display.setTransform(originalTransform);
	}


	ControlToolbar toolbar = null;

	public void setControlToolbar(final ControlToolbar toolbar) {
		this.toolbar = toolbar;
	}

	public Plan getPlan() {
		return this.plan;
	}

	public void setPlan(final Plan plan) {
		this.plan = plan;
	}

}
