/* *********************************************************************** *
 * project: org.matsim.*
 * InfoBoc.java
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import processing.core.PImage;

public class Branding implements VisDebuggerAdditionalDrawer, VisDebuggerOverlay{

	private PImage logo;
	private PImage contact;

	@Override
	public void draw(EventsBasedVisDebugger p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void drawText(EventsBasedVisDebugger p) {
		if (this.logo == null){
			this.logo = p.loadImage("/Users/laemmel/devel/fzj/logo/black_300dpi.png");
			this.contact = p.loadImage("/Users/laemmel/devel/fzj/logo/contact2.png");
			
		}
		float ts = 18;
		String ext = "q/2d extension";
		int tx = p.width-this.logo.width-15;
		int ty = p.height-this.logo.height-15;
		int w = this.logo.width;
		int h = this.logo.height;
		p.strokeWeight(2);
		p.fill(0,0,0,235);
		p.stroke(222,222,222,255);
		
		int round = 10;
		p.rect(tx-5, ty-5, w+10, h+10,round);
		p.image(this.logo,tx, ty, w, h);
//		
		p.image(this.contact,5,p.height-this.contact.height-5,this.contact.width,this.contact.height);
		
//		float x = 5 + round;
//		float y = 5 + round + ts;
//		p.textSize(ts);	
////		PVector cv = p.zoomer.getCoordToDisp(new PVector((float)(li.tx+p.offsetX),(float)-(li.ty+p.offsetY)));
////		p.fill(0,0,0,255);
////		float w = p.textWidth(li.text);
//		
//		double t = p.time;
//		String tm = Time.writeTime(t, Time.TIMEFORMAT_HHMMSS);
//		String stm = "time: " + tm;
//		float w = p.textWidth(stm);
//		
//		p.fill
//		p.textSize(ts);
//		p.stroke(0,235);
//		float tw = p.textWidth("contact: g.laemmel@fz-juelich.de");
//		System.out.println(tw);
//		p.rect(5, p.height-5-ts, tw, ts,round/2);
//		p.pushMatrix();
//		p.translate(ts/2+5+5, p.height -tw/2-5-5-5);
//		p.rotate((float) -Math.PI/2);
//		p.textAlign(PConstants.LEFT) ;
		
		
//		p.fill(255);
//		p.textAlign(PConstants.CENTER, PConstants.CENTER);

		
//		p.text("contact: g.laemmel@fz-juelich.de", 5,p.height-5-2.5f);
//		p.popMatrix();
//		p.fill(255);
//		p.textAlign(PConstants.LEFT);
//		p.text(stm, x, y);
//		double sph = this.speedup > .98 ? Math.round(this.speedup) : this.speedup;
////		String tt = Integer.toString(ttt);
////		String dec = Integer.toString((int)((this.speedup-ttt)*100));
//		p.text("speedup: " + sph , x, y+ts+ts/2);
//		p.text("fps: " + (int)(p.frameRate+.5) , x, y+ts+ts/2 + ts + ts/2);

	}


}
