/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversWriterTest.java
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

package org.matsim.contrib.freightReceiver;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freightReceiver.run.chessboard.ReceiverChessboardScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ReceiversWriterTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testV1() {
		Scenario sc = ReceiverChessboardScenario.createChessboardScenario(1L, 5, utils.getOutputDirectory(), false );
		ReceiverUtils.getReceivers(sc).getAttributes().putAttribute("date",
				new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format( Calendar.getInstance().getTime()));

		/* Now the receiver is 'complete', and we can write it to file. */
		try {
			new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).writeV1(utils.getOutputDirectory() + "receivers_v1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should write without exception.");
		}

		Assert.assertTrue("File should exist.", new File(utils.getOutputDirectory() + "receivers_v1.xml").exists());
	}

	@Test
	public void testV2() {
		Scenario sc = ReceiverChessboardScenario.createChessboardScenario(1L, 5,  utils.getOutputDirectory(), false );
		ReceiverUtils.getReceivers(sc).getAttributes().putAttribute("date",
				new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format( Calendar.getInstance().getTime()));

		/* Now the receiver is 'complete', and we can write it to file. */
		try {
			new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).writeV2(utils.getOutputDirectory() + "receivers_v2.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should write without exception.");
		}

		Assert.assertTrue("File should exist.", new File(utils.getOutputDirectory() + "receivers_v2.xml").exists());
	}

}
