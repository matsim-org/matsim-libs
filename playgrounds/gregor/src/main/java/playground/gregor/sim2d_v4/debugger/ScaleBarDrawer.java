/* *********************************************************************** *
 * project: org.matsim.*
 * ScaleBarDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.debugger;

public class ScaleBarDrawer implements VisDebuggerAdditionalDrawer {

	@Override
	public void draw(VisDebugger p) {
		int w = p.getWidth();
		int h = p.getHeight();
		float scale = p.getScale();
		float width = w / scale;
		if (width > 1000) {
			draw(p,300,300*scale,h,w);
		} else if (width > 500) {
			draw(p,200,200*scale,h,w);
		} else if (width > 250) {
			draw(p,100,100*scale,h,w);
		} else if (width > 100) {
			draw(p,30,30*scale,h,w);
		} else if (width > 50) {
			draw(p,20,20*scale,h,w);
		} else if (width > 25) {
			draw(p,10,10*scale,h,w);
		}else if (width > 10) {
			draw(p,3,3*scale,h,w);
		} else if (width > 5) {
			draw(p,2,2*scale,h,w);
		} else {
			draw(p,1,1*scale,h,w);
		}
	}

	private void draw(VisDebugger p, int meter, float pixel, int h, int w) {
		
		p.fill(16, 128);
		p.stroke(0, 0);
		
		p.rect(w-30-pixel, h-45, pixel+20, 40);
		
		p.fill(255);
		p.stroke(255);
		p.strokeWeight(2);
		p.line(w-20-pixel, h-20, w-20, h-20);
		p.line(w-20-pixel, h-10, w-20-pixel, h-30);
		p.line(w-20, h-10, w-20, h-30);
		p.text(meter + " meter",w-20-pixel/2, h-30);
		
	}

}
