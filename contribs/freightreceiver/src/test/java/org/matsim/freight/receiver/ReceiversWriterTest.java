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

package org.matsim.freight.receiver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.receiver.run.chessboard.ReceiverChessboardScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ReceiversWriterTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testV1() {
		Scenario sc = ReceiverChessboardScenario.createChessboardScenario(1L, 5, utils.getOutputDirectory(), false );
		ReceiverUtils.getReceivers(sc).getAttributes().putAttribute("date",
				new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format( Calendar.getInstance().getTime()));

		/* Now the receiver is 'complete', and we can write it to file. */
		try {
			new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).writeV1(utils.getOutputDirectory() + "receivers_v1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should write without exception.");
		}

		Assertions.assertTrue(new File(utils.getOutputDirectory() + "receivers_v1.xml").exists(), "File should exist.");
	}

	@Test
	void testV2() {
		Scenario sc = ReceiverChessboardScenario.createChessboardScenario(1L, 5,  utils.getOutputDirectory(), false );
		ReceiverUtils.getReceivers(sc).getAttributes().putAttribute("date",
				new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format( Calendar.getInstance().getTime()));

		/* Now the receiver is 'complete', and we can write it to file. */
		try {
			new ReceiversWriter( ReceiverUtils.getReceivers( sc ) ).writeV2(utils.getOutputDirectory() + "receivers_v2.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should write without exception.");
		}

		Assertions.assertTrue(new File(utils.getOutputDirectory() + "receivers_v2.xml").exists(), "File should exist.");
	}

}
