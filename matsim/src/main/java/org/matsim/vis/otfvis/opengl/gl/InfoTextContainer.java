/* *********************************************************************** *
 * project: org.matsim.*
 * InfoText.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.media.opengl.GLAutoDrawable;

import org.matsim.api.core.v01.Id;

public class InfoTextContainer {
	
	private static LinkedList<InfoText> elements = new LinkedList<InfoText>(); 
	private static Set<InfoText> elementsPermanent = new HashSet<InfoText>(); 

	public static InfoText showTextOnce(String text, float x, float y, float size) {
		InfoText tt = new InfoText(text, x,y,0,size);
		tt.setDecorated(false);
		elements.addFirst(tt);
		return tt;
	}

	public static InfoText showTextPermanent(String text, float x, float y, float size) {
		InfoText tt = null;
		tt = new InfoText(text, x,y,0, size);
		elementsPermanent.add(tt);
		return tt;
	}

	public static void removeTextPermanent(InfoText text) {
		elementsPermanent.remove(text);
	}

	public static void drawInfoTexts(GLAutoDrawable drawable, Collection<String> visibleLinkIds) {
		for (InfoText text : elements) {
			drawInfoTextIfNecessary(drawable, visibleLinkIds, text);
		}
		elements.clear();
		
		for (InfoText text : elementsPermanent) {
			drawInfoTextIfNecessary(drawable, visibleLinkIds, text);
		}
	}

	private static void drawInfoTextIfNecessary(GLAutoDrawable drawable,
			Collection<String> visibleLinkIds, InfoText text) {
		Id linkForText = text.getLinkId();
		if (linkForText == null) {
			text.draw(drawable);
		} else {
			if (visibleLinkIds.contains(linkForText.toString())) {
				text.draw(drawable);
			}
		}
	}

}
