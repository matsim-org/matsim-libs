/* *********************************************************************** *
 * project: org.matsim.*
 * TimeDependentTrigger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.snapshots.writers;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;


public class TimeDependentTrigger extends OTFGLAbstractDrawableReceiver {

	public OTFTimeDependentDrawer myDrawer;
	int time;
	

	@Override
	public void onDraw(GL gl) {
		this.myDrawer.onDraw(gl,this.time);
		
	}

	public void setTime(double time2) {
		this.time = (int) time2;
		
	}
	
	public void setDrawer(OTFTimeDependentDrawer drawer) {
		this.myDrawer = drawer;
	}

}
