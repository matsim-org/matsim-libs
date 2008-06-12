/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimResourceTest.java
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

package org.matsim.gbl;

import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;

import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class MatsimResourceTest extends MatsimTestCase {
	
	public final void testGetAsImage() {
		final Image logo = MatsimResource.getAsImage("matsim_logo_transparent.png");
		
		// make sure the image is really loaded.
    MediaTracker mediaTracker = new MediaTracker(new Container());
    mediaTracker.addImage(logo, 0);
    try {
			mediaTracker.waitForID(0);
    } catch (InterruptedException e) {
    	throw new RuntimeException(e);
    }
		// verify that the correct image was correctly loaded by testing its dimension
		assertEquals(256, logo.getWidth(null));
		assertEquals(48, logo.getHeight(null));
	}
	
}
