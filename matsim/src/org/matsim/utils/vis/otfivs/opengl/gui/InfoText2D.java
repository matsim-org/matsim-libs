/* *********************************************************************** *
 * project: org.matsim.*
 * InfoText2D.java
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

package org.matsim.utils.vis.otfivs.opengl.gui;

import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.media.opengl.GLAutoDrawable;
import javax.swing.JComponent;

import org.matsim.utils.vis.otfivs.opengl.gui.InfoText2D;
import org.matsim.utils.vis.otfivs.opengl.gui.TextBubble;



import com.sun.opengl.util.j2d.TextRenderer;

public  class InfoText2D extends TextBubble{
	private static List<InfoText2D> elements = new LinkedList<InfoText2D>(); 
	
	String line = null;
	int x=30,y=10;
	boolean isValid = true;
	public boolean isValid() {return isValid;};
	
	public InfoText2D(String line) {
		super(line);
		this.line = line;
	}
	
	public void render(Graphics2D g2) {
		y += 1;
		setAlpha(getAlpha() - 0.02f);
		render(g2,x,y);
       if (getAlpha() <= 0.05f) isValid = false;

	}
	
	public static void showText (String text) {
		elements.add(new InfoText2D(text));
	}

	public static void drawInfoTexts(Graphics2D g) {
		for (InfoText2D text : elements) {
			text.render(g);
		}
		
		ListIterator<InfoText2D> iter = elements.listIterator();
		while(iter.hasNext()) {
			if(!iter.next().isValid()) iter.remove();
		}
	}
}

