/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererVolumes.java
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
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;

public class LinkSetRendererVolumes extends RendererA {

	ControlToolbar toolbar = null;

  static double flowCapFactor = 0.10;
  static double timeFactor = 1.0;
  static double netCapFactor = flowCapFactor * timeFactor;

    private final ValueColorizer colorizer = new ValueColorizer(new double[] { 0.0, 1000.0 * netCapFactor, 2000.0 * netCapFactor, 3000.0 * netCapFactor, 4000.0 * netCapFactor }, new Color[] {
        Color.WHITE, Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE });

    private DisplayNet network;

    double linkWidth;

    public LinkSetRendererVolumes(VisConfig visConfig, DisplayNet network) {
        super(visConfig);
        this.network = network;

        this.linkWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
    }

    // -------------------- RENDERING --------------------

    @Override
		protected void myRendering(Graphics2D display, AffineTransform boxTransform) {
    	String test = getVisConfig().get("ShowAgents");
    	boolean drawAgents = test == null || test.equals("true");
        this.linkWidth = DisplayLink.LANE_WIDTH* getVisConfig().getLinkWidthFactor();


        NetJComponent comp = getNetJComponent();

        AffineTransform originalTransform = display.getTransform();

        double scale = 1.0;
        if (this.toolbar != null) {
        	scale = this.toolbar.getScale();
        }

        //display.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double strokeWidth = 0.1 * linkWidth;
        if (strokeWidth < (20.0 / scale)) strokeWidth = 20.0 / scale; // min pixel-width on screen
        if (strokeWidth > (100.0 / scale)) strokeWidth = 100.0 / scale;  // max pixel-width on screen
        display.setStroke(new BasicStroke((float)strokeWidth));

        for (DisplayableLinkI link : network.getLinks().values()) {
            if (!comp.checkLineInClip(link.getStartEasting(), link.getStartNorthing(),
            		link.getEndEasting(), link.getEndNorthing())) {
            	continue;
            }

            AffineTransform linkTransform = new AffineTransform(originalTransform);
            linkTransform.concatenate(boxTransform);
            linkTransform.concatenate(link.getLinear2PlaneTransform());

            /*
             * (1) RENDER LINK
             */

            display.setTransform(linkTransform);

            int cellLength_m = (int)Math.round(link.getLength_m() / link.getDisplayValueCount());
            int cellWidth_m = (int)Math.round(linkWidth * (link.getDisplayValue(0) / 180.0));
            int cellStart_m = 0;

            for (int i = 0; i < link.getDisplayValueCount(); i++) {

                display.setColor(colorizer.getColor(link.getDisplayValue(i)));
                display.fillRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);

                display.setColor(Color.BLACK);
                display.drawRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);

                cellStart_m += cellLength_m;
            }

            /*
             * (2) RENDER VEHICLES
             */
            double agentWidth = linkWidth*.75;
            double agentLength = agentWidth;
            // 7.5 * getVisConfig().getLinkWidthFactor();

            // ====================

            // <--- generate transform ----|
            boolean isFlipped = false;
            if (link.getStartEasting() <= link.getEndEasting()) {
            	AffineTransform flipTransform = AffineTransform.getScaleInstance(1,-1);
            	linkTransform.concatenate(flipTransform);
            	display.setTransform(linkTransform);
            	isFlipped = true;
            }

            if (link.getMovingAgents() != null && drawAgents)
            		for (DrawableAgentI agent : link.getMovingAgents()) {

                    // |--- generate transform --->

                    // if (link.getStartEasting() <= link.getEndEasting()) {
                    // AffineTransform agentFlipTransform = AffineTransform
                    // .getTranslateInstance(0, -0.25f
                    // * (linkWidth + agentWidth));
                    // agentFlipTransform.concatenate(AffineTransform
                    // .getScaleInstance(1, -1));
                    // agentFlipTransform.concatenate(AffineTransform
                    // .getTranslateInstance(0,
                    // 0.25f * (linkWidth + agentWidth)));
                    //
                    // display.getTransform().concatenate(agentFlipTransform);
                    // }


                    int x = (int)Math.round(agent.getPosInLink_m() - 0.5
                            * agentLength);
                    int y = 0 + (int)(agent.getLane()*agentWidth);
                    if (!isFlipped) y = (int)Math.round(-0.5 * (agentWidth + linkWidth));

                    display.setColor(Color.BLUE);
                    display.fillOval(x, y, (int)Math.round(agentLength), (int)Math.round(agentWidth));
                }
        }

        display.setTransform(originalTransform);
    }

    public void setControlToolbar(ControlToolbar toolbar) {
    	this.toolbar = toolbar;
    }

}
