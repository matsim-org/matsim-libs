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
package playground.kai.usecases.processing;

import processing.core.PApplet;

/**
 * @author nagel
 *
 */
public class RotatingCircle extends PApplet {
	private final int xsize=500, ysize=500 ;

	private int ii =0 ;

	public static void main( String[] args ) {
		PApplet.main( new String[] { "--present", "testProcessing.MyPApplet"}  );
	}

	@Override
	public void draw() {
		this.strokeWeight(10) ;

		background(255) ;
		ii++ ;
		float xx = xsize/2 + ( xsize/2*(float) Math.sin(ii/360.) );
		float yy = ysize/2 + ( ysize/2*(float) Math.cos(ii/360.) );
		this.point( xx,yy ) ;


	}

	@Override
	public void settings() {
		size(xsize,ysize );
	}

}
