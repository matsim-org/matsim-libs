/* *********************************************************************** *
 * project: org.matsim.*
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;

/***
 * Drawer class for drawing simple quads
 */
public class SwingSimpleQuadDrawer extends OTFSwingAbstractDrawableReceiver implements OTFDataQuadReceiver{
	protected final float[] line = new float[5];
	protected String id = "noId";
	
	@Override
	public void setQuad(float startX, float startY, float endX, float endY) {
		setQuad(startX, startY,endX, endY, 1);
	}

	@Override
	public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
		this.line[0] = startX;
		this.line[1] = startY;
		this.line[2] = endX;
		this.line[3] = endY;
		this.line[4] = nrLanes;
	}

	@Override
	public void setColor(float coloridx) {
		
	}

	@Override
	public void onDraw(Graphics2D display) {
		display.setColor(new Color(OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor().getRGB()));
		display.setStroke(new BasicStroke(this.line[4] * OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		display.drawLine((int) this.line[0], (int) this.line[1], (int) this.line[2], (int) this.line[3]);
		display.setStroke(new BasicStroke(5));


		// Show LinkIds
		if (OTFClientControl.getInstance().getOTFVisConfig().isDrawingLinkIds()){
		    float idSize = 4*OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		    int fontSize = (int)idSize;
		    float middleX = (float)(0.5*this.line[0] + (0.5)*this.line[2]);
		    float middleY = (float)(0.5*this.line[1] + (0.5)*this.line[3]);
			Line2D line = new Line2D.Float(middleX, middleY, (middleX + idSize),(middleY + idSize));
			display.setColor(Color.blue);
			display.draw(line);
			java.awt.Font font_old = display.getFont();
			AffineTransform tx = new AffineTransform(1,0,0,-1,0,0);
			display.transform(tx);
			java.awt.Font font = new java.awt.Font("Arial Unicode MS", java.awt.Font.PLAIN, fontSize);
			display.setFont(font);
			display.drawString(this.id,(float)(middleX + 1.25*idSize),-(float)(middleY + 0.75*idSize));
			try {
				tx.invert();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			display.transform(tx);
			display.setFont(font_old);
		}

	}

	@Override
	public void setId(char[] idBuffer) {
		this.id = String.valueOf(idBuffer);
	}
}