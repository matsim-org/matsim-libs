/* *********************************************************************** *
 * project: org.matsim.*
 * TestProcessing.java
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
package playground.benjamin.processing;

import processing.core.PApplet;

/**
 * @author benjamin
 *
 */
public class TestProcessing extends PApplet{

	@Override
	public void setup() {
		size(200,200);
		background(0);
	}

	@Override
	public void draw() {
		stroke(255);
		if (mousePressed) {
			line(mouseX,mouseY,pmouseX,pmouseY);
		}
	}

	public static void main(String[] args) {
		PApplet.main(new String[] {"--present", "playground.benjamin.processing.TestProcessing"});
	}
}