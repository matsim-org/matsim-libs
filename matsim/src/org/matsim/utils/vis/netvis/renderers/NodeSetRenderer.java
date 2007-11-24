/* *********************************************************************** *
 * project: org.matsim.*
 * NodeSetRenderer.java
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

import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;
import org.matsim.utils.vis.netvis.visNet.DisplayNode;

/**
 * @author gunnar
 */
public class NodeSetRenderer extends RendererA {

    public static final boolean RENDER_NODES = false;

    private final DisplayNet network;

    public NodeSetRenderer(VisConfig visConfig, DisplayNet network) {
        super(visConfig);
        this.network = network;
    }

    @Override
		public void setTargetComponent(NetJComponent comp) {
        super.setTargetComponent(comp);
    }

    // -------------------- RENDERING --------------------

    @Override
		protected synchronized void myRendering(Graphics2D display,
            AffineTransform boxTransform) {

        if (!RENDER_NODES)
            return;

        final double laneWidth = DisplayLink.LANE_WIDTH * getVisConfig().getLinkWidthFactor();
        NetJComponent comp = getNetJComponent();
        AffineTransform originalTransform = display.getTransform();
        display.setStroke(new BasicStroke(Math.round(0.05 * laneWidth)));

        AffineTransform nodeTransform = new AffineTransform(originalTransform);
        nodeTransform.concatenate(boxTransform);
        display.setTransform(nodeTransform);

        for (DisplayNode node : this.network.getNodes().values()) {
            final double x = node.getEasting();
            final double y = node.getNorthing();

            if (!comp.checkLineInClip(x, y, x, y))
                continue;

            display.setColor(Color.WHITE);
            display.fillOval((int) (x - laneWidth), (int) (y - laneWidth),
                    (int) (2.0 * laneWidth), (int) (2.0 * laneWidth));

            display.setColor(Color.BLACK);
            display.drawOval((int) (x - laneWidth), (int) (y - laneWidth),
                    (int) (2.0 * laneWidth), (int) (2.0 * laneWidth));
        }

        display.setTransform(originalTransform);
    }
}
