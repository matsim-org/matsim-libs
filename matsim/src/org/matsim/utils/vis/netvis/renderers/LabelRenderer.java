/* *********************************************************************** *
 * project: org.matsim.*
 * LabelRenderer.java
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;
import org.matsim.utils.vis.netvis.visNet.DisplayNode;

public class LabelRenderer extends RendererA {
	private DisplayNet network;

	int textHeight;

	public LabelRenderer(VisConfig visConfig, DisplayNet network) {
		super(visConfig);
		this.network = network;
		textHeight = 12;
	}

	// -------------------- RENDERING --------------------

	@Override
	protected void myRendering(Graphics2D display, AffineTransform boxTransform) {

		AffineTransform originalTransform = display.getTransform();
		NetJComponent comp = getNetJComponent();

		/*
		 * RENDER NODE LABELS
		 */

		if (getVisConfig().showNodeLabels())
			for (DisplayNode node : network.getNodes().values()) {
				final double x = node.getEasting();
				final double y = node.getNorthing();

				if (comp.checkViewClip(x, y) != 5)
					continue;

				Point2D point = new Point2D.Double(x, y);
				// We just transform tho origin here, because we don't want the
				// text
				// to be rotated sheared etc.
				AffineTransform nodeTransform = new AffineTransform(boxTransform);
				nodeTransform.transform(point, point);

				nodeTransform = new AffineTransform(originalTransform);
				nodeTransform.translate(point.getX(), point.getY());

				display.setTransform(nodeTransform);

				display.setFont(new Font(display.getFont().getName(), Font.PLAIN, textHeight));
				display.setColor(Color.BLACK);

				String label = node.getDisplayText();
				if (label == null || "".equals(label))
					label = node.getId().toString();

				if (label != null && !"".equals(label))
					display.drawString(label, 0, 0);
			}

		display.setTransform(originalTransform);

		/*
		 * RENDER LINK LABELS
		 */

		if (getVisConfig().showLinkLabels())
			for (DisplayableLinkI link : network.getLinks().values()) {

				// We don't use *0.5 here to have texts for two-directions not
				// written over each other
				double xpos = link.getStartEasting() + (link.getEndEasting() - link.getStartEasting()) * .42;
				double ypos = link.getStartNorthing() + (link.getEndNorthing() - link.getStartNorthing()) * .42;

				if (comp.checkViewClip(xpos, ypos) != 5) {
					continue;
				}

				Point2D point = new Point2D.Double(xpos, ypos);
				// We just transform tho origin here, because we don't want the
				// text
				// to be rotated sheared etc.
				AffineTransform linkTransform = new AffineTransform(boxTransform);
				linkTransform.transform(point, point);

				AffineTransform linkTransform2 = new AffineTransform(originalTransform);

				linkTransform2.translate(point.getX(), point.getY());

				double dx = link.getEndEasting() - link.getStartEasting();
				double dy = link.getEndNorthing() - link.getStartNorthing();
				double theta = Math.atan2(dx, dy);
				if (theta <= 0)
					linkTransform2.rotate(theta + Math.PI / 2.);
				else
					linkTransform2.rotate(theta - Math.PI / 2.);

				display.setTransform(linkTransform2);

				display.setFont(new Font(display.getFont().getName(), Font.PLAIN, textHeight));
				display.setColor(Color.BLACK);

				String label = link.getDisplayText();

				if (label == null || "".equals(label)) {
					label = link.getId().toString();
				}

				if (label != null && !"".equals(label)) {
					final int textWidth = display.getFontMetrics().stringWidth(label);
					final int yoffset = (theta <= 0) ? -textHeight / 2 : (int) textHeight;
					display.drawString(label, -textWidth / 2, yoffset);
				}
			}

		display.setTransform(originalTransform);
	}

}
