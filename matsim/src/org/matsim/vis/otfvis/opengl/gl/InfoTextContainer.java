package org.matsim.vis.otfvis.opengl.gl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.media.opengl.GLAutoDrawable;

import org.matsim.api.core.v01.Id;

public class InfoTextContainer {
	
	private static LinkedList<InfoText> elements = new LinkedList<InfoText>(); 
	private static Set<InfoText> elementsPermanent = new HashSet<InfoText>(); 

	public static void showTextOnce(String text, float x, float y, float size) {
		InfoText tt = new InfoText(text, x,y,0,size);
		tt.setDecorated(false);
		elements.addFirst(tt);
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
