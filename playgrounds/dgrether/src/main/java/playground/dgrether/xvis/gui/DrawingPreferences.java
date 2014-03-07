/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.gui;

import java.awt.Color;

import org.apache.log4j.Logger;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.ScaleEvent;


public class DrawingPreferences {
	
	private static final Logger log = Logger.getLogger(DrawingPreferences.class);
	
	private Color backgroundColor;
	private Color linkColor;
	private float scale = - 0.5f;
	private Color greenColor;
	private Color redYellowColor;
	private Color yellowColor;
	private Color redColor;
	private Color selectionColor;
	private float realScale = 0;

	private boolean showLaneIds = false;

	private boolean showLinkIds = false;

	private boolean showLink2LinkLines = false;

	public DrawingPreferences(){
		this.backgroundColor = new Color(0, 0, 0);
		this.linkColor = new Color(255, 255, 200);
		this.redColor = new Color(255, 0, 0);
		this.redYellowColor = new Color(255, 200, 0);
		this.yellowColor = new Color(255, 255, 0);
		this.selectionColor = new Color(230, 40, 230);
		this.calcRealScale();
	}

	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	public Color getLinkColor(){
		return this.linkColor;
	}
	
	public float getRealScale() {
		return this.realScale;
	}

	private void calcRealScale(){
		this.realScale = (float) Math.exp(this.scale);
//		log.debug("scale: " + this.scale + " real: " + this.realScale);
	}
	
	private void calcScale(){
		this.scale = (float) Math.log(this.realScale);
		log.error("scale: " + this.scale + " real: " + this.realScale);
	}
	
	public void incrementScale(float increment) {
		this.scale += increment;
		this.calcRealScale();
		XVisControl.getInstance().getControlEventsManager().fireScaleEvent(new ScaleEvent(this.realScale));
	}

	
	public void setRealScale(float realScale) {
		this.realScale = realScale;
		this.calcScale();
	}

	public Color getGreenColor() {
		return this.greenColor;
	}
	
	public Color getRedYellowColor() {
		return this.redYellowColor;
	}
	public Color getYellowColor() {
		return this.yellowColor;
	}
	public Color getRedColor() {
		return this.redColor;
	}


	public Color getSelectionColor() {
		return this.selectionColor;
	}


	public boolean isShowLaneIds() {
		return this.showLaneIds;
	}
	
	public boolean isShowLinkIds() {
		return this.showLinkIds;
	}

	public void setShowLaneIds(boolean showLaneIds) {
		this.showLaneIds = showLaneIds;
	}
	
	public void setShowLinkIds(boolean showLinkIds){
		this.showLinkIds = showLinkIds;
	}


	public boolean isShowLink2LinkLines() {
		return this.showLink2LinkLines;
	}
	
	public void setShowLink2LinkLines(boolean b){
		this.showLink2LinkLines = b;
	}
	
	


	
}
