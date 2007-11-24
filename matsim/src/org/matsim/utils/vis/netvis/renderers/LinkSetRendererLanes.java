/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSetRendererLanes.java
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
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;

public class LinkSetRendererLanes extends RendererA {

	ControlToolbar toolbar = null;

    private final ValueColorizer colorizer = new ValueColorizer(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, new Color[] {
        Color.DARK_GRAY, Color.YELLOW, Color.GREEN, Color.BLUE, Color.RED });

    private DisplayNet network;

    double linkWidth;

    public LinkSetRendererLanes(VisConfig visConfig, DisplayNet network) {
        super(visConfig);
        this.network = network;

        this.linkWidth = DisplayLink.LANE_WIDTH * visConfig.getLinkWidthFactor();
    }

    @Override
		public void setTargetComponent(NetJComponent comp) {
        super.setTargetComponent(comp);
    }

    // -------------------- RENDERING --------------------

    @Override
		protected synchronized void myRendering(Graphics2D display, AffineTransform boxTransform) {
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
    		int cellWidth_m = (int)Math.round(linkWidth * link.getLanes());
    		int cellStart_m = 0;

    		for (int i = 0; i < link.getDisplayValueCount(); i++) {

    			display.setColor(colorizer.getColor(link.getLanes()));
    			display.fillRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);

    			display.setColor(Color.BLACK);
    			display.drawRect(cellStart_m, -cellWidth_m, cellLength_m, cellWidth_m);

    			cellStart_m += cellLength_m;
    		}

    	}

    	display.setTransform(originalTransform);
    }

    public void setControlToolbar(ControlToolbar toolbar) {
    	this.toolbar = toolbar;
    }

}
