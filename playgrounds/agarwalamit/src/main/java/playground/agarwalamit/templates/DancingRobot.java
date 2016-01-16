/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.templates;

import processing.core.PApplet;

/**
 * Taken from 
 */

public class DancingRobot extends PApplet {
	int armAngle = 0;
	int angleChange = 5;
	final int ANGLE_LIMIT = 135;

	public static void main(String[] args) {
		PApplet.main(new String [] {"--present","playground.agarwalamit.templates.DancingRobot"});
	}

	@Override
	public void draw() {
		this.translate(this.displayWidth/2, this.displayHeight/2);
		background(255);
		pushMatrix();
		translate(50, 50); // place robot so arms are always on screen
		drawRobot();
		armAngle += angleChange;

		// if the arm has moved past its limit,
		// reverse direction and set within limits.
		if (armAngle > ANGLE_LIMIT || armAngle < 0)
		{
			angleChange = -angleChange;
			armAngle += angleChange;
		}
		popMatrix();
	}

	@Override
	public void settings() { // setup does not work here when not using the PDE
		size(this.displayWidth, this.displayHeight );
		smooth();
//		frameRate(30); // using this throws NPE
	}
	void drawRobot()
	{
		noStroke();
		fill(38, 38, 200);
		rect(20, 0, 38, 30); // head
		rect(14, 32, 50, 50); // body
		drawLeftArm();
		drawRightArm();
		rect(22, 84, 16, 50); // left leg
		rect(40, 84, 16, 50); // right leg

		fill(222, 222, 249);
		ellipse(30, 12, 12, 12); // left eye
		ellipse(47, 12, 12, 12);  // right eye
	}

	void drawLeftArm()
	{
		pushMatrix();
		translate(12, 32);
		rotate(radians(armAngle));
		rect(-12, 0, 12, 37); // left arm
		popMatrix();
	}

	void drawRightArm()
	{
		pushMatrix();
		translate(66, 32);
		rotate(radians(-armAngle));
		rect(0, 0, 12, 37); // right arm
		popMatrix();
	}
}
