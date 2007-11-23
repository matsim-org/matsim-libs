/* *********************************************************************** *
 * project: org.matsim.*
 * BackgroundRenderer.java
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.matsim.utils.vis.netvis.VisConfig;

public class BackgroundRenderer extends RendererA {

    public BackgroundRenderer(VisConfig visConfig) {
        super(visConfig);
    }

    // -------------------- RENDERING --------------------

    protected void myRendering(Graphics2D display, AffineTransform boxTransform) {

        Rectangle visRect = getNetJComponent().getVisibleRect();

        display.setBackground(Color.WHITE);
        display.clearRect(visRect.x, visRect.y, visRect.width, visRect.height);

        String logo = getVisConfig().getLogo();
        if (logo == null) logo = "";
        if (logo.length() > 0) {

            int targetLogoHeight = visRect.height * 8 / 10;
            int targetLogoWidth = visRect.width * 9 / 10;

            display.setFont(new Font(display.getFont().getName(), Font.BOLD,
                    targetLogoHeight));
            int trueLogoWidth = display.getFontMetrics().stringWidth(logo);

            double scaling = Math.min((double) targetLogoWidth / trueLogoWidth,
                    1);

            int scaledLogoHeight = (int)Math.round(scaling * targetLogoHeight);
            display.setFont(new Font(display.getFont().getName(), Font.PLAIN,
                    scaledLogoHeight));
            int scaledLogoWidth = display.getFontMetrics().stringWidth(logo);

            display.setColor(new Color(0.98f, 0.98f, 0.98f));

            int xCoord = visRect.x + (visRect.width - scaledLogoWidth) / 2;
            int yCoord = visRect.y + (visRect.height + scaledLogoHeight) / 2;

            display.drawString(logo, xCoord, yCoord);
        }
    }

}
