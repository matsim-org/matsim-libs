/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleStaticNetLayer.java
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

package org.matsim.vis.otfvis.opengl.layer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.gl.InfoText;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;


/**
 * SimpleStaticNetLayer draws the Network.
 * 
 * It uses an OpenGL "Display List" to speed up drawing. The Display List essentially collects all draw commands from the link drawers ("items")
 * and caches them for extremely fast redrawing.
 *
 * @author dstrippgen
 *
 */
public class OGLSimpleStaticNetLayer implements GLEventListener {

	private Rect oldRect;
	private Color oldColor;
	private float oldLinkWidth;

	private int netDisplList = -1;

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {
		netDisplList = -1;
		checkNetList(glAutoDrawable);
	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		GL2 gl = (GL2) glAutoDrawable.getGL();
		OTFClientControl.getInstance().getMainOTFDrawer().setFrustrum(gl);
		float[] backgroundColor = OTFClientControl.getInstance().getOTFVisConfig().getBackgroundColor().getColorComponents(new float[4]);
		gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		float[] networkColor = OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor().getColorComponents(new float[4]);
		gl.glColor4d(networkColor[0], networkColor[1], networkColor[2], networkColor[3]);
		checkNetList(glAutoDrawable);
		gl.glCallList(netDisplList);
	}

	private void checkNetList(final GLAutoDrawable glAutoDrawable) {
		GL2 gl = (GL2) glAutoDrawable.getGL();
		final OTFVisConfigGroup config = OTFClientControl.getInstance().getOTFVisConfig();
		final Rect rect = OTFClientControl.getInstance().getMainOTFDrawer().getViewBoundsAsQuadTreeRect();

		if (oldLinkWidth != config.getLinkWidth() || !config.getNetworkColor().equals(oldColor) || !rect.equals(oldRect)) {
			// If display options have changed or if the screen area has changed, we need to recreate the display list.
			gl.glDeleteLists(netDisplList, 1);
			netDisplList = -2;
		}
		if (netDisplList < 0) {
			netDisplList = gl.glGenLists(1);
			gl.glNewList(netDisplList, GL2.GL_COMPILE);
			float[] components = config.getNetworkColor().getColorComponents(new float[4]);
			gl.glColor4d(components[0], components[1], components[2], config.getNetworkColor().getAlpha() / 255.0f);
			OTFClientControl.getInstance().getMainOTFDrawer().getQuad().execute(rect, new QuadTree.Executor<OTFDataReader>() {
				@Override
				public void execute(double x, double y, OTFDataReader object) {
					if (object instanceof OTFLinkAgentsHandler) {
						((OTFLinkAgentsHandler) object).drawLink();
					}
				}
			});
			if (config.isDrawingLinkIds() && isZoomBigEnoughForLabels(config)) {
				final Map<Coord, String> coordStringPairs = new HashMap<>();
				OTFClientControl.getInstance().getMainOTFDrawer().getQuad().execute(rect, new QuadTree.Executor<OTFDataReader>() {
					@Override
					public void execute(double x, double y, OTFDataReader object) {
						if (object instanceof OTFLinkAgentsHandler) {
							((OTFLinkAgentsHandler) object).prepareLinkId(coordStringPairs);
						}
					}
				});
				displayLinkIds(coordStringPairs, glAutoDrawable, rect);
			}
			gl.glEndList();
			oldRect = rect;
			oldLinkWidth = config.getLinkWidth();
			oldColor = config.getNetworkColor();
		}
	}

	private boolean isZoomBigEnoughForLabels(OTFVisConfigGroup config) {
		Coord size = OTFClientControl.getInstance().getMainOTFDrawer().getPixelsize();
		final double cellWidth = config.getLinkWidth();
		final double pixelsizeStreet = 5;
		return (size.getX()*pixelsizeStreet < cellWidth) && (size.getX()*pixelsizeStreet < cellWidth);
	}

	private void displayLinkIds(Map<Coord, String> linkIds, GLAutoDrawable glAutoDrawable, Rect rect) {
		TextRenderer textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 32), true, false);
		String testText = "0000000";
		Rectangle2D test = textRenderer.getBounds(testText);
		Map<Coord, Boolean> xymap = new HashMap<>(); // Why is here a Map used, and not a Set?
		double xRaster = test.getWidth(), yRaster = test.getHeight();
		for( Map.Entry<Coord, String> e : linkIds.entrySet()) {
			Coord coord = e.getKey();
			String linkId = e.getValue();
			float east = (float)coord.getX() ;
			float north = (float)coord.getY() ;
			float textX = (float) (((int)(east / xRaster) +1)*xRaster);
			float textY = north -(float)(north % yRaster) +80;
			Coord text = new Coord((double) textX, (double) textY);
			int i = 1;
			while (xymap.get(text) != null) {
				text = new Coord((double) textX, (double) (i * (float) yRaster + textY));
				if(xymap.get(text) == null) break;
				text = new Coord((double) (textX + i * (float) xRaster), (double) textY);
				if(xymap.get(text) == null) break;
				i++;
			}
			xymap.put(text, Boolean.TRUE);
			GL2 gl = glAutoDrawable.getGL().getGL2();
			gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);//Blue
			gl.glLineWidth(2);
			gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glVertex3d(east, north, 0);
			gl.glVertex3d((float) text.getX(), (float) text.getY(), 0);
			gl.glEnd();
			InfoText infoText = new InfoText(linkId, (float)text.getX(), (float)text.getY());
			infoText.draw(textRenderer, glAutoDrawable, rect);
		}
	}


	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

	}
}
