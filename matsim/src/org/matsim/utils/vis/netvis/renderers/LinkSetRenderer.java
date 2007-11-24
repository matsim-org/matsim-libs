/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRenderer.java
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
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;

public class LinkSetRenderer extends RendererA {

    private final boolean RANDOMIZE_LANES = false;

    private final boolean RENDER_CELL_CONTOURS = true;

    private final ValueColorizer colorizer = new ValueColorizer();

    private final DisplayNet network;

    private double laneWidth;

    public LinkSetRenderer(VisConfig visConfig, DisplayNet network) {
        super(visConfig);
        this.network = network;

        this.laneWidth = DisplayLink.LANE_WIDTH
                * visConfig.getLinkWidthFactor();
    }

    @Override
		public void setTargetComponent(NetJComponent comp) {
        super.setTargetComponent(comp);
    }

    // -------------------- RENDERING --------------------

    @Override
		protected synchronized void myRendering(Graphics2D display,
            AffineTransform boxTransform) {
        String test = getVisConfig().get("ShowAgents");
        boolean drawAgents = test == null || test.equals("true");
        this.laneWidth = DisplayLink.LANE_WIDTH
                * getVisConfig().getLinkWidthFactor();

        NetJComponent comp = getNetJComponent();

        AffineTransform originalTransform = display.getTransform();

        display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));

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
            linkTransform.concatenate(link.getLinear2PlaneTransform());

            /*
             * (1) RENDER LINK
             */

            display.setTransform(linkTransform);

            final int lanes = link.getLanes();
            final int cellLength_m = (int)Math.round(link.getLength_m()
                    / link.getDisplayValueCount());
            final int cellWidth_m = (int)Math.round(laneWidth * lanes);
            int cellStart_m = 0;

            for (int i = 0; i < link.getDisplayValueCount(); i++) {

                display.setColor(colorizer.getColor(link.getDisplayValue(i)));

                display.fillRect(cellStart_m, -cellWidth_m, cellLength_m,
                        cellWidth_m);

                if (RENDER_CELL_CONTOURS) {
                    display.setColor(Color.BLACK);
                    display.drawRect(cellStart_m, -cellWidth_m, cellLength_m,
                            cellWidth_m);
                }

                cellStart_m += cellLength_m;
            }

            display.setColor(Color.BLACK);
            display.drawRect(0, -cellWidth_m, cellLength_m
                    * link.getDisplayValueCount(), cellWidth_m);

            /*
             * (2) RENDER VEHICLES
             *
             * IMPORTANT: If you modify this, ensure proper rendering of agents
             * on multi-lane links!
             */

            final double agentWidth = laneWidth;
            final double agentLength = agentWidth;

            final boolean flip = (link.getStartEasting() <= link
                    .getEndEasting());

            if (flip) {
                AffineTransform flipTransform = AffineTransform
                        .getScaleInstance(1, -1);
                linkTransform.concatenate(flipTransform);
                display.setTransform(linkTransform);
            }

            if (link.getMovingAgents() != null && drawAgents)
            	for (DrawableAgentI agent : link.getMovingAgents()) {
                    final int lane = (RANDOMIZE_LANES ? (agent.hashCode()
                            % lanes + 1) : agent.getLane());

                    final int x = (int)Math.round(agent.getPosInLink_m() - 0.5
                            * agentLength);

                    final int y = (int)Math.round(agentWidth
                            * ((flip ? lanes : 0) - lane));

                    display.setColor(Color.BLUE);
                    display.fillOval(x, y, (int)Math.round(agentLength), (int)Math.round(agentWidth));
                }
        }

        display.setTransform(originalTransform);
    }
}
