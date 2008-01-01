/* *********************************************************************** *
 * project: org.matsim.*
 * BarChartTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.charts;

import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;

import org.matsim.testcases.MatsimTestCase;

/**
 * Test for {@link XYLineChart}
 *
 * @author mrieser
 */
public class XYLineChartTest extends MatsimTestCase {

	/**
	 * Test that a file was really generated, and that the image, when loaded, has the specified size.
	 */
	public void testXYLineChartDemo() {
		String imagefile = getOutputDirectory() + "xylinechart.png";
		Demo demo = new Demo();
		demo.createXYLineChart(imagefile);

		assertTrue(new File(imagefile).exists());

		Image image = Toolkit.getDefaultToolkit().getImage(imagefile);
		// make sure the image is really loaded.
    MediaTracker mediaTracker = new MediaTracker(new Container());
    mediaTracker.addImage(image, 0);
    try {
			mediaTracker.waitForID(0);
			assertEquals(800, image.getWidth(null));
			assertEquals(600, image.getHeight(null));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
