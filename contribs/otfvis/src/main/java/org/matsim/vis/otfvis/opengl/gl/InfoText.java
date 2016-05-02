/* *********************************************************************** *
 * project: org.matsim.*
 * InfoText.java
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

package org.matsim.vis.otfvis.opengl.gl;

import java.awt.Color;
import java.io.Serializable;

import com.jogamp.opengl.GLAutoDrawable;

import org.matsim.core.utils.collections.QuadTree.Rect;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * InfoText is the class behind all text displayed in the OPenGL context.
 * Texts can be either drawn for one tick only or reside permanent until removal.
 * 
 * @author dstrippgen
 *
 */
public class InfoText implements Serializable {

	private static final long serialVersionUID = 1L;

	private String text;
	private float x;
	private float y;
	private Color color = new Color(50, 50, 128, 200);

	public InfoText(String text, float x, float y) {
		this.text = text;
		this.x = x;
		this.y = y;
	}

	public void draw(TextRenderer renderer, GLAutoDrawable drawable, Rect rect) {
        int width = (int) (rect.maxX - rect.minX);
        int height = (int) (rect.maxY - rect.minY);
        int screenWidth = drawable.getSurfaceWidth();
        float widthRatio = (float) screenWidth / (float) width;
        int screenHeight = drawable.getSurfaceHeight();
        float heightRatio = (float) screenHeight / (float) height;
        renderer.beginRendering(screenWidth, screenHeight);
		renderer.setColor(color.getRed()/255.f, color.getGreen()/255.f, color.getBlue()/255.f, color.getAlpha()/255.f);
		renderer.draw(text, (int) ((x - rect.minX) * widthRatio) , (int) ((y - rect.minY) * heightRatio));
		renderer.endRendering();
	}

	public void setAlpha(float alpha) {
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha*255));
	}

	public void setColor(Color color) {
		this.color = color;
	}

}

