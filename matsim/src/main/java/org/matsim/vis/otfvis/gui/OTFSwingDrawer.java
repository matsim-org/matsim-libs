/* *********************************************************************** *
 * project: org.matsim.*
 * NetJComponent.java
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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;

/**
 * @author david
 */
abstract class OTFSwingAbstractDrawableReceiver implements OTFDrawable, OTFDataReceiver{

	@Override
	public final void draw() {
		onDraw(OTFSwingDrawer.g2d);
	}

	abstract protected void onDraw(Graphics2D g2d);

	@Override
	public void invalidate(SceneGraph graph) {
		graph.addItem(this);
	}
}

/**
 * The class implements the Component for SWING based drawing of the OTFVis.
 * This version of the OTFVis does not support all possible features implemented in the OpenGL-based version.
 *
 * @author dstrippgen
 */
public class OTFSwingDrawer extends JComponent {
	public static Graphics2D g2d = null;

	double scale = 1;

	private static final long serialVersionUID = 1L;

	private static final float linkWidth = 100;
	private static final float strokeWidth = Math.round(0.05 * linkWidth);

	private final int frameDefaultWidth;

	private final int frameDefaultHeight;

	private final OTFClientQuad quad;

	private OTFQueryHandler queryHandler;

	OTFHostControlBar hostControlBar;

	private OTFSwingDrawerContainer parentDrawer;

	// --------------- CONSTRUCTION ---------------

	public OTFSwingDrawer(OTFClientQuad quad, OTFHostControlBar hostControlBar, OTFSwingDrawerContainer parentDrawer) {
		this.quad = quad;
		this.parentDrawer = parentDrawer;
		// calculate size of frame

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double factor = screenSize.getWidth() / networkClippingWidth();
		factor = Math.min(factor, screenSize.getHeight() / networkClippingHeight());
		factor *= 0.8f;

		frameDefaultWidth = (int) Math.floor(networkClippingWidth() * factor);
		frameDefaultHeight = (int) Math.floor(networkClippingHeight() * factor);

		scale(1);
		this.hostControlBar = hostControlBar;
	}

	void scale(double factor) {
		if (factor > 0) {
			this.scale = factor;
			int scaledWidth = (int) Math.round(factor * frameDefaultWidth);
			int scaledHeight = (int) Math.round(factor * frameDefaultHeight);
			this.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
		}
	}

	// -------------------- COORDINATE TRANSFORMATION --------------------

	private double networkClippingMaxEasting() {
		return quad.getMaxEasting() -quad.getMinEasting() + 1;
	}

	private double networkClippingMaxNorthing() {
		return quad.getMaxNorthing() - quad.getMinNorthing() + 1;
	}

	private double networkClippingWidth() {
		return networkClippingMaxEasting() - (0 - 1);
	}

	private double networkClippingHeight() {
		return networkClippingMaxNorthing() - (0 - 1);
	}


	public float getScale() {
		return (float) scale;
	}

	AffineTransform getBoxTransform() {

		// two original extreme coordinates ...

		double v1 = 0 - 1;
		double w1 = 0 - 1;

		double v2 = networkClippingMaxEasting();
		double w2 = networkClippingMaxNorthing();

		// ... mapped onto two extreme picture coordinates ...

		Dimension prefSize = this.getPreferredSize();

		double x1 = 0;
		double y1 = (int) prefSize.getHeight();

		double x2 = (int) prefSize.getWidth();
		double y2 = 0;

		// ... yields a simple affine transformation without shearing:

		double m00 = (x1 - x2) / (v1 - v2);
		double m02 = x1 - m00 * v1;

		double m11 = (y1 - y2) / (w1 - w2);
		double m12 = y1 - m11 * w1;

		return new AffineTransform(m00, 0.0, 0.0, m11, m02, m12);
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		parentDrawer.mouseMan.drawElements(g2);
		OTFSwingDrawer.g2d = g2;
		g2.setStroke(new BasicStroke(strokeWidth));
		g2.transform(getBoxTransform());
		SceneGraph sceneGraph = quad.getSceneGraph(hostControlBar.getOTFHostControl().getSimTime(), null, parentDrawer);
		sceneGraph.draw();
		if (this.queryHandler != null) {
			this.queryHandler.drawQueries(parentDrawer);
		}
	}

	public OTFClientQuad getQuad() {
		return quad;
	}

	public void clearCache() {
		if(quad != null) quad.clearCache();
	}

	public void handleClick(Double point, int mouseButton, MouseEvent e) {
		Point2D.Double origPoint = new Point2D.Double(point.x + this.quad.offsetEast, point.y + this.quad.offsetNorth);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.quad.getId(), origPoint, mouseButton);
	}

	public void handleClick(Rectangle currentRect, int button) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(currentRect.x + this.quad.offsetEast, currentRect.y + this.quad.offsetNorth, currentRect.width, currentRect.height);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.quad.getId(), origRect, button);
	}

	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}

}