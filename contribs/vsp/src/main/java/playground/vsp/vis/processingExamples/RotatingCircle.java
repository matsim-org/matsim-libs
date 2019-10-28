/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.vsp.vis.processingExamples;

import processing.core.PApplet;

/**
 * @author nagel
 *
 */
public class RotatingCircle extends PApplet {
	private final int xsize=600, ysize=600 ;

	private int ii =0 ;

	public static void main( String[] args ) {
		PApplet.main( new String[] { "--present", "playground.vsp.vis.processingExamples.RotatingCircle"}  );
	}

	@Override
	public void draw() {
		this.strokeWeight(20) ;
		this.stroke( 255, 0, 0, 111) ; // R G B transparency
		
		this.background(255) ; // "clears" the background

		ii++ ;
		float xx = xsize/2 + ( xsize/2*(float) Math.sin(ii/90.) );
		float yy = ysize/2 + ( ysize/2*(float) Math.cos(ii/90.) );
		this.point( xx, yy ) ;
		
	}
	
	@Override
	public void settings() { // setup does not work here when not using the PDE
		size(xsize,ysize );
	}

}
