/* *********************************************************************** *
 * project: org.matsim.*
 * MousePositionDrawer.java
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

public class MousePositionDrawer implements VisDebuggerAdditionalDrawer{

	@Override
	public void draw(VisDebugger p) {
		final double scale = p.getScale();
		p.fill(16,128);
		p.stroke(0,0);
		p.rect(10,p.getHeight()-45,120,40);
		StringBuffer tt = new StringBuffer();
		int mx = (int) (p.mouseX/scale);
		int cmx = (int) (((p.mouseX/scale) - mx)*100);
		tt.append(mx);
		tt.append(".");
		if (cmx < 10) {
			tt.append(0);
		}
		tt.append(cmx);
		tt.append(" ; ");
		int my = (int) (p.mouseY/scale);
		int cmy = (int) (((p.mouseY/scale) - my)*100);
		tt.append(my);
		tt.append(".");
		if (cmy < 10) {
			tt.append(0);
		}
		tt.append(cmy);
		
		p.fill(255);
		p.stroke(255);
		p.text(tt.toString(), 15, p.getHeight()-20);
		
	}

}
